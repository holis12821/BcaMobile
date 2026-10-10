package id.bca.bcamobile.core.network

/**
 * Keadaan konektivitas yang ditampilkan lampu indikator.
 *
 * Tiga keadaan, bukan empat: tidak ada "belum diketahui". Keadaan awal dibaca
 * **sinkron** dari [android.net.ConnectivityManager] saat monitor dibuat, jadi
 * tidak ada jendela waktu di mana lampunya tidak punya jawaban. Lampu abu-abu
 * yang berarti "entah" hanya memindahkan pertanyaannya ke nasabah.
 */
enum class NetworkStatus {
    /** Jaringan ada, tervalidasi, dan panggilan API terakhir sehat. */
    ONLINE,

    /**
     * Tersambung tapi tidak layak pakai: panggilan API lambat, atau perkiraan
     * bandwidth turun di bawah ambang.
     *
     * Dibedakan dari [OFFLINE] karena tindakan nasabahnya berbeda — menunggu,
     * bukan mencari sinyal.
     */
    DEGRADED,

    /**
     * Tidak ada jalan ke server. Mencakup dua hal yang terasa sama bagi nasabah:
     * tidak ada jaringan sama sekali, dan jaringan ada tapi API tidak bisa
     * dihubungi (RTO, 5xx, TLS gagal, captive portal).
     */
    OFFLINE,
}

/**
 * Ambang yang menentukan kapan lampu berganti warna.
 *
 * Satu objek, seperti `LivenessConfig`, dan untuk alasan yang sama: angka yang
 * tersebar sebagai konstanta privat tidak bisa dikalibrasi tanpa menyentuh
 * logikanya. Nilai di bawah adalah **titik awal** — yang final datang dari
 * pengukuran di jaringan seluler Indonesia yang sesungguhnya.
 */
data class NetworkStatusConfig(
    /**
     * Panggilan API yang lebih lama dari ini dianggap lambat → [NetworkStatus.DEGRADED].
     *
     * 2,5 detik dipilih di atas waktu muat normal endpoint teringan (`health`,
     * ratusan milidetik) tapi masih jauh di bawah `CONNECT_TIMEOUT` OkHttp —
     * supaya "lambat" sempat terlihat sebagai kuning sebelum berubah jadi merah
     * karena timeout.
     */
    val slowCallMillis: Long = 2_500L,

    /**
     * Perkiraan bandwidth turun minimum sebelum jaringan dianggap lemah.
     *
     * Dipakai **hanya** sebagai petunjuk saat belum ada panggilan API terbaru:
     * `linkDownstreamBandwidthKbps` adalah perkiraan tipikal dari sistem, bukan
     * pengukuran, dan di beberapa perangkat nilainya tetap meski sinyal berubah.
     * Hasil panggilan sungguhan selalu lebih dipercaya daripada angka ini.
     *
     * 300 kbps kira-kira batas di mana satu request JSON mulai terasa menunggu.
     */
    val minDownstreamKbps: Int = 300,

    /**
     * Umur maksimum hasil panggilan API sebelum dianggap basi.
     *
     * Setelah ini, status jatuh kembali ke sinyal sistem. Tanpa batas umur,
     * satu kegagalan akan membuat lampunya merah selamanya walaupun jaringan
     * sudah pulih dan nasabah tidak memanggil apa pun.
     */
    val signalFreshnessMillis: Long = 30_000L,

    /**
     * Jeda antar-probe `GET /health` saat aplikasi di depan.
     *
     * Probe hanya berjalan kalau **tidak ada** panggilan API nyata dalam jeda
     * terakhir, jadi di layar yang aktif memanggil API angka ini tidak pernah
     * menambah traffic sama sekali.
     */
    val probeIntervalMillis: Long = 30_000L,

    /**
     * Lama satu perubahan harus bertahan sebelum lampunya ikut berubah.
     *
     * Jaringan seluler rutin berkedip — satu request lambat di antara yang
     * normal bukan gangguan. Tanpa penahan ini lampunya berkedip kuning-hijau
     * dan nasabah belajar mengabaikannya, yang membuat lampu merah juga
     * terabaikan. Pengecualian: perpindahan ke [NetworkStatus.OFFLINE] karena
     * jaringan sistem hilang berlaku **seketika**, karena itu fakta, bukan
     * perkiraan.
     */
    val stabilizeMillis: Long = 1_500L,
)
