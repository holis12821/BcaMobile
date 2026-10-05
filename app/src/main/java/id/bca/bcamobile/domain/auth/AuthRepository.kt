package id.bca.bcamobile.domain.auth

import id.bca.bcamobile.domain.auth.model.AuthUser
import id.bca.bcamobile.domain.auth.model.BiometricChallenge
import id.bca.bcamobile.domain.auth.model.BiometricRegistration
import id.bca.bcamobile.domain.auth.model.BiometricType
import id.bca.bcamobile.domain.auth.model.PinPurpose
import id.bca.bcamobile.domain.auth.model.PinVerification
import id.bca.bcamobile.domain.common.DataResult

/**
 * Autentikasi nasabah. Kontrak: `bca-mobile-api/docs/01-API-SPECIFICATION.md` §2.
 *
 * Implementasi yang menyimpan token; pemanggil tidak pernah menyentuh token langsung.
 * PIN dan kode akses masuk sebagai teks biasa lalu dienkripsi di dalam — pemanggil
 * tidak boleh mengenkripsi sendiri, dan tidak boleh menyimpan nilainya.
 */
interface AuthRepository {

    val isLoggedIn: Boolean

    suspend fun loginWithAccessCode(accessCode: String): DataResult<AuthUser>

    suspend fun biometricChallenge(): DataResult<BiometricChallenge>

    suspend fun loginWithBiometric(
        type: BiometricType,
        challengeId: String,
        signedChallenge: String,
        keyId: String,
    ): DataResult<AuthUser>

    /**
     * Membuat kunci baru di AndroidKeyStore lalu mendaftarkannya ke server.
     * Butuh access token, jadi hanya boleh dipanggil setelah login kode akses.
     */
    suspend fun registerBiometric(type: BiometricType): DataResult<BiometricRegistration>

    /** Membuang kunci lokal dan mematikan biometrik di server. */
    suspend fun unregisterBiometric(): DataResult<Unit>

    /** true bila perangkat ini sudah punya kunci biometrik yang terdaftar. */
    fun hasBiometricKey(): Boolean

    /** Token berumur pendek untuk satu transaksi; simpan di state, bukan di disk. */
    suspend fun verifyPin(pin: String, purpose: PinPurpose): DataResult<PinVerification>

    /** Memindahkan PIN transaksi. */
    suspend fun changePin(oldPin: String, newPin: String): DataResult<Unit>

    /** Memindahkan kode akses login — rahasia yang berbeda dari [changePin]. */
    suspend fun changeAccessCode(oldCode: String, newCode: String): DataResult<Unit>

    /** Membersihkan token lokal apa pun hasil panggilan server. */
    suspend fun logout(): DataResult<Unit>
}
