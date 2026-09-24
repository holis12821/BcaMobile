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
     * [IconSmall] dan [IconLarge] tetap dipakai di layar lama. Empat nilai di bawah
     * ini menyatakan ukuran yang disebut desain secara langsung, jadi tidak perlu
     * dibulatkan ke dua nilai di atas.
     */
    val Icon16 = 16.dp
    val Icon20 = 20.dp
    val Icon24 = 24.dp
    val Icon32 = 32.dp
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
}