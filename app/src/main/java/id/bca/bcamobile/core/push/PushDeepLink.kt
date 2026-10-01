package id.bca.bcamobile.core.push

import android.net.Uri
import id.bca.bcamobile.ui.navigation.BuktiTransaksi
import id.bca.bcamobile.ui.navigation.Notifikasi

/**
 * Menerjemahkan `data.deep_link` dari muatan push menjadi route navigasi.
 *
 * Skema `bcamobile://` adalah kesepakatan dengan backend, bukan tebakan client:
 * server yang menyusun nilainya (`bcamobile://transaction/{id}`), dan client hanya
 * meneruskan. Jangan merakit deep link dari `type`.
 *
 * Route diimpor dari `ui.navigation` seperti yang sudah dilakukan
 * `session/SessionRepository.kt` — pemetaan tujuan tinggal di satu tempat, bukan
 * tersebar di activity dan nav host.
 */
object PushDeepLink {

    /** Kunci extras Intent yang diisi SDK FCM dari `data` saat notifikasi ditekan. */
    const val EXTRA_TYPE = "type"
    const val EXTRA_DEEP_LINK = "deep_link"

    private const val SCHEME = "bcamobile"
    private const val HOST_TRANSACTION = "transaction"

    /**
     * Tujuan untuk [deepLink].
     *
     * Tanpa deep link, atau skema/tujuan yang tidak dikenal, jatuh ke layar
     * [Notifikasi] — nasabah baru saja menekan sesuatu dan pantas melihat
     * akibatnya, bukan dibiarkan di layar awal tanpa penjelasan.
     */
    fun routeFor(deepLink: String?): Any {
        val uri = deepLink?.takeIf { it.isNotBlank() }?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: return Notifikasi
        if (!uri.scheme.equals(SCHEME, ignoreCase = true)) return Notifikasi

        return when (uri.host?.lowercase()) {
            HOST_TRANSACTION -> uri.pathSegments.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { BuktiTransaksi(transactionId = it) }
                ?: Notifikasi

            else -> Notifikasi
        }
    }
}
