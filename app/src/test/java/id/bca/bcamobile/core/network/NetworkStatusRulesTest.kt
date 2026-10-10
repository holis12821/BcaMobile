package id.bca.bcamobile.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Aturan warna lampu indikator jaringan.
 *
 * Yang diuji di sini adalah tiga hal yang paling mudah salah: urutan keutamaan
 * sinyal, kedaluwarsa sinyal, dan perbedaan antara "bandwidth rendah" dan
 * "bandwidth tidak dilaporkan".
 */
class NetworkStatusRulesTest {

    private val config = NetworkStatusConfig()
    private val now = 1_000_000L

    private fun statusOf(signals: NetworkSignals) =
        evaluateNetworkStatus(signals, config, now)

    // -- Sinyal sistem ---------------------------------------------------------

    @Test
    fun `tanpa jaringan tervalidasi lampunya merah`() {
        val status = statusOf(NetworkSignals(systemOnline = false))

        assertEquals(NetworkStatus.OFFLINE, status)
    }

    /**
     * Captive portal: jaringan ada, tapi tidak sampai ke internet.
     *
     * `systemOnline` sudah mensyaratkan `VALIDATED`, jadi keadaan ini masuk ke
     * cabang yang sama dengan tidak ada jaringan sama sekali — dan memang itu
     * yang dirasakan nasabah.
     */
    @Test
    fun `jaringan tanpa validasi diperlakukan sama dengan tidak ada jaringan`() {
        val status = statusOf(
            NetworkSignals(systemOnline = false, downstreamKbps = 50_000),
        )

        assertEquals(NetworkStatus.OFFLINE, status)
    }

    /**
     * Sinyal sistem yang hilang mengalahkan panggilan API yang baru saja sukses.
     *
     * Urutannya penting: nasabah yang baru memuat satu layar lalu masuk lift
     * harus langsung melihat merah, bukan hijau sisa panggilan terakhir.
     */
    @Test
    fun `jaringan hilang mengalahkan panggilan sukses yang masih segar`() {
        val status = statusOf(
            NetworkSignals(
                systemOnline = false,
                lastCallAtMillis = now,
                lastCallFailed = false,
                lastCallDurationMillis = 100L,
            ),
        )

        assertEquals(NetworkStatus.OFFLINE, status)
    }

    // -- Hasil panggilan API ---------------------------------------------------

    /**
     * Inti permintaannya: API gangguan → merah, walaupun jaringannya sempurna.
     *
     * Ini keadaan yang tidak bisa dideteksi dari sinyal sistem sama sekali.
     */
    @Test
    fun `panggilan API gagal membuat merah walau jaringan penuh`() {
        val status = statusOf(
            NetworkSignals(
                systemOnline = true,
                downstreamKbps = 50_000,
                lastCallAtMillis = now,
                lastCallFailed = true,
                lastCallDurationMillis = 120L,
            ),
        )

        assertEquals(NetworkStatus.OFFLINE, status)
    }

    @Test
    fun `panggilan lambat membuat kuning`() {
        val status = statusOf(
            NetworkSignals(
                systemOnline = true,
                lastCallAtMillis = now,
                lastCallDurationMillis = config.slowCallMillis,
            ),
        )

        assertEquals(NetworkStatus.DEGRADED, status)
    }

    @Test
    fun `panggilan tepat di bawah ambang masih hijau`() {
        val status = statusOf(
            NetworkSignals(
                systemOnline = true,
                lastCallAtMillis = now,
                lastCallDurationMillis = config.slowCallMillis - 1,
            ),
        )

        assertEquals(NetworkStatus.ONLINE, status)
    }

    @Test
    fun `panggilan cepat membuat hijau`() {
        val status = statusOf(
            NetworkSignals(
                systemOnline = true,
                lastCallAtMillis = now,
                lastCallDurationMillis = 180L,
            ),
        )

        assertEquals(NetworkStatus.ONLINE, status)
    }

    // -- Kedaluwarsa sinyal ----------------------------------------------------

    /**
     * Tanpa batas umur, satu kegagalan akan memerahkan lampunya selamanya —
     * walaupun jaringan sudah pulih dan nasabah tidak memanggil apa pun lagi.
     */
    @Test
    fun `kegagalan yang sudah basi tidak lagi memerahkan lampu`() {
        val status = statusOf(
            NetworkSignals(
                systemOnline = true,
                lastCallAtMillis = now - config.signalFreshnessMillis - 1,
                lastCallFailed = true,
            ),
        )

        assertEquals(NetworkStatus.ONLINE, status)
    }

    @Test
    fun `kegagalan tepat di batas umur masih dihitung`() {
        val status = statusOf(
            NetworkSignals(
                systemOnline = true,
                lastCallAtMillis = now - config.signalFreshnessMillis,
                lastCallFailed = true,
            ),
        )

        assertEquals(NetworkStatus.OFFLINE, status)
    }

    @Test
    fun `belum ada panggilan sama sekali bukan kegagalan`() {
        val status = statusOf(NetworkSignals(systemOnline = true, lastCallAtMillis = 0L))

        assertEquals(NetworkStatus.ONLINE, status)
    }

    // -- Perkiraan bandwidth ---------------------------------------------------

    @Test
    fun `bandwidth rendah tanpa panggilan terbaru membuat kuning`() {
        val status = statusOf(
            NetworkSignals(
                systemOnline = true,
                downstreamKbps = config.minDownstreamKbps - 1,
            ),
        )

        assertEquals(NetworkStatus.DEGRADED, status)
    }

    /**
     * `0` berarti sistem tidak melaporkan angkanya, bukan bandwidth nol.
     *
     * Memperlakukannya sebagai lemah akan membuat lampu kuning permanen di
     * setiap perangkat yang tidak menyediakan perkiraan ini — gejala yang akan
     * terbaca sebagai "indikatornya rusak".
     */
    @Test
    fun `bandwidth nol berarti tidak dilaporkan, bukan lemah`() {
        val status = statusOf(NetworkSignals(systemOnline = true, downstreamKbps = 0))

        assertEquals(NetworkStatus.ONLINE, status)
    }

    /**
     * Pengukuran nyata mengalahkan perkiraan sistem.
     *
     * `linkDownstreamBandwidthKbps` adalah angka tipikal, dan di sebagian
     * perangkat nilainya tidak berubah meski sinyalnya berubah. Panggilan yang
     * sungguh-sungguh cepat membuktikan angka itu salah.
     */
    @Test
    fun `panggilan cepat mengalahkan perkiraan bandwidth yang rendah`() {
        val status = statusOf(
            NetworkSignals(
                systemOnline = true,
                downstreamKbps = 10,
                lastCallAtMillis = now,
                lastCallDurationMillis = 150L,
            ),
        )

        assertEquals(NetworkStatus.ONLINE, status)
    }

    @Test
    fun `bandwidth tinggi tidak menutupi panggilan yang lambat`() {
        val status = statusOf(
            NetworkSignals(
                systemOnline = true,
                downstreamKbps = 100_000,
                lastCallAtMillis = now,
                lastCallDurationMillis = config.slowCallMillis + 500,
            ),
        )

        assertEquals(NetworkStatus.DEGRADED, status)
    }
}
