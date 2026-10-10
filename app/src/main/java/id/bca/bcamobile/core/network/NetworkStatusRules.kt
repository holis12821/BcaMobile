package id.bca.bcamobile.core.network

/**
 * Sinyal mentah yang menentukan warna lampu.
 *
 * Dipisah dari [NetworkStatusMonitor] supaya aturannya bisa diuji tanpa
 * `ConnectivityManager`, `ProcessLifecycleOwner`, atau OkHttp. Aturan inilah
 * bagian yang paling mudah salah dan paling perlu diuji — bukan perakitan
 * callback-nya.
 */
data class NetworkSignals(
    /** Ada jaringan yang ber-`INTERNET` **dan** `VALIDATED` menurut sistem. */
    val systemOnline: Boolean,

    /**
     * Perkiraan bandwidth turun dari sistem, kbps. `0` berarti **tidak
     * dilaporkan**, bukan nol.
     */
    val downstreamKbps: Int = 0,

    /** Waktu panggilan API terakhir selesai, epoch millis. `0` = belum ada. */
    val lastCallAtMillis: Long = 0L,

    /** Panggilan terakhir tidak menghasilkan response, atau dijawab 5xx. */
    val lastCallFailed: Boolean = false,

    /** Lama panggilan terakhir, millis. */
    val lastCallDurationMillis: Long = 0L,
)

/**
 * Menentukan warna lampu dari sinyal yang ada.
 *
 * Urutannya dari yang paling bisa dibuktikan ke yang paling perkiraan:
 *
 *  1. **Sistem bilang tidak ada jaringan** — fakta, tidak ada yang bisa
 *     mengalahkannya.
 *  2. **Hasil panggilan API yang masih segar** — mengukur jalur yang benar-benar
 *     dipakai aplikasi. Inilah yang menangkap server yang mati di balik sinyal
 *     empat bar.
 *  3. **Perkiraan bandwidth** — hanya kalau tidak ada panggilan segar.
 *
 * [nowMillis] dioper, tidak dibaca di dalam, supaya hasilnya deterministik dan
 * kedaluwarsa sinyal bisa diuji tanpa menunggu.
 */
fun evaluateNetworkStatus(
    signals: NetworkSignals,
    config: NetworkStatusConfig,
    nowMillis: Long,
): NetworkStatus {
    if (!signals.systemOnline) return NetworkStatus.OFFLINE

    val hasFreshCall = signals.lastCallAtMillis > 0 &&
        nowMillis - signals.lastCallAtMillis <= config.signalFreshnessMillis

    if (hasFreshCall) {
        // Panggilan yang gagal berarti tidak ada jalan ke server SEKARANG,
        // apa pun kata sinyal sistem. Dari sisi nasabah, server yang tidak
        // menjawab dan jaringan yang mati sama saja.
        if (signals.lastCallFailed) return NetworkStatus.OFFLINE
        if (signals.lastCallDurationMillis >= config.slowCallMillis) {
            return NetworkStatus.DEGRADED
        }
        return NetworkStatus.ONLINE
    }

    // `0` berarti sistem tidak menyediakan angka ini — memperlakukannya sebagai
    // bandwidth nol akan membuat lampu kuning permanen di perangkat yang memang
    // tidak melaporkannya.
    if (signals.downstreamKbps in 1 until config.minDownstreamKbps) {
        return NetworkStatus.DEGRADED
    }

    return NetworkStatus.ONLINE
}
