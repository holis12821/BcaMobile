package id.bca.bcamobile.core.network

import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Melaporkan hasil setiap request OkHttp ke [NetworkStatusMonitor].
 *
 * Dipasang di `NetworkModule.baseClientBuilder`, yang dilewati **kedua** client
 * (`@OnboardingNetwork` dan `@AppNetwork`) — jadi satu listener menutup seluruh
 * lalu lintas aplikasi tanpa ada jalur yang terlewat.
 *
 * Dipilih [EventListener], bukan `Interceptor`, karena interceptor tidak pernah
 * melihat kegagalan yang terjadi sebelum chain-nya berjalan: DNS gagal, TLS
 * gagal, dan connect timeout. Justru itulah kegagalan yang paling perlu
 * memerahkan lampu — dan dengan interceptor ketiganya tidak terlihat.
 *
 * Yang dicatat hanya **durasi dan berhasil/gagal**. Tidak ada URL, header, atau
 * body: kelas ini hidup di jalur yang dilewati NIK, token, dan PIN.
 */
@Singleton
class NetworkCallWatcher @Inject constructor(
    private val monitor: NetworkStatusMonitor,
) : EventListener() {

    /**
     * Keadaan satu request yang sedang berjalan.
     *
     * [serverError] harus hidup di sini, bukan disimpulkan di `callEnd`: OkHttp
     * memanggil `responseHeadersEnd` lebih dulu lalu `callEnd` sesudahnya, jadi
     * rancangan pertama saya mencatat 5xx sebagai gagal lalu **menimpanya**
     * dengan "berhasil" sesaat kemudian. Lampunya tetap hijau di hadapan server
     * yang balas 500 — persis keadaan yang paling perlu terlihat.
     */
    private class InFlight(val startNanos: Long) {
        @Volatile
        var serverError: Boolean = false
    }

    /**
     * Map, bukan satu field: OkHttp menjalankan beberapa request serentak, dan
     * satu field akan membuat durasi request yang satu diukur dari mulainya
     * request yang lain.
     *
     * Dibersihkan di `callEnd`/`callFailed`, yang OkHttp jamin dipanggil tepat
     * satu kali per call — jadi map ini tidak tumbuh.
     */
    private val inFlight = ConcurrentHashMap<Call, InFlight>()

    override fun callStart(call: Call) {
        inFlight[call] = InFlight(System.nanoTime())
    }

    /**
     * 5xx ditandai gagal walaupun HTTP-nya "berhasil".
     *
     * Dari sisi nasabah, server yang menjawab 500 sama tidak bisa dipakainya
     * dengan server yang tidak menjawab. 4xx **tidak** termasuk: itu jawaban
     * yang sah dari server yang sehat — PIN salah atau sesi habis bukan
     * gangguan jaringan, dan memerahkan lampu karenanya akan menyesatkan.
     */
    override fun responseHeadersEnd(call: Call, response: Response) {
        if (response.code >= HTTP_SERVER_ERROR) {
            inFlight[call]?.serverError = true
        }
    }

    override fun callEnd(call: Call) {
        val record = inFlight.remove(call) ?: return
        monitor.recordCall(
            durationMillis = elapsedMillis(record.startNanos),
            failed = record.serverError,
        )
    }

    override fun callFailed(call: Call, ioe: IOException) {
        val record = inFlight.remove(call) ?: return
        // Timeout, DNS, TLS, socket putus — semuanya sampai di sini, dan
        // semuanya berarti tidak ada jalan ke server saat ini.
        monitor.recordCall(durationMillis = elapsedMillis(record.startNanos), failed = true)
    }

    private fun elapsedMillis(startNanos: Long): Long =
        (System.nanoTime() - startNanos) / NANOS_PER_MILLI

    private companion object {
        const val HTTP_SERVER_ERROR = 500
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
