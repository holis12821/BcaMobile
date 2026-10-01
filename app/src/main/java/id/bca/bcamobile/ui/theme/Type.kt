package id.bca.bcamobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.bca.bcamobile.R

val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_medium, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

// Dikonfirmasi oleh Stitch design system "Modern Financial Interface".
val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Inter, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.64).sp),
    displaySmall = TextStyle(fontFamily = Inter, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = Inter, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge   = TextStyle(fontFamily = Inter, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium  = TextStyle(fontFamily = Inter, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    titleSmall   = TextStyle(fontFamily = Inter, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge    = TextStyle(fontFamily = Inter, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium   = TextStyle(fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall    = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    labelLarge   = TextStyle(fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium  = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall   = TextStyle(fontFamily = Inter, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
)

/**
 * Gaya teks di luar type scale [AppTypography].
 *
 * Artefak Stitch layar Notifikasi menyebut ukuran yang tidak ada di skala
 * (19/17/15/13sp). Nilainya dicatat di sini apa adanya, terpisah dari
 * [AppTypography], supaya skala resmi tidak ikut bergeser.
 *
 * Tinggi baris: artefak memakai rasio Tailwind (`leading-relaxed` = 1,625),
 * yang pada 13sp menghasilkan 21,1sp. Dibulatkan ke 20sp mengikuti ritme skala
 * — selisihnya satu piksel dan tidak terlihat.
 */
object AppTextStyle {
    val TopBarTitle = TextStyle(fontFamily = Inter, fontSize = 19.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
    val CardTitle   = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
    val CardAmount  = TextStyle(fontFamily = Inter, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
    val CardBody    = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)

    /**
     * Label pengelompokan tanggal. Ditulis kapital dengan jarak huruf longgar
     * (`tracking-wider` = 0,05em, jadi 0,6sp pada 12sp).
     */
    val GroupLabel  = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
}
