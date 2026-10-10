package id.bca.bcamobile.core.security

import android.content.Context
import android.os.Build
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import id.bca.bcamobile.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Sinyal risiko perangkat yang dikirim sebagai **masukan**, bukan sebagai keputusan.
 *
 * Client tidak pernah memblokir sendiri berdasarkan nilai di sini: pemeriksaan root
 * dan emulator di sisi client bisa dipalsukan oleh hal yang sama yang hendak
 * dideteksinya. Yang membuatnya berguna adalah server, yang memadukannya dengan
 * verdict Play Integrity — dan verdict itulah yang diverifikasi, bukan dipercaya.
 */
data class DeviceRiskSignals(
    val isEmulatorLikely: Boolean,
    val hasRootArtifacts: Boolean,
    val isDebuggerAttached: Boolean,
)

/** Token integritas, atau alasan kenapa tidak ada. */
sealed interface IntegrityTokenResult {
    data class Available(val token: String) : IntegrityTokenResult

    /** Nomor project Cloud belum dikonfigurasi — lihat `PLAY_INTEGRITY_CLOUD_PROJECT`. */
    data object NotConfigured : IntegrityTokenResult

    data class Failed(val reason: String) : IntegrityTokenResult
}

/**
 * Peminta token Play Integrity, terikat ke nonce tantangan liveness.
 *
 * Token **tidak dibaca di client**: isinya ditandatangani Google dan hanya berarti
 * setelah di-decode server (keputusan Q3). Yang dilakukan kelas ini hanya meminta
 * token dengan nonce yang sama dengan nonce tantangan, supaya server bisa memastikan
 * token itu diterbitkan untuk percobaan ini — bukan token lama yang dipakai ulang.
 */
@Singleton
class DeviceIntegrityProvider @Inject constructor(
    private val context: Context,
) {

    /** true bila verdict yang gagal hanya dicatat, tidak memblokir (dev/SIT saja). */
    val isLogOnly: Boolean get() = BuildConfig.PLAY_INTEGRITY_LOG_ONLY

    suspend fun requestToken(nonce: String): IntegrityTokenResult {
        val cloudProject = BuildConfig.PLAY_INTEGRITY_CLOUD_PROJECT
        if (cloudProject == 0L) return IntegrityTokenResult.NotConfigured

        return suspendCancellableCoroutine { continuation ->
            runCatching {
                IntegrityManagerFactory.create(context)
                    .requestIntegrityToken(
                        IntegrityTokenRequest.builder()
                            // Nonce tantangan dipakai apa adanya: server membandingkannya
                            // dengan nonce yang diterbitkannya sendiri.
                            .setNonce(nonce)
                            .setCloudProjectNumber(cloudProject)
                            .build(),
                    )
                    .addOnSuccessListener { response ->
                        if (continuation.isActive) {
                            continuation.resume(IntegrityTokenResult.Available(response.token()))
                        }
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) {
                            continuation.resume(
                                IntegrityTokenResult.Failed(
                                    error::class.simpleName ?: "unknown",
                                ),
                            )
                        }
                    }
            }.onFailure { error ->
                if (continuation.isActive) {
                    continuation.resume(
                        IntegrityTokenResult.Failed(error::class.simpleName ?: "unknown"),
                    )
                }
            }
        }
    }

    /**
     * Sinyal risiko yang bisa dibaca tanpa izin tambahan.
     *
     * Daftarnya pendek dengan sengaja. Pemeriksaan root yang panjang memberi rasa aman
     * yang keliru: satu modul Magisk menyembunyikan semuanya. Yang benar-benar menahan
     * perangkat yang sudah dikuasai adalah verdict Play Integrity di server.
     */
    fun riskSignals(): DeviceRiskSignals = DeviceRiskSignals(
        isEmulatorLikely = isEmulatorLikely(),
        hasRootArtifacts = ROOT_PATHS.any { runCatching { File(it).exists() }.getOrDefault(false) },
        isDebuggerAttached = android.os.Debug.isDebuggerConnected(),
    )

    private fun isEmulatorLikely(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("vbox") ||
            Build.FINGERPRINT.contains("emulator") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.PRODUCT == "google_sdk" ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu")

    private companion object {
        val ROOT_PATHS = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
        )
    }
}
