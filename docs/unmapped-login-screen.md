# Laporan UNMAPPED — Screen Login m-BCA

Screen **Login m-BCA** dari project Stitch (ID: `17b151aaf4cd4d45ad8e1329e9b8c306`)
memiliki beberapa nilai visual yang belum punya token di design system.

Per aturan `SKILL.md` §2.2: token tidak ditemukan = STOP.
Nilai-nilai ini harus ditambahkan **sebelum** screen digarap.

---

## 1. Alpha — perlu ditambah ke `AppAlpha` di `Color.kt`

Saat ini `AppAlpha` hanya punya dua nilai:

```kotlin
object AppAlpha {
    const val A10 = 0.10f
    const val A50 = 0.50f
}
```

Screen Login m-BCA membutuhkan empat alpha tambahan:

| Nilai | Dipakai untuk | Contoh class di HTML |
|-------|---------------|----------------------|
| `0.20` | Background logo, border tombol biometrik, background ikon quick links | `bg-white/20`, `border-white/20` |
| `0.30` | Garis divider "or login with" | `bg-white/30` |
| `0.70` | Teks divider "or login with" | `text-white/70` |
| `0.80` | Overlay latar belakang (efek glass) | `bg-primary/80` |

### Token yang perlu ditambahkan

```kotlin
object AppAlpha {
    const val A10 = 0.10f
    const val A20 = 0.20f   // baru
    const val A30 = 0.30f   // baru
    const val A50 = 0.50f
    const val A70 = 0.70f   // baru
    const val A80 = 0.80f   // baru
}
```

---

## 2. Ukuran komponen — perlu ditambah ke `AppSize` di `Dimens.kt`

Saat ini `AppSize` hanya punya satu nilai:

```kotlin
object AppSize {
    val MinTouchTarget = 48.dp
}
```

Screen ini membutuhkan dua ukuran tambahan:

| Nilai | Dipakai untuk | Contoh class di HTML |
|-------|---------------|----------------------|
| `64.dp` | Tombol biometrik (Face ID / Fingerprint) | `w-16 h-16` (Tailwind default 16×4=64px) |
| `96.dp` | Container logo BCA | `w-24 h-24` (Tailwind default 24×4=96px) |

### Token yang perlu ditambahkan

```kotlin
object AppSize {
    val MinTouchTarget = 48.dp
    val BiometricButton = 64.dp   // baru
    val LogoContainer = 96.dp     // baru
}
```

---

## 3. Ikon — tidak tersedia di `material-icons-core`

Dependency saat ini hanya `androidx.compose.material:material-icons-core`.
Tiga ikon yang dipakai di screen ini tidak tersedia di artifact itu:

| Ikon desain | Dipakai untuk | Tersedia di core? |
|-------------|---------------|-------------------|
| `fingerprint` | Tombol login sidik jari | Tidak |
| `credit_card` | Quick link Flazz | Tidak |
| `language` | Quick link KlikBCA | Tidak |
| `face` | Tombol login wajah | Ya |
| `arrow_forward` | Ikon di tombol m-BCA Login | Ya |
| `info` | Quick link Info BCA | Ya |

### Opsi penyelesaian

**Opsi A — Tambah dependency `material-icons-extended`**

```toml
# gradle/libs.versions.toml
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
```

```kotlin
// app/build.gradle.kts
implementation(libs.androidx.compose.material.icons.extended)
```

Catatan: artifact ini besar (~36 MB), tapi R8/ProGuard akan menghapus ikon yang tidak dipakai
saat build release.

**Opsi B — Pakai ikon alternatif dari core**

| Ikon desain | Alternatif core |
|-------------|-----------------|
| `fingerprint` | `Icons.Default.Lock` |
| `credit_card` | `Icons.Default.Star` |
| `language` | `Icons.Default.Search` |

Hasil visual berbeda dari desain, tapi tidak butuh dependency tambahan.

---

## Instruksi untuk sesi Claude Code berikutnya

Setelah ketiga hal di atas diputuskan, copy-paste prompt di bawah ini:

```
Tambahkan token berikut, lalu implement screen Login m-BCA dari desain.

Token alpha (di AppAlpha, Color.kt):
- A20 = 0.20f
- A30 = 0.30f
- A70 = 0.70f
- A80 = 0.80f

Token ukuran (di AppSize, Dimens.kt):
- BiometricButton = 64.dp
- LogoContainer = 96.dp

Ikon: [pilih salah satu]
- (a) tambah dependency material-icons-extended
- (b) pakai ikon alternatif dari core

Tambahkan token dalam commit terpisah sebelum menggarap screen.
```

---

## Referensi

- Stitch project: `projects/11405127577840585953`
- Screen ID: `17b151aaf4cd4d45ad8e1329e9b8c306`
- Screen title: Login m-BCA
- Skill: `stitch-to-compose` (`.claude/skills/stitch-to-compose/SKILL.md`)
- Token file: `.claude/skills/stitch-to-compose/references/design-tokens.md`