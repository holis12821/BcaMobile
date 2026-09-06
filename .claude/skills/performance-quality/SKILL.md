---
name: performance-quality
description: Panduan performa, keamanan, dan kenyamanan user untuk bca_mobile. Gunakan saat membuat screen baru, menambah fitur, review kode, audit performa, atau saat ada keluhan app lambat/berat/ANR. Trigger juga pada "app berat", "lambat", "ANR", "lag", "jank", "recomposition", "memory leak", "baterai boros", "startup lambat", "scroll lag", "freeze", "optimasi", "R8", "ProGuard", "blur", "LazyColumn", "infinite animation", "font loading", "stable", "immutable", dan "performa". JANGAN dipakai untuk urusan visual/token (itu `stitch-to-compose`) atau navigasi/state (itu `compose-architecture`).
---

# Performa, Keamanan & Kualitas — bca_mobile

Membuat aplikasi bukan sekadar membuat — aplikasi harus **cepat, aman, dan nyaman** di
tangan user. Skill ini memastikan setiap kode yang ditulis memperhatikan ketiga aspek itu.

Dua skill lain mengatur wilayah mereka sendiri:
- `stitch-to-compose` — penampilan visual dan token
- `compose-architecture` — navigasi, state, dan keamanan sesi

Skill ini mengatur **bagaimana kode ditulis agar tidak merugikan user**.

---

## 0. Prinsip: User Tidak Peduli Kode Kamu Rapi

User hanya merasakan tiga hal:
1. **Cepat atau lambat** — startup, transisi, scroll, respons tap
2. **Aman atau tidak** — data bocor, session hijack, screenshot sensitif
3. **Nyaman atau frustasi** — error handling, feedback, aksesibilitas

Kode yang bersih tapi lambat tetap gagal. Kode yang jalan tapi tidak aman tetap gagal.
Ketiganya harus dipenuhi **bersamaan**, bukan dipilih salah satu.

---

## 1. Aturan Performa — Compose

### 1.1 Jangan Buat Objek Baru di Setiap Rekomposisi

```kotlin
// SALAH — UiState baru dibuat setiap recomposition
composable<Beranda> {
    BerandaScreen(state = BerandaUiState(), ...)
}

// BENAR — state dari ViewModel atau remember
composable<Beranda> {
    val viewModel: BerandaViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    BerandaScreen(state = state, ...)
}
```

Ini berlaku untuk: UiState, List, lambda, dan objek apa pun yang diteruskan ke composable.

### 1.2 Jangan Pakai `Column + verticalScroll` untuk List

```kotlin
// SALAH — semua item di-compose sekaligus
Column(Modifier.verticalScroll(rememberScrollState())) {
    items.forEach { item -> ItemRow(item) }
}

// BENAR — hanya item yang terlihat di-compose
LazyColumn {
    items(items, key = { it.id }) { item -> ItemRow(item) }
}
```

Kapan `Column + verticalScroll` boleh: kalau jumlah elemen **tetap dan sedikit** (< 10)
dan tidak ada list dinamis di dalamnya. Contoh: form input, settings screen sederhana.

Kapan wajib `LazyColumn`/`LazyRow`: kalau data bisa tumbuh, atau kalau ada list
di dalam scrollable container.

### 1.3 Infinite Animation Harus Berhenti Saat Tidak Relevan

```kotlin
// SALAH — animasi berjalan walau status bukan SCANNING
val transition = rememberInfiniteTransition()
val progress by transition.animateFloat(...)
if (status == SCANNING) {
    // pakai progress
}

// BENAR — animasi hanya hidup saat dibutuhkan
if (status == SCANNING) {
    val transition = rememberInfiniteTransition()
    val progress by transition.animateFloat(...)
    // pakai progress
}
```

`rememberInfiniteTransition` berjalan selama composable hidup di composition tree.
Menyembunyikan output-nya tidak menghentikan kalkulasinya.

### 1.4 Jangan Pakai `Modifier.blur()` di Content yang Scroll

`Modifier.blur()` fallback ke software rendering di API < 31 (device minSdk 26).
Software blur bisa memakan 10-30ms per frame — lebih lama dari budget 16ms per frame.

Alternatif:
- Gunakan `background` dengan alpha rendah
- Pre-render efek blur sebagai gambar statis (PNG/WebP kecil)
- Gunakan `drawBehind` dengan gradient manual

### 1.5 Gunakan `key` di LazyColumn/LazyRow

```kotlin
// SALAH — Compose tidak bisa track item
LazyColumn {
    items(transactions) { tx -> TransactionRow(tx) }
}

// BENAR — Compose bisa reuse composable saat list berubah
LazyColumn {
    items(transactions, key = { it.id }) { tx -> TransactionRow(tx) }
}
```

Tanpa `key`, setiap perubahan di list menyebabkan recomposition seluruh list.

### 1.6 Gunakan ImmutableList untuk UiState

```kotlin
// SALAH — List adalah interface, Compose anggap unstable
data class BerandaUiState(
    val promoItems: List<PromoItem> = emptyList(),
)

// BENAR — ImmutableList dijamin stable
data class BerandaUiState(
    val promoItems: ImmutableList<PromoItem> = persistentListOf(),
)
```

`List<>` di Kotlin adalah interface yang bisa berubah-ubah implementation-nya.
Compose compiler tidak bisa membuktikan bahwa isinya sama, sehingga selalu
memicu recomposition. `ImmutableList` dari `kotlinx.collections.immutable`
dijamin stable.

### 1.7 Dekoratif Jangan Pakai Extra Compose Node

```kotlin
// SALAH — Box tambahan hanya untuk gradient
Box(modifier = Modifier.matchParentSize().background(brush = gradient))

// BENAR — drawBehind, tanpa compose node tambahan
Modifier.drawBehind { drawRect(brush = gradient) }
```

Setiap `Box`, `Row`, `Column` adalah compose node yang harus di-measure dan di-layout.
Untuk efek visual murni, gunakan `drawBehind`, `drawWithContent`, atau `drawWithCache`.

### 1.8 Hindari Recomposition Bottom Bar

Bottom bar yang di-compose ulang setiap navigasi menyebabkan "kedipan" dan jank.
Scaffold + BottomBar harus di-share antar tab, bukan dibuat per-tab.

---

## 2. Aturan Performa — Build & APK

### 2.1 R8 WAJIB Aktif di Release Build

```kotlin
// WAJIB di app/build.gradle.kts
release {
    optimization { enable = true }
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

Tanpa R8: APK 2-3x lebih besar, startup lebih lambat, method count lebih tinggi.
Tidak ada alasan untuk mematikan R8 di release build kecuali sedang debug masalah R8.

### 2.2 Jangan Hardcode Delay untuk Splash

```kotlin
// SALAH — user dipaksa tunggu 2 detik
delay(2000.milliseconds)
repository.markReady()

// BENAR — tunggu pekerjaan nyata, atau tanpa delay
// Jika ada inisialisasi:
val token = encryptedPrefs.getSessionToken()
if (token != null) validateToken(token)
repository.markReady()
```

Splash delay artifisial adalah anti-pattern. User membuka app untuk menyelesaikan
tugas, bukan untuk melihat logo.

### 2.3 Font: Pilih Variable Font atau Lazy Loading

4 file TTF terpisah = 400KB-1.5MB total. Compose memuat font synchronous
saat pertama kali dipakai.

Opsi:
1. **Variable font** — 1 file untuk semua weight, ukuran total lebih kecil
2. **Google Fonts provider** — async loading dengan system font fallback
3. **Subset font** — hapus glyph yang tidak dipakai (Latin-only untuk app ini)

### 2.4 Gambar: Vector > WebP > PNG

- Logo dan ikon sederhana → VectorDrawable (XML)
- Foto dan gambar kompleks → WebP (40-60% lebih kecil dari PNG)
- PNG hanya kalau tidak ada alternatif

---

## 3. Aturan Keamanan

### 3.1 Checklist Keamanan Wajib

Ini bukan opsional — ini minimum untuk aplikasi perbankan:

- [ ] `FLAG_SECURE` di semua Activity yang menampilkan data sensitif
- [ ] Kode akses dan PIN tidak pernah masuk `SavedStateHandle` atau log
- [ ] Kode akses dan PIN di-clear dari memori segera setelah dipakai
- [ ] Session timeout + auto-lock setelah background
- [ ] Kematian proses → state kembali ke `Locked`/`LoggedOut`
- [ ] Tidak ada data sensitif di shared preferences tanpa enkripsi
- [ ] WebView (jika ada) hardened: no JavaScript jika tidak perlu, no file access
- [ ] Certificate pinning untuk API calls (jika sudah ada networking)
- [ ] Tidak ada API key/secret di source code
- [ ] ProGuard/R8 aktif (obfuscation = satu layer pertahanan)

### 3.2 Kode Akses/PIN Handling

```kotlin
// SALAH — PIN disimpan di SavedStateHandle
savedStateHandle["pin"] = enteredPin

// SALAH — PIN masuk log
Log.d("Auth", "PIN entered: $pin")

// BENAR — PIN hanya di memory proses, di-clear segera
private val digits = mutableListOf<Int>()

fun submit(): Boolean {
    val result = verifyHash(digits)
    digits.clear()  // clear segera
    return result
}
```

### 3.3 Jangan Simpan State Sensitif di Recomposition

```kotlin
// SALAH — saldo tersimpan di UiState yang bisa di-restore
data class BerandaUiState(
    val balance: String = "Rp 12.500.000",  // hardcoded!
)

// BENAR — saldo datang dari repository, di-fetch ulang setelah proses mati
```

---

## 4. Aturan UX / Kenyamanan User

### 4.1 Feedback Harus Instan

- Tap → visual feedback dalam < 100ms (ripple, color change)
- Operasi > 300ms → tampilkan loading indicator
- Operasi > 2s → tampilkan progress dengan pesan
- Operasi gagal → pesan error spesifik + aksi (retry, back, help)

### 4.2 Error Bukan Dead End

```kotlin
// SALAH — error tanpa jalan keluar
Text("Terjadi kesalahan")

// BENAR — error dengan aksi
Column {
    Text("Koneksi gagal. Periksa jaringan Anda.")
    Button(onClick = onRetry) { Text("Coba Lagi") }
}
```

Setiap error state HARUS punya minimal satu aksi: retry, back, atau help.
User tidak boleh terjebak di layar error tanpa bisa melakukan apa-apa.

### 4.3 Aksesibilitas = Wajib, Bukan Fitur

- Touch target minimum 48dp — sudah diatur di `stitch-to-compose`
- Semua ikon interaktif punya `contentDescription`
- Ikon dekoratif eksplisit `contentDescription = null`
- Warna tidak boleh jadi satu-satunya pembeda informasi
- Font scale 1.3x tidak boleh menyebabkan teks terpotong

### 4.4 Scroll Harus Smooth

Target: 60fps di semua layar, termasuk device low-end.

Penyebab jank yang sering ditemui di project ini:
- `blur()` di scrollable content
- Object allocation di recomposition
- Semua item di-compose sekaligus (Column vs LazyColumn)
- PNG decode saat scroll

### 4.5 Jangan Blokir UI Thread

```kotlin
// SALAH — operasi di main thread
fun onSubmit() {
    val hash = hashPin(digits)  // bisa 50-200ms
    repository.verify(hash)
}

// BENAR — operasi di background
fun onSubmit() {
    viewModelScope.launch(Dispatchers.Default) {
        val hash = hashPin(digits)
        repository.verify(hash)
    }
}
```

Hashing, enkripsi, file I/O, network call — semua harus di background thread.
ANR terjadi kalau main thread diblokir > 5 detik.

---

## 5. Checklist Sebelum Lapor Selesai

Setiap kali menulis kode baru atau memodifikasi kode yang ada:

### Performa
- [ ] Tidak ada object allocation di dalam composable yang bisa di-`remember`
- [ ] List dinamis pakai `LazyColumn`/`LazyRow` dengan `key`
- [ ] Tidak ada `blur()` di scrollable content
- [ ] Infinite animation hanya hidup saat state-nya relevan
- [ ] Tidak ada delay artifisial yang memblokir user
- [ ] UiState pakai ImmutableList atau @Stable annotation

### Keamanan
- [ ] Tidak ada data sensitif di log
- [ ] Tidak ada credential hardcoded
- [ ] PIN/kode akses di-clear dari memori setelah dipakai
- [ ] `FLAG_SECURE` terpasang di layar sensitif

### UX
- [ ] Loading state ada
- [ ] Error state ada dengan aksi (retry/back)
- [ ] Empty state ada untuk list
- [ ] Touch target >= 48dp
- [ ] `contentDescription` terisi di elemen interaktif

---

## 6. Anti-Pattern yang Ditemukan di Project Ini

Daftar ini berdasarkan audit 2 September 2026. Item yang sudah diperbaiki ditandai ✅.

| # | Anti-Pattern | File | Severity | Status |
|---|---|---|---|---|
| 1 | R8 mati di release | `build.gradle.kts` | CRITICAL | ✅ Fixed |
| 2 | Splash delay 2 detik | `SessionViewModel.kt` | CRITICAL | ✅ Fixed |
| 3 | `blur()` di scrollable | `TransferScreen.kt` | CRITICAL | ✅ Fixed |
| 4 | Infinite animation saat tidak perlu | `FaceIdScreen.kt`, `FingerPrintScreen.kt` | MAJOR | ✅ Fixed |
| 5 | Crossfade 500ms di session | `BcaNavHost.kt` | MAJOR | ✅ Fixed (→200ms) |
| 6 | UiState baru per recomposition | `MainGraph.kt`, `EWalletGraph.kt` | MAJOR | ✅ Fixed (remember) |
| 7 | PNG logo bukan vector | `res/drawable/bca_logo_white.png` | MAJOR | Open |
| 8 | 4 TTF font terpisah | `res/font/inter_*.ttf` | MAJOR | Open |
| 9 | Bottom bar per-tab (bukan shared) | `MainGraph.kt` | MAJOR | Open |
| 10 | `List<>` di UiState (unstable) | Multiple UiState classes | MODERATE | Open |
| 11 | `Row + horizontalScroll` untuk promo | `HomeScreen.kt:595-608` | MODERATE | Open |
| 12 | `Column + verticalScroll` untuk list | `HomeScreen.kt:117`, `TransferScreen.kt:137` | MODERATE | Open |

Detail lengkap: `docs/design/performance-audit.md`

---

## Referensi

- `docs/design/performance-audit.md` — audit performa lengkap dengan line numbers
- [Android Vitals: ANR](https://developer.android.com/topic/performance/vitals/anr)
- [Jetpack Compose Performance](https://developer.android.com/develop/ui/compose/performance)
- [Compose Stability Explained](https://developer.android.com/develop/ui/compose/performance/stability)
- [R8 Full Mode](https://developer.android.com/build/shrink-code)