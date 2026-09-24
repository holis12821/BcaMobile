package id.bca.bcamobile.core.network

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Satu kunci idempotensi per percobaan transaksi, dipakai ulang untuk setiap retry
 * percobaan yang sama. Tanpa ini, retry setelah timeout bisa menghasilkan dua transfer.
 *
 * Kunci dibuang lewat [release] begitu transaksi selesai — berhasil maupun ditolak
 * secara final. Jangan memanggil [release] saat masih mungkin di-retry.
 */
@Singleton
class IdempotencyKeyProvider @Inject constructor() {

    private val keys = ConcurrentHashMap<String, String>()

    /** Kunci untuk [scope], mis. `transfer:<inquiry_id>`. Sama selama belum di-release. */
    fun keyFor(scope: String): String =
        keys.getOrPut(scope) { UUID.randomUUID().toString() }

    fun release(scope: String) {
        keys.remove(scope)
    }
}
