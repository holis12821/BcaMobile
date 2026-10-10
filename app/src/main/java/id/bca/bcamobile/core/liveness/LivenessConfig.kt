package id.bca.bcamobile.core.liveness

/**
 * Pemetaan tanda sumbu ML Kit ke arah yang dirasakan pengguna.
 *
 * ML Kit tidak menjanjikan tanda `headEulerAngleY` relatif terhadap pengguna:
 * nilainya relatif terhadap **gambar**, dan gambar kamera depan dicerminkan oleh
 * preview tapi belum tentu oleh buffer analisa. Akibatnya "menoleh ke kiri" bisa
 * terbaca sebagai yaw positif di satu perangkat dan negatif di perangkat lain.
 *
 * Karena itu tandanya tidak ditebak di dalam kode deteksi, melainkan dikonfigurasi
 * di satu tempat ini dan **diverifikasi di perangkat fisik** — lihat daftar periksa
 * kalibrasi di `docs/bca-face-liveness-decisions.md` Q7. Nilai bawaan di bawah
 * adalah dugaan awal, bukan hasil pengukuran.
 */
data class LivenessCalibration(
    /**
     * Dikalikan ke `headEulerAngleY` supaya hasilnya **positif saat pengguna
     * menoleh ke kiri-nya sendiri**. Isi `-1f` bila ternyata terbalik.
     */
    val yawSignForUserLeft: Float = 1f,
    /**
     * Dikalikan ke `headEulerAngleX` supaya hasilnya **positif saat pengguna
     * menengadah**. Isi `-1f` bila ternyata terbalik.
     */
    val pitchSignForUp: Float = 1f,
    /**
     * true bila buffer `ImageAnalysis` ikut dicerminkan seperti preview.
     *
     * Hanya memengaruhi koordinat kotak wajah (gerbang "wajah di tengah"), bukan
     * sudut kepala — sudut sudah ditangani dua tanda di atas.
     */
    val isAnalysisMirrored: Boolean = false,
)

/**
 * Semua angka ambang deteksi liveness, dalam satu objek.
 *
 * Dulu tersebar sebagai konstanta privat di dalam detektor, yang berarti tidak ada
 * cara mengkalibrasi ulang tanpa menyentuh logika deteksi. Nilai di sini adalah
 * **titik awal** dari spesifikasi Phase 2; yang final datang dari pengujian di
 * perangkat fisik.
 */
data class LivenessConfig(
    val calibration: LivenessCalibration = LivenessCalibration(),

    // -- Gerbang kualitas sebelum tantangan dimulai otomatis -------------------

    /** Lebar kotak wajah dibanding lebar frame; terlalu kecil berarti terlalu jauh. */
    val minFaceWidthRatio: Float = 0.35f,
    val maxFaceWidthRatio: Float = 0.70f,
    /** Pusat oval panduan dalam koordinat frame ternormalisasi (0..1). */
    val ovalCenterX: Float = 0.5f,
    val ovalCenterY: Float = 0.5f,
    /**
     * Jarak maksimum pusat wajah dari pusat oval, sebagai rasio sisi frame.
     *
     * Pengganti praktis untuk uji "kotak wajah di dalam oval": oval panduan digambar
     * composable dan geometrinya tidak tersedia di aliran analisa. Yang diuji di sini
     * adalah titik pusat dan ukuran — cukup untuk menolak wajah di pojok atau terlalu
     * jauh, dan tidak berpura-pura mengukur hal yang tidak diketahuinya.
     */
    val maxCenterOffsetRatio: Float = 0.18f,
    /** Batas |yaw|, |pitch|, |roll| yang masih dianggap menghadap lurus. */
    val neutralYawDegrees: Float = 10f,
    val neutralPitchDegrees: Float = 10f,
    val neutralRollDegrees: Float = 15f,
    val eyeOpenProbability: Float = 0.8f,
    val eyeClosedProbability: Float = 0.2f,
    /** Lama gerbang kualitas harus bertahan sebelum tantangan mulai sendiri. */
    val readyHoldMillis: Long = 500L,
    /**
     * Tinggi dibanding lebar elips panduan yang digambar di layar.
     *
     * Kepala manusia dilihat dari depan berbanding kira-kira 1 : 1,4 — itu yang
     * membuat panduannya terbaca sebagai kepala, bukan sebagai kotak. Angka ini
     * hanya soal **bentuk**; yang menilai **ukuran** wajah tetap
     * [minFaceWidthRatio]..[maxFaceWidthRatio].
     */
    val faceGuideHeightToWidth: Float = 1.38f,

    // -- Deteksi per gerakan ---------------------------------------------------

    /** Yaw yang harus dilewati untuk TURN_LEFT / TURN_RIGHT. */
    val turnYawDegrees: Float = 25f,
    val lookUpPitchDegrees: Float = 20f,
    val lookDownPitchDegrees: Float = 15f,
    /**
     * Jumlah frame berurutan yang harus memenuhi syarat sebelum satu gerakan
     * dianggap selesai. Satu frame saja membuat deteksi bergetar: jitter sudut
     * ±5° di perangkat kelas bawah sudah cukup untuk memicu langkah yang salah.
     */
    val holdFrames: Int = 3,
    /**
     * Seluruh rangkaian buka → pejam → buka harus selesai dalam rentang ini.
     *
     * Yang diukur adalah sejak mata **mulai** menutup, bukan sejak frame terbuka
     * pertama: keadaan "terbuka" adalah keadaan diam yang bisa berlangsung semenit.
     * Batas ini yang membuat foto bermata terpejam gagal — foto tidak punya transisi.
     */
    val blinkWindowMillis: Long = 600L,

    // -- Batas waktu dan percobaan --------------------------------------------

    val stepTimeoutMillis: Long = 8_000L,
    val totalTimeoutMillis: Long = 30_000L,
    /** Wajah hilang lebih lama dari ini saat tantangan berjalan → tantangan dibatalkan. */
    val faceLostGraceMillis: Long = 1_000L,
    /** Gerakan salah sebanyak ini dalam satu tantangan → tantangan gagal. */
    val maxWrongMoves: Int = 3,
) {
    /**
     * Lebar elips panduan sebagai rasio lebar viewfinder.
     *
     * **Diturunkan** dari gerbang, tidak diangkakan sendiri: panduan yang digambar
     * harus menunjukkan ukuran yang benar-benar diterima. Sebelum ini panduannya
     * `CircleShape` di atas kotak non-persegi — artinya pill selebar ~92% viewfinder
     * — sementara gerbang menuntut [minFaceWidthRatio]..[maxFaceWidthRatio].
     * Akibatnya nasabah yang mengisi panduan sampai penuh justru ditolak
     * `MOVE_AWAY`, dan yang ukurannya benar tampak kecil mengambang di tengah.
     *
     * Titik tengah band dipilih supaya dua arah koreksi ("dekatkan" dan
     * "jauhkan") punya ruang yang sama.
     */
    val faceGuideWidthFraction: Float
        get() = (minFaceWidthRatio + maxFaceWidthRatio) / 2f
}

/**
 * Geometri elips panduan untuk lapisan UI.
 *
 * Layar tidak boleh memegang [LivenessConfig] — composable layar stateless dan
 * confignya hidup di ViewModel. Tapi angka panduan juga tidak boleh diangkakan
 * ulang di layar, karena begitu keduanya terpisah, panduan dan gerbang mulai
 * menyimpang tanpa ada yang menyadarinya. Objek ini jalan tengahnya: satu
 * turunan dari nilai bawaan config, dibaca sebagai konstanta.
 *
 * Berlaku dengan asumsi `PreviewView.ScaleType.FILL_CENTER` dan frame analisa
 * 3:4 di viewfinder yang **lebih lebar** dari itu — pada kombinasi tersebut
 * pemotongan terjadi vertikal saja, jadi rasio lebar di layar sama dengan rasio
 * lebar di frame analisa. Kalau rasio viewfinder diubah jadi lebih sempit dari
 * 0,75 asumsi ini batal dan panduannya harus dihitung ulang.
 */
object LivenessFaceGuide {
    private val defaults = LivenessConfig()

    /** Lebar elips, rasio terhadap lebar viewfinder. */
    val widthFraction: Float = defaults.faceGuideWidthFraction

    /** Tinggi elips, rasio terhadap lebarnya sendiri. */
    val heightToWidth: Float = defaults.faceGuideHeightToWidth
}
