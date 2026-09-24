package id.bca.bcamobile.core.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Hasil penyiapan objek tanda tangan; kunci bisa hangus tanpa peringatan. */
sealed interface SigningKeyResult {
    data class Ready(val signature: Signature) : SigningKeyResult

    /** Kunci belum ada, atau hangus karena biometrik perangkat berubah. */
    data object NeedsRegistration : SigningKeyResult

    data object Unavailable : SigningKeyResult
}

/**
 * Pemilik pasangan kunci login biometrik di AndroidKeyStore.
 *
 * Kunci privat tidak pernah keluar dari perangkat keras — yang dikirim ke server
 * hanya kunci publik, rantai attestation, dan tanda tangan atas challenge.
 *
 * **Asumsi yang belum dikonfirmasi backend:** EC P-256 dan `SHA256withECDSA`.
 * Lihat `docs/backend/10-HANDOVER-BLOCKER-BACKEND.md` butir 2. Kalau backend
 * memilih RSA, yang berubah hanya berkas ini dan konstanta di bawah.
 */
@Suppress("DEPRECATION")
@Singleton
class BiometricKeyManager @Inject constructor(
    private val context: Context,
) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val keyStore: KeyStore
        get() = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    fun hasKey(): Boolean = runCatching { keyStore.containsAlias(KEY_ALIAS) }.getOrDefault(false)

    /** Pengenal kunci yang dikirim ke server; dibuat sekali bersama kuncinya. */
    fun keyId(): String = prefs.getString(KEY_ID, null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString(KEY_ID, it).apply()
    }

    /**
     * Membuat pasangan kunci baru. Kunci lama dibuang lebih dulu supaya tidak ada
     * dua kunci aktif untuk satu perangkat.
     *
     * @param attestationChallenge nilai acak yang mengikat attestation ke sesi
     *   pendaftaran ini. Dipakai server untuk menolak attestation daur ulang.
     */
    fun createKey(attestationChallenge: ByteArray): Boolean {
        deleteKey()
        val created = runCatching { generate(attestationChallenge, strongBox = true) }
            .recoverCatching { error ->
                // Sebagian besar perangkat tidak punya StrongBox; itu bukan kegagalan.
                // Penjaga SDK wajib: StrongBoxUnavailableException baru ada di API 28,
                // sementara minSdk 26. Tanpa itu, pemeriksaan tipe di perangkat lama
                // memicu NoClassDefFoundError dan menelan alasan kegagalan aslinya.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    error is StrongBoxUnavailableException
                ) {
                    generate(attestationChallenge, strongBox = false)
                } else {
                    throw error
                }
            }
            .recoverCatching {
                // Perangkat lama dan emulator bisa menolak attestation.
                generate(attestationChallenge = null, strongBox = false)
            }
            .isSuccess

        if (created) keyId() else deleteKey()
        return created
    }

    private fun generate(attestationChallenge: ByteArray?, strongBox: Boolean) {
        val builder = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec(CURVE))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(true)
            // Sidik jari atau wajah baru menghanguskan kunci — disengaja.
            .setInvalidatedByBiometricEnrollment(true)

        if (attestationChallenge != null) {
            builder.setAttestationChallenge(attestationChallenge)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 0 detik: wajib verifikasi biometrik setiap kali kunci dipakai.
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        }
        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE).apply {
            initialize(builder.build())
            generateKeyPair()
        }
    }

    /** X.509 SubjectPublicKeyInfo dalam base64, tanpa pembungkus PEM. */
    fun publicKeyBase64(): String? = runCatching {
        val certificate = keyStore.getCertificate(KEY_ALIAS) ?: return null
        Base64.encodeToString(certificate.publicKey.encoded, Base64.NO_WRAP)
    }.getOrNull()

    /**
     * Rantai sertifikat attestation, daun lebih dulu.
     *
     * Kontrak backend menulis `attestation` sebagai satu string, padahal
     * attestation sebenarnya berupa rantai. Sampai bentuknya dipastikan, hanya
     * sertifikat daun yang dikirim — lihat butir 2 dokumen handover.
     */
    fun attestationChain(): List<String> = runCatching {
        keyStore.getCertificateChain(KEY_ALIAS)
            ?.map { Base64.encodeToString(it.encoded, Base64.NO_WRAP) }
            .orEmpty()
    }.getOrDefault(emptyList())

    /**
     * Menyiapkan [Signature] yang terikat kunci privat. Objek ini belum boleh
     * dipakai — harus melewati BiometricPrompt lebih dulu.
     */
    fun signatureForSigning(): SigningKeyResult {
        val privateKey = runCatching { keyStore.getKey(KEY_ALIAS, null) as? PrivateKey }
            .getOrNull() ?: return SigningKeyResult.NeedsRegistration

        val signature = runCatching { Signature.getInstance(SIGNATURE_ALGORITHM) }
            .getOrNull() ?: return SigningKeyResult.Unavailable

        return try {
            signature.initSign(privateKey)
            SigningKeyResult.Ready(signature)
        } catch (_: KeyPermanentlyInvalidatedException) {
            // Biometrik perangkat berubah. Kunci ini tidak akan pernah berlaku lagi.
            deleteKey()
            SigningKeyResult.NeedsRegistration
        } catch (_: Exception) {
            SigningKeyResult.Unavailable
        }
    }

    fun deleteKey() {
        runCatching { keyStore.deleteEntry(KEY_ALIAS) }
        prefs.edit().remove(KEY_ID).apply()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "bca_biometric_login_v1"
        const val CURVE = "secp256r1"
        const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        const val PREFS_FILE = "biometric_key"
        const val KEY_ID = "key_id"
    }
}
