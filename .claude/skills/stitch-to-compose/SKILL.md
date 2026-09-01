---
name: stitch-to-compose
description: Konversi desain UI/UX (Stitch, Figma, atau spesifikasi visual) menjadi Jetpack Compose Kotlin memakai design token yang sudah dikunci — bukan nilai hardcoded. Gunakan saat implementasi screen dari desain, convert screen Stitch/Figma ke composable, menambah komponen UI baru, setup atau menambah token theme (warna/spacing/radius/stroke/tipografi), review UI Compose, atau audit hardcoded color/dp. Trigger juga pada "convert desain ke Compose", "implement screen ini", "bikin composable dari desain", "kenapa warnanya beda dari desain", "cek hardcode", "tambah token", nama screen dari inventory (Splash, Welcome/Login, Kode Akses, Face ID, Touch ID, Beranda, Mutasi, Rentang Waktu, e-Wallet, Bukti Transaksi, Riwayat), dan istilah token (Primary 500, Spacing #4, Radius #5, Stroke #1). JANGAN dipakai untuk kerja non-UI (networking, repository, migrasi Gradle) atau untuk mengubah nilai token itu sendiri tanpa persetujuan.
---

# Stitch/Figma → Jetpack Compose

Skill ini mengatur **cara menerjemahkan desain menjadi Compose**, bukan cara mendesain.
Tujuannya satu: hasil implementasi cocok dengan desain **dan** tidak ada satu pun nilai
visual yang ditulis langsung di composable.

## 0. Prinsip dasar (baca sebelum apa pun)

**Sumber kebenaran ada tiga lapis, dengan wewenang berbeda. Tidak boleh ditukar:**

| Sumber | Berwenang atas | TIDAK berwenang atas |
|---|---|---|
| `references/screen-inventory.md` + screenshot di `docs/design/` | **Elemen apa yang ada**, hierarki, teks, state | Nilai numerik presisi |
| `references/design-tokens.md` | **Semua nilai visual** — warna, spacing, radius, stroke, tipografi | — |
| HTML/Tailwind dari Stitch | **Struktur layout dan nilai numerik**, untuk elemen yang sudah dikonfirmasi ada di desain | Elemen apa yang ada; alur dan navigasi |

**Stitch adalah sumber utama untuk mengerjakan layout** — HTML-nya memberi hierarki dan angka
presisi yang tidak bisa didapat dari screenshot. Tapi wewenangnya berhenti pada elemen yang
sudah lolos pemeriksaan konformitas.

**Dua hal yang bukan wewenang Stitch:**

1. **Elemen apa yang ada.** Pemeriksaan pada tiga screen project ini menemukan 11 elemen
   tambahan, 5 elemen hilang, dan 7 elemen berbeda — termasuk hilangnya FAB scan dan empty
   state. Karena itu §3 fase 2 mewajibkan pemeriksaan konformitas sebelum layout digarap.
2. **Alur dan navigasi.** Stitch menghasilkan screen satu per satu tanpa model perpindahan
   antar screen — filter Mutasi bahkan di-inline sehingga menghapus satu screen dari alur.
   Navigasi diatur skill `compose-architecture`, bukan diturunkan dari artefak Stitch.

Screenshot **tidak pernah** menjadi sumber angka. Sampling warna dari gambar meleset karena
anti-alias, shadow, overlay, dan kompresi.

**Lalu angka presisi diambil dari mana?** Dari skala token, bukan dari pengukuran.
Itu justru gunanya skala: lihat proporsi di screenshot, pilih step terdekat di skala
(`Spacing`, `AppShape`, `StrokeWidth`), pakai step itu. Skala spacing punya 11 step —
cukup untuk menyatakan jarak apa pun di layar mobile. Yang **dilarang** adalah menciptakan
nilai di luar skala. Kalau dua step sama-sama masuk akal dan bedanya terlihat, itu kondisi
STOP: tanya, jangan pilih sendiri.

## 1. Lampiran wajib per sesi

Sebelum menulis composable, pastikan tiga hal ini ada di konteks. Kalau salah satu tidak ada,
minta dulu — jangan mulai.

1. **Spesifikasi screen** — entri screen di `references/screen-inventory.md` **dan** screenshot
   desain di `docs/design/`. Keduanya, bukan salah satu. HTML dari Stitch boleh disertakan
   sebagai petunjuk struktur, tapi bukan spesifikasi (lihat §0).
2. **Isi file theme yang berlaku** — `Color.kt`, `Type.kt`, `Dimens.kt`, `Shape.kt`.
   Ini daftar nama yang valid. Tanpa ini agent tidak punya pilihan selain hardcode.
3. **`references/design-tokens.md`** — daftar token resmi beserta nilainya.

## 2. Aturan mutlak

Delapan aturan ini tidak bisa dinegosiasi dalam satu sesi implementasi.

1. **Tidak ada literal visual di composable.** Dilarang `Color(0xFF…)`, `.dp` telanjang,
   `.sp` telanjang, Dilarang keras hardcoded string literal langsung di UI compose seperti contoh : `Text("Login"), Button("Submit)"`
    serta text" apapun kecuali ui composable string yang diambil dari `res/values/strings.xml`. dan `RoundedCornerShape(12.dp)` inline. Semua lewat token.
   Satu-satunya file yang boleh memuat angka mentah: `Color.kt`, `Dimens.kt`, `Shape.kt`, `Type.kt`.
2. **Token tidak ditemukan → STOP.** Kalau desain memakai warna/ukuran yang tidak ada padanannya
   di daftar token, agent **berhenti dan melapor**. Dilarang: membuat token baru sendiri,
   memakai hex langsung, atau membulatkan ke token terdekat diam-diam.
   Penambahan token adalah keputusan design system, dikerjakan terpisah **sebelum** screen digarap.
3. **Semua string ke `strings.xml`, additive only.** Tidak ada teks hardcoded di composable.
   Tidak menghapus atau mengubah entri `strings.xml` yang sudah ada.
4. **Composable stateless.** Screen composable menerima `state` + lambda event.
   Tidak ada `ViewModel` di dalam composable UI; tidak ada pemanggilan repository/network.
   `modifier: Modifier = Modifier` selalu jadi parameter opsional pertama setelah parameter wajib.
5. **Satu screen per sesi.** Jangan menggarap satu flow sekaligus. Navigation dan wiring
   ViewModel adalah pekerjaan terpisah.
6. **Preview wajib** — minimal light + dark untuk setiap screen dan setiap komponen baru.
7. **Aksesibilitas bukan opsional** — touch target ≥ 48dp, setiap ikon/gambar interaktif punya
   `contentDescription`, ikon dekoratif eksplisit `contentDescription = null`.
8. **Reuse sebelum bikin.** Cek komponen yang sudah ada di project dulu. Bikin komponen baru
   hanya kalau tidak ada padanannya, dan letakkan di layer komponen — bukan di dalam file screen.
9. **Implementasi** - Sebelum anda memulai tolong jelaskan metode pengerjaan anda secara mendetail sebelum
   saya minta untuk mengerjakannya, audit terlebih dahulu, karna agent ai terkadang suka semaunya implementasi ini sangat berbahaya karna perubahan apapun
   susah untuk di tracking dan di cek meskipun ada version control. dan saya sebagai human juga harus tau perubahan apa
   yang ai ubah sebelum saya mengerjakannya.

## 3. Alur kerja per screen

Kerjakan berurutan. Jangan lompat ke fase 4 sebelum fase 2 bersih.

**Fase 1 — Inventory.** Baca artefak desain. Tulis daftar: elemen apa saja, hierarkinya,
teks yang muncul, state yang terlihat. Belum menulis kode.

**Fase 2 — Token diff.** Kumpulkan semua nilai visual dari artefak (warna, spacing, radius,
stroke, ukuran font). Petakan satu per satu ke token di `references/design-tokens.md`.
Hasilkan tabel: `nilai desain → nama token`. Nilai yang tidak punya padanan masuk daftar
**UNMAPPED**. Kalau daftar UNMAPPED tidak kosong → **STOP dan lapor** (aturan §2.2).

**Sebelum mengusulkan token baru, jawab dulu: apakah artefaknya sendiri sesuai desain?**
Screen hasil generate AI sering memakai nilai default framework (skala opacity Tailwind
10/20/…/90, ikon Material generik) yang tidak ada hubungannya dengan design system ini.
Nilai seperti itu **bukan** kekurangan token — itu penyimpangan artefak.

Laporan UNMAPPED wajib memuat, untuk setiap nilai:

1. **Elemen apa** yang memakainya, dan **apakah elemen itu ada** di
   `references/screen-inventory.md` atau di screenshot desain.
2. **Dugaan asal nilai**: dari desain, atau dari default framework.
3. **Rekomendasi**: tambah token, snap ke token terdekat yang sudah ada, atau perbaiki artefak.

Elemen yang tidak ada di desain tapi muncul di artefak **dilaporkan sebagai penyimpangan**,
bukan sebagai permintaan token. Menambah token untuk elemen yang bukan bagian desain berarti
menyalin improvisasi generator ke dalam design system secara permanen.

**Kategori token baru bukan urusan satu screen.** Menambah nilai ke kategori yang sudah ada
(misalnya satu step alpha) berbeda kelas dengan membuat kategori baru (misalnya dimensi
komponen). Kategori baru butuh aturan penamaan yang diputuskan lebih dulu, bukan diputuskan
sambil menggarap screen.

**Fase 3 — Skeleton.** Susun struktur layout saja: `Scaffold`, `Column`/`Row`/`Box`/`LazyColumn`,
tanpa styling detail. Pastikan hierarki cocok dengan urutan DOM/spec, bukan dengan tebakan visual.

**Fase 4 — Detail.** Isi styling dari tabel token fase 2. Ekstrak sub-komponen yang berulang.

**Fase 5 — State.** Tambahkan state holder (`data class …UiState`) dan lambda event.
Tangani minimal: loading, empty, error, success — lihat §6.

**Fase 6 — Preview + self-check.** Tulis preview light/dark, lalu jalankan checklist §8.

## 4. Token

Daftar lengkap ada di **`references/design-tokens.md`**, termasuk kode Kotlin siap pakai untuk
`Color.kt`, `Dimens.kt`, `Shape.kt`, `Type.kt`.

Ringkas:

- **Warna** — 5 ramp (Primary, Secondary, Success, Danger, Neutral), skala 100–1000.
  Varian `10` dan `50` adalah **alpha**, bukan warna baru: `warna.copy(alpha = 0.1f)` / `0.5f`.
- **Spacing** — `#0`–`#10` = 2, 4, 8, 12, 16, 20, 24, 32, 40, 48, 56 dp.
- **Radius** — `#none`–`#7` = 0, 2, 4, 6, 8, 10, 12, 16 dp, plus `#full`.
- **Stroke** — `#0`–`#3` = 1, 2, 4, 6 dp.
- **Font** — Inter; weight Light 300, Regular 400, Medium 500, Bold 700.

**Catatan penting:** desain sumber **tidak mendefinisikan type scale** (ukuran font & line height
per gaya teks). Sampai type scale disepakati, setiap ukuran font adalah kondisi STOP.
Usulan default ada di `references/design-tokens.md` §5, ditandai `PROPOSED` — harus dikonfirmasi
manusia sebelum dipakai.

## 5. Mapping HTML/Tailwind → Compose

Tabel lengkap dan anti-pattern beserta contoh kode ada di
**`references/mapping-html-to-compose.md`**. Baca file itu sebelum fase 3.

Yang paling sering salah kalau tidak dicek:

- `gap-*` → `Arrangement.spacedBy()`, **bukan** padding per anak.
- list yang bisa panjang → `LazyColumn`, **bukan** `Column` + `verticalScroll`.
- `position: absolute` → `Box` + `Modifier.align`, **bukan** `Modifier.offset`.
- `w-full` → `fillMaxWidth()`, **bukan** lebar tetap hasil ukur screenshot.
- `hover:` → tidak ada padanan di mobile; abaikan, jangan dipaksa jadi `pressed`.

## 6. Yang tidak ada di desain

Desain visual biasanya hanya menggambarkan happy path. Empat state ini **wajib ada** di
implementasi meski tidak digambar:

| State | Aturan |
|---|---|
| Loading | Wajib. Kalau desain tidak menentukan bentuknya, pakai pola yang sudah ada di project; kalau belum ada → tanya. |
| Empty | Wajib untuk semua list. Teks empty harus dikonfirmasi, jangan dikarang. |
| Error | Wajib. Termasuk aksi retry. |
| Long content | Teks panjang, nama panjang, nominal besar — pastikan tidak overflow. |

Task flow di `references/screen-inventory.md` mendefinisikan sebagian state ini secara eksplisit
(contoh: daftar mutasi kosong). Pakai yang sudah didefinisikan; sisanya tanya.

## 6b. Batas dengan skill `compose-architecture`

Skill ini mengurus **penampilan**: token, layout, susunan elemen. Struktur navigasi, state,
dan batas ViewModel diatur skill `compose-architecture`.

Kalau pekerjaan menyentuh route, graph, back stack, `popUpTo`, ViewModel, atau `UiState` —
itu wilayah skill tersebut, termasuk keputusan apakah sebuah screen menjadi `composable()`
atau `dialog()`.

Task flow di `references/screen-inventory.md` adalah spesifikasi **perilaku**; penerjemahannya
ke graph bukan urusan skill ini.

## 7. Integrasi ke app

Project ini campuran XML dan Compose. Tentukan per screen, jangan diseragamkan:

- **Screen sudah ada sebagai Activity/Fragment** → pasang lewat `ComposeView`, pertahankan
  entry point yang ada. Jangan mengubah manifest atau navigation graph yang sudah jalan.
- **Screen baru berdiri sendiri** → Navigation Compose, route dideklarasikan terpisah.

Kalau tidak jelas screen-nya masuk kategori mana → tanya, jangan pilih sendiri.

## 8. Definition of Done

Sebuah screen dianggap selesai kalau semua ini terpenuhi. Jangan laporkan selesai sebelum
setiap baris dicek satu per satu.

- [ ] Daftar UNMAPPED kosong — semua nilai visual memakai token.
- [ ] Tidak ada `Color(0xFF`, `.dp` telanjang, `.sp` telanjang di file composable.
- [ ] Tidak ada string hardcoded; entri `strings.xml` bersifat penambahan saja.
- [ ] Composable stateless; tidak ada ViewModel/repository di dalamnya.
- [ ] `modifier: Modifier = Modifier` tersedia di setiap composable publik.
- [ ] Preview light dan dark ada, dan keduanya render tanpa error.
- [ ] Screenshot hasil dibandingkan dengan screenshot desain — perbedaan yang tersisa dicatat.
- [ ] Loading / empty / error tertangani.
- [ ] Touch target ≥ 48dp; `contentDescription` terisi atau eksplisit `null`.
- [ ] Font scale 1.3× tidak menyebabkan teks terpotong.
- [ ] Layar sempit (360dp) tidak menyebabkan horizontal overflow.

## 9. Enforcement

Prompt saja tidak menutup celah hardcode. Pasang pemeriksa mekanis:

```bash
# gagal kalau ada literal warna/dimensi dan string di luar file token
#!/usr/bin/env bash

# Enforce UI agar tidak menggunakan:
# - hardcoded color
# - hardcoded dimension
# - hardcoded typography size
# - hardcoded user-facing UI string
#
# Catatan:
# String yang bersifat internal/data seperti:
# - animation label
# - transaction title/description
# - log message
# - URL
# - API key
# tidak dianggap sebagai hardcoded UI.

set -uo pipefail

SRC="app/src/main/java"

# File yang diperbolehkan berisi token UI.
TOKEN_FILES='(Color|Dimens|Shape|Type|Strings|StringResource|Typography|Theme)\.kt'

FAILED=0

echo "======================================"
echo " Hardcoded UI Check"
echo "======================================"
echo ""

# --------------------------------------------------
# 1. Hardcoded Color
# --------------------------------------------------

echo "[1/4] Checking hardcoded colors..."

color_hits=$(
  grep -rnE \
    --include='*.kt' \
    'Color\(0x[0-9A-Fa-f]{8}\)' \
    "$SRC" \
    | grep -vE "$TOKEN_FILES" \
    || true
)

if [ -n "$color_hits" ]; then
  echo "FAIL: hardcoded color ditemukan:"
  echo "$color_hits"
  FAILED=1
else
  echo "OK: tidak ada hardcoded color"
fi

echo ""

# --------------------------------------------------
# 2. Hardcoded Dimension / Typography
# --------------------------------------------------

echo "[2/4] Checking hardcoded dimensions..."

dimension_hits=$(
  grep -rnE \
    --include='*.kt' \
    '(^|[^a-zA-Z0-9_])[0-9]+(\.[0-9]+)?\.(dp|sp)\b' \
    "$SRC" \
    | grep -vE "$TOKEN_FILES" \
    || true
)

if [ -n "$dimension_hits" ]; then
  echo "FAIL: hardcoded dimension ditemukan:"
  echo "$dimension_hits"
  FAILED=1
else
  echo "OK: tidak ada hardcoded dimension"
fi

echo ""

# --------------------------------------------------
# 3. Hardcoded UI String
# --------------------------------------------------

echo "[3/4] Checking hardcoded UI strings..."

string_hits=$(
  grep -rnE \
    --include='*.kt' \
    'Text\(\s*"[^"]+"|contentDescription\s*=\s*"[^"]+"|placeholder\s*=\s*"[^"]+"|setText\(\s*"[^"]+"|Toast\.makeText\([^,]+,\s*"[^"]+"' \
    "$SRC" \
    | grep -vE "$TOKEN_FILES" \
    || true
)

if [ -n "$string_hits" ]; then
  echo "FAIL: hardcoded UI string ditemukan:"
  echo "$string_hits"
  FAILED=1
else
  echo "OK: tidak ada hardcoded UI string"
fi

echo ""

# --------------------------------------------------
# 4. Hardcoded UI String Property
# --------------------------------------------------

echo "[4/4] Checking user-facing UI properties..."

property_hits=$(
  grep -rnE \
    --include='*.kt' \
    '(supportingText|error|placeholderText|accessibilityLabel|hint)\s*=\s*"[^"]+"' \
    "$SRC" \
    | grep -vE "$TOKEN_FILES" \
    || true
)

if [ -n "$property_hits" ]; then
  echo "FAIL: hardcoded UI property string ditemukan:"
  echo "$property_hits"
  FAILED=1
else
  echo "OK: tidak ada hardcoded UI property string"
fi

echo ""

# --------------------------------------------------
# Result
# --------------------------------------------------

echo "======================================"

if [ "$FAILED" -ne 0 ]; then
  echo "FAIL: hardcoded UI ditemukan."
  echo "Gunakan stringResource()/strings.xml atau token UI yang sesuai."
  echo "======================================"
  exit 1
fi

echo "PASS: tidak ditemukan hardcoded UI."
echo "======================================"

exit 0
```

Jalankan di pre-commit hook atau step CI. Aturan Detekt custom (`ForbiddenMethodCall` /
regex rule) lebih rapi kalau project sudah memakai Detekt.

## 10. Referensi

- `references/design-tokens.md` — nilai token + kode Kotlin siap pakai.
- `references/mapping-html-to-compose.md` — tabel mapping + anti-pattern.
- `references/screen-inventory.md` — daftar screen, komponen berulang, dan task flow.
