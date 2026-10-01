package id.bca.bcamobile.ui.theme

import androidx.compose.ui.unit.dp

/** Spacing scale — penomoran mengikuti dokumen desain (#0..#10). */
object Spacing {
    val s0 = 2.dp
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 20.dp
    val s6 = 24.dp
    val s7 = 32.dp
    val s8 = 40.dp
    val s9 = 48.dp
    val s10 = 56.dp
}

/**
 * Langkah setengah di antara dua step [Spacing].
 *
 * Artefak Stitch layar Notifikasi memakai jarak yang jatuh tepat di tengah
 * skala (Tailwind `gap-1.5`, `gap-2.5`, `gap-3.5`). Nilainya ditaruh di objek
 * tersendiri, **bukan** disisipkan ke [Spacing], supaya skala resmi tetap utuh
 * seperti dokumen desain — alasan yang sama dengan [AppSize.LoadMoreDot].
 */
object SpacingHalfStep {
    /** Antara [Spacing.s1] (4) dan [Spacing.s2] (8). */
    val h1 = 6.dp

    /** Antara [Spacing.s2] (8) dan [Spacing.s3] (12). */
    val h2 = 10.dp

    /** Antara [Spacing.s3] (12) dan [Spacing.s4] (16). */
    val h3 = 14.dp
}

/** Stroke width — #0..#3. */
object StrokeWidth {
    val w0 = 1.dp
    val w1 = 2.dp
    val w2 = 4.dp
    val w3 = 6.dp
}

/** Ukuran minimum yang tidak boleh dilanggar. */
object AppSize {
    val MinTouchTarget = 48.dp
    val IconSmall = 18.dp
    val IconLarge = 28.dp

    /**
     * Ukuran ikon eksplisit dari design system Stitch.
     *
     * [IconSmall] dan [IconLarge] tetap dipakai di layar lama. Lima nilai di bawah
     * ini menyatakan ukuran yang disebut desain secara langsung, jadi tidak perlu
     * dibulatkan ke dua nilai di atas. [Icon40] berasal dari artefak layar
     * Hubungi CS — ikon di dalam lingkaran kepala layar.
     */
    val Icon16 = 16.dp
    val Icon20 = 20.dp
    val Icon24 = 24.dp
    val Icon32 = 32.dp
    val Icon40 = 40.dp

    /**
     * Titik indikator "masih ada halaman berikutnya" di kaki daftar.
     *
     * 6dp disebut desain secara langsung dan tidak ada di skala [Spacing] — skala
     * itu lompat 4→8. Ditaruh di sini, bukan disisipkan ke skala, supaya skala
     * spacing tetap utuh seperti dokumen desain.
     */
    val LoadMoreDot = 6.dp
    val BiometricButton = 64.dp
    val LogoContainer = 96.dp
    val PromoImageHeight = 128.dp
    val SplashLogo = 192.dp
    val PromoCardWidth = 280.dp
    val ScannerFrame = 256.dp

    /** Render kartu debit: tinggi kartu dan ukuran chip EMV. */
    val CardChipWidth = 40.dp
    val CardChipHeight = 28.dp
    val DebitCardHeight = 176.dp

    /**
     * Ukuran dari artefak Stitch layar Notifikasi yang tidak ada di skala.
     *
     * [TopBarButton] adalah diameter lingkaran yang digambar desain (40dp);
     * area sentuhnya tetap [MinTouchTarget], jadi tombolnya boleh terlihat
     * 40dp tanpa melanggar batas sentuh.
     */
    val Icon26 = 26.dp
    val AvatarMedium = 44.dp
    val TopBarHeight = 64.dp
    val TopBarButton = 40.dp
    val BadgeMin = 20.dp
    val UnreadDot = 8.dp
    val EndOfListBar = 32.dp

    /**
     * Ukuran dari artefak Stitch layar Hubungi CS.
     *
     * [IconCircle] dan [IconBox] adalah alas ikon — lingkaran di kartu Call
     * Center dan kotak bersudut di kartu kanal. Nilainya kebetulan sama dengan
     * [MinTouchTarget] dan [Spacing.s10], tapi maknanya berbeda: yang satu batas
     * sentuh, yang satu jarak, yang ini ukuran gambar. Menyamakannya berarti
     * mengubah tiga hal sekaligus saat salah satunya digeser desain.
     *
     * [ButtonCompact] adalah tinggi tombol sebaris di dalam kartu; lebih pendek
     * dari tombol utama tapi tetap di atas [MinTouchTarget].
     */
    val IconCircle = 48.dp
    val IconBox = 56.dp
    val ButtonCompact = 56.dp
    val ContactAvatar = 80.dp
}

/**
 * Ketinggian bayangan.
 *
 * Kategori ini lahir dari artefak Stitch layar Notifikasi, yang memakai
 * `shadow-sm` pada kartu dan `shadow-md` pada app bar. `shadow-inner` di
 * lingkaran ikon **tidak punya padanan di Compose** dan dilewati — Compose
 * hanya menggambar bayangan ke luar.
 */
object Elevation {
    val None = 0.dp
    val Card = 1.dp
    val Bar = 3.dp
}
