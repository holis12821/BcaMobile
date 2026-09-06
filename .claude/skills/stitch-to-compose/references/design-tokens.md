# Design Tokens

Nilai di file ini diambil dari label hex pada dokumen desain dan diverifikasi silang dengan
sampling piksel swatch (cocok dalam ±2 per kanal). **Label adalah nilai resmi.**

Aturan pakai: composable **tidak boleh** menulis nilai dari file ini secara langsung.
Composable memanggil nama token. File ini dan file Kotlin di §6 adalah satu-satunya tempat
angka mentah boleh muncul.

---

## 1. Warna

### PRIMARY (biru)

| Step | Hex | Step | Hex |
|---|---|---|---|
| 100 | `#ABD9FF` | 600 | `#0077D9` |
| 200 | `#81C6FF` | 700 | `#0060AF` |
| 300 | `#58B3FF` | 800 | `#004B8A` |
| 400 | `#2EA0FF` | 900 | `#003764` |
| 500 | `#048EFF` | 1000 | `#00223F` |

### SECONDARY (biru abu)

| Step | Hex | Step | Hex |
|---|---|---|---|
| 100 | `#EDF3F8` | 600 | `#6191BD` |
| 200 | `#DBE6F0` | 700 | `#477CAB` |
| 300 | `#BCD1E3` | 800 | `#3B658C` |
| 400 | `#9EBCD6` | 900 | `#2E4F6E` |
| 500 | `#7FA6CA` | 1000 | `#21394F` |

### SUCCESS (hijau)

| Step | Hex | Step | Hex |
|---|---|---|---|
| 100 | `#D5FADE` | 600 | `#18DB43` |
| 200 | `#ACF6BC` | 700 | `#15BA39` |
| 300 | `#82F19B` | 800 | `#11992F` |
| 400 | `#59ED79` | 900 | `#0D7925` |
| 500 | `#2FE858` | 1000 | `#0A581B` |

### DANGER (merah)

| Step | Hex | Step | Hex |
|---|---|---|---|
| 100 | `#FECED2` | 600 | `#D00416` |
| 200 | `#FD9EA6` | 700 | `#B20313` |
| 300 | `#FC6D79` | 800 | `#930310` |
| 400 | `#FB3C4D` | 900 | `#75020C` |
| 500 | `#FA0B20` | 1000 | `#560209` |

### NEUTRAL (abu)

| Step | Hex | Step | Hex |
|---|---|---|---|
| 100 | `#FFFFFF` | 600 | `#8E8E8E` |
| 200 | `#E8E8E8` | 700 | `#777777` |
| 300 | `#D2D2D2` | 800 | `#606060` |
| 400 | `#BBBBBB` | 900 | `#4A4A4A` |
| 500 | `#A4A4A4` | 1000 | `#333333` |

### Varian alpha (`10` dan `50`)

Di dokumen desain, kolom `10` dan `50` digambar sebagai checkerboard — artinya **transparansi**,
bukan warna baru. Jangan dibuat sebagai konstanta hex tersendiri.

| Ramp | Warna dasar | `10` | `50` |
|---|---|---|---|
| Primary | `Primary700` `#0060AF` | alpha 0.10 | alpha 0.50 |
| Secondary | `Secondary200` `#DBE6F0` | alpha 0.10 | alpha 0.50 |
| Success | `Success500` `#2FE858` | alpha 0.10 | alpha 0.50 |
| Danger | `Danger600` `#D00416` | alpha 0.10 | alpha 0.50 |
| Neutral | `Neutral1000` `#333333` | alpha 0.10 | — |

Di Compose: `AppColor.Primary700.copy(alpha = 0.10f)`.

---

## 2. Spacing

| Token | dp | Token | dp |
|---|---|---|---|
| `#0` | 2 | `#6` | 24 |
| `#1` | 4 | `#7` | 32 |
| `#2` | 8 | `#8` | 40 |
| `#3` | 12 | `#9` | 48 |
| `#4` | 16 | `#10` | 56 |
| `#5` | 20 | | |

Penamaan Kotlin sengaja 1:1 dengan desain (`s0`…`s10`) supaya tidak ada ruang salah petakan.

---

## 3. Radius

| Token | dp | Token | dp |
|---|---|---|---|
| `#none` | 0 | `#4` | 8 |
| `#1` | 2 | `#5` | 10 |
| `#2` | 4 | `#6` | 12 |
| `#3` | 6 | `#7` | 16 |
| `#full` | 999 (pill / lingkaran) | | |

`#full` di Compose = `CircleShape`, bukan `RoundedCornerShape(999.dp)`.

---

## 4. Stroke

| Token | dp |
|---|---|
| `#0` | 1 |
| `#1` | 2 |
| `#2` | 4 |
| `#3` | 6 |

---

## 5. Tipografi

**Yang didefinisikan desain:**

- Font family: **Inter**
- Weight: Light 300, Regular 400, Medium 500, Bold 700

**Dikonfirmasi oleh Stitch design system "Modern Financial Interface".**

Type scale berikut sudah **CONFIRMED**. SemiBold (600) di-map ke `inter_medium.ttf`
karena file font `inter_semibold.ttf` belum tersedia.

| Peran | Size | Line height | Weight |
|---|---|---|---|
| displayLarge | 32sp | 40sp | Bold |
| displaySmall | 28sp | 36sp | Bold |
| headlineMedium | 20sp | 28sp | SemiBold |
| titleLarge | 22sp | 28sp | Bold |
| titleMedium | 18sp | 24sp | Medium |
| titleSmall | 16sp | 22sp | Medium |
| bodyLarge | 16sp | 24sp | Regular |
| bodyMedium | 14sp | 20sp | Regular |
| bodySmall | 12sp | 16sp | Regular |
| labelLarge | 14sp | 20sp | SemiBold |
| labelMedium | 12sp | 16sp | Medium |
| labelSmall | 10sp | 14sp | Medium |

---

## 6. Kotlin siap pakai

Package di bawah mengikuti project `bca_mobile`. **Verifikasi terhadap `namespace` di
`app/build.gradle.kts`** dan sesuaikan kalau berbeda.

Empat file ini adalah **satu-satunya** tempat angka mentah boleh muncul.

### `Color.kt`

```kotlin
package id.bca.bcamobile.ui.theme

import androidx.compose.ui.graphics.Color

object AppColor {
    // PRIMARY
    val Primary100 = Color(0xFFABD9FF)
    val Primary200 = Color(0xFF81C6FF)
    val Primary300 = Color(0xFF58B3FF)
    val Primary400 = Color(0xFF2EA0FF)
    val Primary500 = Color(0xFF048EFF)
    val Primary600 = Color(0xFF0077D9)
    val Primary700 = Color(0xFF0060AF)
    val Primary800 = Color(0xFF004B8A)
    val Primary900 = Color(0xFF003764)
    val Primary1000 = Color(0xFF00223F)

    // SECONDARY
    val Secondary100 = Color(0xFFEDF3F8)
    val Secondary200 = Color(0xFFDBE6F0)
    val Secondary300 = Color(0xFFBCD1E3)
    val Secondary400 = Color(0xFF9EBCD6)
    val Secondary500 = Color(0xFF7FA6CA)
    val Secondary600 = Color(0xFF6191BD)
    val Secondary700 = Color(0xFF477CAB)
    val Secondary800 = Color(0xFF3B658C)
    val Secondary900 = Color(0xFF2E4F6E)
    val Secondary1000 = Color(0xFF21394F)

    // SUCCESS
    val Success100 = Color(0xFFD5FADE)
    val Success200 = Color(0xFFACF6BC)
    val Success300 = Color(0xFF82F19B)
    val Success400 = Color(0xFF59ED79)
    val Success500 = Color(0xFF2FE858)
    val Success600 = Color(0xFF18DB43)
    val Success700 = Color(0xFF15BA39)
    val Success800 = Color(0xFF11992F)
    val Success900 = Color(0xFF0D7925)
    val Success1000 = Color(0xFF0A581B)

    // DANGER
    val Danger100 = Color(0xFFFECED2)
    val Danger200 = Color(0xFFFD9EA6)
    val Danger300 = Color(0xFFFC6D79)
    val Danger400 = Color(0xFFFB3C4D)
    val Danger500 = Color(0xFFFA0B20)
    val Danger600 = Color(0xFFD00416)
    val Danger700 = Color(0xFFB20313)
    val Danger800 = Color(0xFF930310)
    val Danger900 = Color(0xFF75020C)
    val Danger1000 = Color(0xFF560209)

    // NEUTRAL
    val Neutral100 = Color(0xFFFFFFFF)
    val Neutral200 = Color(0xFFE8E8E8)
    val Neutral300 = Color(0xFFD2D2D2)
    val Neutral400 = Color(0xFFBBBBBB)
    val Neutral500 = Color(0xFFA4A4A4)
    val Neutral600 = Color(0xFF8E8E8E)
    val Neutral700 = Color(0xFF777777)
    val Neutral800 = Color(0xFF606060)
    val Neutral900 = Color(0xFF4A4A4A)
    val Neutral1000 = Color(0xFF333333)
}

// Varian alpha — turunan, bukan warna baru.
object AppAlpha {
    const val A10 = 0.10f
    const val A20 = 0.20f
    const val A30 = 0.30f
    const val A50 = 0.50f
    const val A60 = 0.60f
    const val A70 = 0.70f
    const val A80 = 0.80f
    const val A90 = 0.90f
}

// M3 Semantic Colors — lihat §7 untuk tabel lengkap.
// Didefinisikan di Color.kt sebagai object M3Color.
// Dipakai oleh LightColorScheme di Theme.kt.
```

### `Dimens.kt`

```kotlin
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
}
```

### `Shape.kt`

```kotlin
package id.bca.bcamobile.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Corner radius — #none..#7 plus #full. */
object AppShape {
    val None = RoundedCornerShape(0.dp)
    val R1 = RoundedCornerShape(2.dp)
    val R2 = RoundedCornerShape(4.dp)
    val R3 = RoundedCornerShape(6.dp)
    val R4 = RoundedCornerShape(8.dp)
    val R5 = RoundedCornerShape(10.dp)
    val R6 = RoundedCornerShape(12.dp)
    val R7 = RoundedCornerShape(16.dp)
    val Full = CircleShape
}
```

### `Type.kt` — CONFIRMED (Stitch design system)

```kotlin
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
```

> **Selalu pakai parameter bernama.** Parameter posisional pertama `TextStyle` adalah `color`,
> **bukan** `fontFamily` — menulis `TextStyle(Inter, …)` gagal kompilasi dengan
> *type mismatch: FontFamily vs Color*.

---

## 7. Pemetaan semantik ke Material3

Project memakai Material3 polos. Pemetaan berikut **CONFIRMED** — dikonfirmasi oleh
Stitch design system "Modern Financial Interface" (seed `#0077C8`, variant FIDELITY).
Skema gelap belum didefinisikan desain dan merupakan kondisi STOP tersendiri.

Warna M3 didefinisikan di `M3Color` object dalam `Color.kt`. Numbered palette (`AppColor.*`)
tetap tersedia untuk referensi langsung di composable (e.g. teks di atas primary background).

| Slot M3 | Token (`M3Color.*`) | Hex |
|---|---|---|
| `primary` | `Primary` | `#005E9F` |
| `onPrimary` | `OnPrimary` | `#FFFFFF` |
| `primaryContainer` | `PrimaryContainer` | `#0077C8` |
| `onPrimaryContainer` | `OnPrimaryContainer` | `#FBFBFF` |
| `inversePrimary` | `InversePrimary` | `#9FCAFF` |
| `secondary` | `Secondary` | `#2F628C` |
| `onSecondary` | `OnSecondary` | `#FFFFFF` |
| `secondaryContainer` | `SecondaryContainer` | `#9ECEFD` |
| `onSecondaryContainer` | `OnSecondaryContainer` | `#235881` |
| `tertiary` | `Tertiary` | `#006B1A` |
| `onTertiary` | `OnTertiary` | `#FFFFFF` |
| `tertiaryContainer` | `TertiaryContainer` | `#008723` |
| `onTertiaryContainer` | `OnTertiaryContainer` | `#F5FFEE` |
| `error` | `Error` | `#BA1A1A` |
| `onError` | `OnError` | `#FFFFFF` |
| `errorContainer` | `ErrorContainer` | `#FFDAD6` |
| `onErrorContainer` | `OnErrorContainer` | `#93000A` |
| `background` | `Background` | `#FAF9F9` |
| `onBackground` | `OnBackground` | `#1B1C1C` |
| `surface` | `Surface` | `#FAF9F9` |
| `onSurface` | `OnSurface` | `#1B1C1C` |
| `surfaceVariant` | `SurfaceVariant` | `#E3E2E2` |
| `onSurfaceVariant` | `OnSurfaceVariant` | `#404751` |
| `surfaceTint` | `SurfaceTint` | `#0061A5` |
| `surfaceBright` | `SurfaceBright` | `#FAF9F9` |
| `surfaceDim` | `SurfaceDim` | `#DBDAD9` |
| `surfaceContainer` | `SurfaceContainer` | `#EFEDED` |
| `surfaceContainerHigh` | `SurfaceContainerHigh` | `#E9E8E8` |
| `surfaceContainerHighest` | `SurfaceContainerHighest` | `#E3E2E2` |
| `surfaceContainerLow` | `SurfaceContainerLow` | `#F4F3F3` |
| `surfaceContainerLowest` | `SurfaceContainerLowest` | `#FFFFFF` |
| `inverseSurface` | `InverseSurface` | `#2F3031` |
| `inverseOnSurface` | `InverseOnSurface` | `#F2F0F0` |
| `outline` | `Outline` | `#717783` |
| `outlineVariant` | `OutlineVariant` | `#C0C7D3` |

Warna sukses tidak punya slot di Material3. Akses lewat `AppColor.Success*` langsung — itu
tetap token, jadi tidak melanggar aturan hardcode.

### Dynamic color wajib dimatikan

Template bawaan Android Studio menyalakan dynamic color untuk Android 12+. Dynamic color
mengambil warna dari wallpaper pengguna dan **menimpa seluruh `colorScheme`** — artinya
semua token warna di atas tidak akan terlihat sama sekali di perangkat Android 12 ke atas.

Di composable theme, pastikan:

```kotlin
dynamicColor: Boolean = false
```

Atau hapus cabang `dynamicColor` seluruhnya. Ini penyebab paling sering dari keluhan
"warnanya beda dari desain padahal token sudah benar".

### Skema gelap

Desain tidak mendefinisikan skema gelap. Template Android Studio sudah memuat
`DarkColorScheme` berisi warna ungu bawaan — kalau dibiarkan, pengguna dengan mode gelap
akan melihat palette template, bukan palette ini.

Dua pilihan, keduanya sah; yang tidak sah adalah membiarkan warna template:

1. **Kunci ke terang** — abaikan parameter `darkTheme`, selalu pakai `LightColors`.
   Paling jujur selama skema gelap belum didesain.
2. **Definisikan skema gelap** — keputusan design system tersendiri, bukan hasil menebak
   dari ramp yang ada.

---

## 8. Cara menambah token

1. Nilai baru dikonfirmasi ke pemilik desain lebih dulu.
2. Tambahkan ke file ini **dan** ke file Kotlin terkait, dalam commit tersendiri
   yang tidak mengandung perubahan screen.
3. Baru kerjakan screen-nya.

Menambah token di tengah pengerjaan screen tidak diperbolehkan — itulah cara palette bocor.
