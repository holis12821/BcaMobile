package id.bca.bcamobile.data.auth

import android.os.Build
import id.bca.bcamobile.BuildConfig
import id.bca.bcamobile.core.device.DeviceIdProvider
import id.bca.bcamobile.core.network.ApiCaller
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.core.security.BiometricKeyManager
import id.bca.bcamobile.core.security.PinEncryptor
import id.bca.bcamobile.data.auth.local.TokenManager
import id.bca.bcamobile.data.auth.remote.AuthApi
import id.bca.bcamobile.data.auth.remote.dto.ChangePinRequest
import id.bca.bcamobile.data.auth.remote.dto.DeviceInfoDto
import id.bca.bcamobile.data.auth.remote.dto.LoginBiometricRequest
import id.bca.bcamobile.data.auth.remote.dto.LoginPinRequest
import id.bca.bcamobile.data.auth.remote.dto.LoginResponse
import id.bca.bcamobile.data.auth.remote.dto.LogoutRequest
import id.bca.bcamobile.data.auth.remote.dto.RegisterBiometricRequest
import id.bca.bcamobile.data.auth.remote.dto.VerifyPinRequest
import id.bca.bcamobile.domain.auth.AuthRepository
import id.bca.bcamobile.domain.auth.model.AuthUser
import id.bca.bcamobile.domain.auth.model.BiometricChallenge
import id.bca.bcamobile.domain.auth.model.BiometricRegistration
import id.bca.bcamobile.domain.auth.model.BiometricType
import id.bca.bcamobile.domain.auth.model.PinPurpose
import id.bca.bcamobile.domain.auth.model.PinVerification
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.common.map
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val caller: ApiCaller,
    private val tokenManager: TokenManager,
    private val pinEncryptor: PinEncryptor,
    private val deviceIdProvider: DeviceIdProvider,
    private val biometricKeyManager: BiometricKeyManager,
) : AuthRepository {

    override val isLoggedIn: Boolean get() = tokenManager.isLoggedIn

    override suspend fun loginWithAccessCode(accessCode: String): DataResult<AuthUser> =
        withFreshPinKey {
            val encrypted = pinEncryptor.encrypt(accessCode)
                ?: return@withFreshPinKey DataResult.Failure(PIN_KEY_MISSING)

            caller.call(allowRetry = false) {
                api.loginWithPin(
                    LoginPinRequest(
                        deviceId = deviceIdProvider.deviceId(),
                        pinEncrypted = encrypted.ciphertext,
                        deviceInfo = deviceInfo(),
                        encryptionKeyId = encrypted.keyId,
                    ),
                )
            }.storeTokens()
        }

    override suspend fun biometricChallenge(): DataResult<BiometricChallenge> =
        caller.call { api.biometricChallenge(deviceIdProvider.deviceId()) }
            .map {
                BiometricChallenge(
                    challengeId = it.challengeId,
                    challenge = it.challenge,
                    expiresInSeconds = it.expiresIn,
                    algorithm = it.algorithm,
                    signatureFormat = it.signatureFormat,
                )
            }

    override suspend fun loginWithBiometric(
        type: BiometricType,
        challengeId: String,
        signedChallenge: String,
        keyId: String,
    ): DataResult<AuthUser> = caller.call(allowRetry = false) {
        api.loginWithBiometric(
            LoginBiometricRequest(
                deviceId = deviceIdProvider.deviceId(),
                biometricType = type.wireValue,
                challengeId = challengeId,
                signedChallenge = signedChallenge,
                keyId = keyId,
            ),
        )
    }.storeTokens()

    override fun hasBiometricKey(): Boolean = biometricKeyManager.hasKey()

    /**
     * Kunci dibuat lebih dulu, baru didaftarkan. Kalau server menolak, kunci lokal
     * ikut dibuang supaya tidak ada kunci yatim yang dipakai login dan selalu gagal.
     */
    override suspend fun registerBiometric(type: BiometricType): DataResult<BiometricRegistration> {
        val attestationChallenge = ByteArray(ATTESTATION_CHALLENGE_BYTES).also {
            SecureRandom().nextBytes(it)
        }
        if (!biometricKeyManager.createKey(attestationChallenge)) {
            return DataResult.Failure(BIOMETRIC_KEY_FAILED)
        }

        val publicKey = biometricKeyManager.publicKeyBase64()
            ?: return DataResult.Failure(BIOMETRIC_KEY_FAILED).also {
                biometricKeyManager.deleteKey()
            }

        val result = caller.call(allowRetry = false) {
            api.registerBiometric(
                RegisterBiometricRequest(
                    deviceId = deviceIdProvider.deviceId(),
                    biometricType = type.wireValue,
                    publicKey = publicKey,
                    keyId = biometricKeyManager.keyId(),
                    // Hanya sertifikat daun; bentuk rantai yang diterima server
                    // belum dipastikan — lihat dokumen handover butir 2.
                    attestation = biometricKeyManager.attestationChain().firstOrNull(),
                ),
            )
        }

        if (result is DataResult.Failure) biometricKeyManager.deleteKey()
        return result.map {
            BiometricRegistration(
                biometricId = it.biometricId,
                // Server boleh menetapkan key_id-nya sendiri; kalau tidak, yang
                // berlaku tetap milik Keystore lokal.
                keyId = it.keyId.ifBlank { biometricKeyManager.keyId() },
                replacedKeys = it.replacedKeys,
            )
        }
    }

    override suspend fun unregisterBiometric(): DataResult<Unit> {
        biometricKeyManager.deleteKey()
        return DataResult.Success(Unit)
    }

    override suspend fun verifyPin(
        pin: String,
        purpose: PinPurpose,
    ): DataResult<PinVerification> {
        return withFreshPinKey {
            val encrypted = pinEncryptor.encrypt(pin)
                ?: return@withFreshPinKey DataResult.Failure(PIN_KEY_MISSING)

            caller.call(allowRetry = false) {
                api.verifyPin(
                    VerifyPinRequest(
                        pinEncrypted = encrypted.ciphertext,
                        purpose = purpose.wireValue,
                        encryptionKeyId = encrypted.keyId,
                    ),
                )
            }.map { PinVerification(it.verificationToken, it.expiresIn) }
        }
    }

    override suspend fun changePin(oldPin: String, newPin: String): DataResult<Unit> =
        withFreshPinKey {
            val old = pinEncryptor.encrypt(oldPin)
                ?: return@withFreshPinKey DataResult.Failure(PIN_KEY_MISSING)
            val new = pinEncryptor.encrypt(newPin)
                ?: return@withFreshPinKey DataResult.Failure(PIN_KEY_MISSING)

            caller.call(allowRetry = false) {
                api.changePin(
                    ChangePinRequest(
                        oldPinEncrypted = old.ciphertext,
                        newPinEncrypted = new.ciphertext,
                        encryptionKeyId = old.keyId,
                    ),
                )
            }.map { }
        }

    override suspend fun changeAccessCode(oldCode: String, newCode: String): DataResult<Unit> =
        withFreshPinKey {
            val old = pinEncryptor.encrypt(oldCode)
                ?: return@withFreshPinKey DataResult.Failure(PIN_KEY_MISSING)
            val new = pinEncryptor.encrypt(newCode)
                ?: return@withFreshPinKey DataResult.Failure(PIN_KEY_MISSING)

            caller.call(allowRetry = false) {
                api.changeAccessCode(
                    ChangePinRequest(
                        oldPinEncrypted = old.ciphertext,
                        newPinEncrypted = new.ciphertext,
                        encryptionKeyId = old.keyId,
                    ),
                )
            }.map { }
        }

    /** Token lokal selalu dibersihkan, termasuk saat server gagal dihubungi. */
    override suspend fun logout(): DataResult<Unit> {
        val result = caller.call(allowRetry = false) {
            api.logout(LogoutRequest(deviceId = deviceIdProvider.deviceId()))
        }
        tokenManager.clear()
        return result.map { }
    }

    private fun DataResult<LoginResponse>.storeTokens(): DataResult<AuthUser> = map {
        tokenManager.saveTokens(it.accessToken, it.refreshToken)
        AuthUser(
            id = it.user?.id.orEmpty(),
            displayName = it.user?.displayName.orEmpty(),
            maskedAccount = it.user?.maskedAccount.orEmpty(),
        )
    }

    /**
     * Menjalankan [block]; kalau server menolak kunci yang dipakai, buang kunci
     * itu lalu jalankan **sekali** lagi dengan kunci baru.
     *
     * Tiga aturan dari `.claude/skills/frontend-pin-encryption/SKILL.md` §4:
     *
     * 1. **Sekali saja.** Gagal lagi berarti masalahnya bukan kunci basi;
     *    mengulang terus hanya menghabiskan jatah rate limit.
     * 2. **Enkripsi ulang dari awal.** [block] memanggil `encrypt` di dalamnya,
     *    jadi percobaan kedua membawa nonce dan `ts` yang segar — ciphertext lama
     *    tidak bisa dipakai ulang karena nonce-nya sudah hangus di server.
     * 3. **Bukan PIN salah.** Kode ini tidak pernah diteruskan sebagai kegagalan
     *    PIN, jadi layar tidak menampilkan sisa percobaan atau peringatan lockout.
     */
    private suspend fun <T> withFreshPinKey(
        block: suspend () -> DataResult<T>,
    ): DataResult<T> {
        val first = block()
        val failure = (first as? DataResult.Failure)?.error
        val isStaleKey = failure is ApiFailure.Business && failure.code in STALE_PIN_KEY_CODES
        if (!isStaleKey) return first

        pinEncryptor.invalidateKey()
        return block()
    }

    private fun deviceInfo() = DeviceInfoDto(
        model = "${Build.MANUFACTURER} ${Build.MODEL}",
        osVersion = "Android ${Build.VERSION.RELEASE}",
        appVersion = BuildConfig.VERSION_NAME,
    )

    private companion object {
        /** Panjang nonce attestation; mengikat sertifikat ke satu sesi pendaftaran. */
        const val ATTESTATION_CHALLENGE_BYTES = 32

        /** Perangkat menolak membuat kunci — biasanya biometrik belum didaftarkan. */
        val BIOMETRIC_KEY_FAILED = ApiFailure.Business(
            code = "CLIENT_BIOMETRIC_KEY_FAILED",
            message = "",
        )

        /**
         * `assets/pin_public.pem` belum tersedia di repo. Tanpa kunci itu PIN tidak
         * boleh dikirim apa adanya, jadi operasi berbasis PIN gagal lebih awal.
         */
        val PIN_KEY_MISSING = ApiFailure.Business(
            code = "CLIENT_PIN_KEY_MISSING",
            message = "",
        )

        /**
         * Server menolak `encryption_key_id` yang dikirim — kuncinya sudah
         * dirotasi. Onboarding memakai kode berbeda untuk keadaan yang sama.
         */
        val STALE_PIN_KEY_CODES = setOf("AUTH_PIN_KEY_UNKNOWN", "CRED_DECRYPTION_FAILED")
    }
}
