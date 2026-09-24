package id.bca.bcamobile.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.Signature
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Kesiapan biometrik perangkat; menentukan apakah opsi login biometrik ditawarkan. */
enum class BiometricAvailability {
    READY,

    /** Perangkat mendukung, tetapi belum ada sidik jari atau wajah terdaftar. */
    NOT_ENROLLED,

    /** Tidak ada sensor, atau sensornya tidak memenuhi kelas STRONG. */
    NO_HARDWARE,

    /** Sensor sementara tidak bisa dipakai, atau butuh pembaruan keamanan. */
    TEMPORARILY_UNAVAILABLE,
}

sealed interface BiometricAuthResult {
    /** [signature] sudah dibuka kuncinya dan siap menandatangani. */
    data class Success(val signature: Signature) : BiometricAuthResult

    /** Pengguna menutup dialog atau memilih tombol negatif. */
    data object Cancelled : BiometricAuthResult

    data class Failed(val code: Int) : BiometricAuthResult
}

/**
 * Pembungkus [BiometricPrompt] yang mengembalikan objek [Signature] yang sudah
 * boleh dipakai.
 *
 * Aplikasi tidak pernah melihat data biometrik maupun hasil verifikasinya —
 * sistem operasi yang memutuskan, lalu menyerahkan kembali objek kriptografi
 * yang kuncinya sudah terbuka.
 */
@Singleton
class BiometricSigner @Inject constructor(
    private val context: Context,
) {

    fun availability(): BiometricAvailability =
        when (BiometricManager.from(context).canAuthenticate(STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN,
            -> BiometricAvailability.NO_HARDWARE

            else -> BiometricAvailability.TEMPORARILY_UNAVAILABLE
        }

    /**
     * Menampilkan dialog biometrik dan menunggu hasilnya.
     *
     * Wajib dipanggil dari dispatcher utama. Pembatalan coroutine menutup dialog,
     * sehingga tidak ada dialog yang tertinggal saat layar dilepas.
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        signature: Signature,
        title: String,
        subtitle: String,
        negativeButton: String,
    ): BiometricAuthResult = suspendCancellableCoroutine { continuation ->
        val callback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                // Objek diambil dari hasil, bukan dari variabel asal — hanya yang
                // ini dijamin sudah dibuka kuncinya oleh sistem.
                val unlocked = result.cryptoObject?.signature
                if (continuation.isActive) {
                    continuation.resume(
                        if (unlocked == null) {
                            BiometricAuthResult.Failed(ERROR_NO_CRYPTO)
                        } else {
                            BiometricAuthResult.Success(unlocked)
                        },
                    )
                }
            }

            override fun onAuthenticationError(code: Int, message: CharSequence) {
                if (!continuation.isActive) return
                val cancelled = code == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    code == BiometricPrompt.ERROR_USER_CANCELED ||
                    code == BiometricPrompt.ERROR_CANCELED
                continuation.resume(
                    if (cancelled) {
                        BiometricAuthResult.Cancelled
                    } else {
                        BiometricAuthResult.Failed(code)
                    },
                )
            }

            /** Satu percobaan gagal; dialog masih terbuka, jadi belum ada hasil akhir. */
            override fun onAuthenticationFailed() = Unit
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            callback,
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            // Wajib ada saat hanya BIOMETRIC_STRONG yang diizinkan.
            .setNegativeButtonText(negativeButton)
            .setAllowedAuthenticators(STRONG)
            .setConfirmationRequired(true)
            .build()

        continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        prompt.authenticate(info, BiometricPrompt.CryptoObject(signature))
    }

    private companion object {
        const val STRONG = BiometricManager.Authenticators.BIOMETRIC_STRONG

        /** Sistem melaporkan sukses tanpa menyertakan objek kripto — tidak boleh terjadi. */
        const val ERROR_NO_CRYPTO = -1
    }
}
