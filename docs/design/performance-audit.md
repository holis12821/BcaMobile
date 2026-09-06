# Audit Performa, ANR & UX — bca_mobile

Tanggal audit: 2 September 2026
Basis: seluruh kode sumber di `app/src/main/java/` dan `app/build.gradle.kts`.

---

## Ringkasan

Aplikasi terasa berat di real device karena **akumulasi banyak masalah kecil-menengah**,
bukan satu bug tunggal. Tiga penyebab terbesar:

1. R8/ProGuard mati total di release build
2. Splash dipaksa 2 detik walau aplikasi sudah siap
3. `Modifier.blur()` dan rekomposisi berlebihan

Perbaikan diterapkan pada 2 September 2026 — semua item CRITICAL dan MAJOR sudah diperbaiki.

---

## Severity

| Level | Arti |
|---|---|
| CRITICAL | Langsung menyebabkan ANR atau jank yang terasa di semua device |
| MAJOR | Memperberat app secara signifikan, terutama di device low-end |
| MODERATE | Menyebabkan rekomposisi berlebihan atau pemborosan memori |
| LOW | Code quality yang berdampak kecil pada performa |

---

## CRITICAL

### C1. R8/Optimization Disabled di Release Build

**File:** `app/build.gradle.kts:21-26`
```kotlin
buildTypes {
    release {
        optimization {
            enable = false
        }
    }
}
```

**Dampak:**
- APK tidak di-shrink: semua kode yang tidak terpakai tetap masuk APK
- APK tidak di-optimize: bytecode tidak dioptimalkan
- APK tidak di-obfuscate: ukuran class/method name membengkak
- Resource tidak di-shrink: drawable/string yang tidak terpakai tetap masuk

Ini satu-satunya alasan terbesar kenapa APK besar dan lambat. Aplikasi Compose
tanpa R8 bisa 2-3x lebih besar dan secara terukur lebih lambat pada startup.

**Rekomendasi:** Aktifkan R8 dengan ProGuard rules yang benar:
```kotlin
release {
    optimization {
        enable = true
    }
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

### C2. Splash Delay 2 Detik Hardcoded

**File:** `session/SessionViewModel.kt:18-21`
```kotlin
viewModelScope.launch {
    delay(SPLASH_MIN_DURATION_MS.milliseconds)  // 2000ms
    repository.markReady()
}
```

**Dampak:** User WAJIB menunggu 2 detik penuh setiap buka aplikasi, bahkan kalau
aplikasi sudah siap dalam 100ms. Ini terasa seperti ANR bagi user.

**Rekomendasi:** Hapus delay artifisial. Kalau ada pekerjaan inisialisasi nyata
(baca token dari encrypted storage, cek session), delay natural dari pekerjaan itu
sudah cukup. Kalau ingin branding splash, 500-800ms sudah cukup, bukan 2000ms.

### C3. `Modifier.blur()` di TransferScreen

**File:** `ui/screen/transfer/TransferScreen.kt:200-204`
```kotlin
Box(
    modifier = Modifier
        .size(Spacing.s10)
        .offset(x = Spacing.s10, y = -Spacing.s10)
        .clip(AppShape.Full)
        .background(AppColor.Neutral100.copy(alpha = AppAlpha.A10))
        .blur(Spacing.s5)  // 20.dp blur!
        .align(Alignment.TopEnd),
)
```

**Dampak:**
- `Modifier.blur()` menggunakan `RenderEffect` (API 31+)
- Di device API < 31 (minSdk 26), fallback ke software rendering layer
- Software blur SANGAT mahal — bisa 10-30ms per frame untuk elemen dekoratif
- Ini elemen dekoratif murni yang memperlambat seluruh scroll

**Rekomendasi:** Hapus blur. Gunakan `background` dengan alpha rendah saja, atau
pre-render blur sebagai PNG/WebP kecil. Elemen dekoratif tidak boleh mengorbankan
performa scroll.

---

## MAJOR

### M1. Infinite Animation Berjalan Saat Tidak Terlihat

**File:** `ui/screen/faceid/FaceIdScreen.kt:185-194`
```kotlin
val infiniteTransition = rememberInfiniteTransition(label = "scan")
val scanLineProgress by infiniteTransition.animateFloat(...)
```
Animasi scan line SELALU berjalan, bahkan saat `status == SUCCESS` atau `FAILED`.
`rememberInfiniteTransition` tidak bisa di-pause — ia berjalan selama composable
di-compose.

**File:** `ui/screen/finger_print/FingerPrintScreen.kt:135-153`
```kotlin
val ringScale by infiniteTransition.animateFloat(...)  // animasi 1
val ringAlpha by infiniteTransition.animateFloat(...)   // animasi 2
```
Dua animasi infinite berjalan terus meskipun `isAnimating == false`. Hanya
rendering-nya yang dilewati, tapi kalkulasi frame tetap jalan dan mengkonsumsi CPU.

**Dampak:** Drain baterai dan CPU usage pada background thread Compose.

**Rekomendasi:** Gunakan `Animatable` yang di-launch/cancel berdasarkan state,
atau pindahkan `rememberInfiniteTransition` ke dalam blok kondisional:
```kotlin
if (status == FaceIdStatus.SCANNING) {
    val transition = rememberInfiniteTransition(...)
    // ...
}
```

### M2. Crossfade 500ms pada Session State Change

**File:** `ui/navigation/BcaNavHost.kt:27-43`
```kotlin
Crossfade(
    targetState = sessionState is SessionState.Loading,
    animationSpec = tween(durationMillis = 500),
    label = "splash",
) { isLoading -> ... }
```

**Dampak:** Setiap kali session state berubah (Loading→LoggedOut, Locked→LoggedOut),
ada animasi crossfade 500ms. Ditambah delay 2 detik di C2, total waktu dari buka
aplikasi sampai melihat Login screen = **2.5 detik minimum**.

Lebih parah: saat `isLoading` berubah, Crossfade meng-compose KEDUA target secara
bersamaan selama 500ms — artinya SplashScreen dan AppNavHost (termasuk seluruh
NavHost setup) hidup bersamaan, doubling memory dan CPU selama transisi.

**Rekomendasi:** Kurangi ke 200-300ms, atau ganti dengan `AnimatedContent` yang
lebih efisien untuk kasus ini.

### M3. UiState Baru Dibuat Setiap Rekomposisi

**File:** `ui/navigation/MainGraph.kt` — multiple locations
```kotlin
composable<Beranda> {
    BerandaScreen(state = BerandaUiState(), ...)  // line 80
}
composable<Mutasi> {
    MutasiScreen(state = MutasiUiState(), ...)    // line 104
}
composable<Transfer> {
    TransferScreen(state = TransferUiState(...), ...)  // line 152-159
}
```

Dan di `EWalletGraph.kt`:
```kotlin
composable<EWalletPilih> {
    TopUpEWalletScreen(state = TopUpEWalletUiState(), ...)  // line 30
}
composable<EWalletBukti> {
    BuktiTransaksiScreen(state = BuktiTransaksiUiState(...), ...)  // line 86-97
}
```

**Dampak:** Setiap rekomposisi membuat objek baru dengan `copy()` dari default.
Untuk `TransferUiState` yang berisi `List<RecentTransferItem>` dengan 4 item,
ini berarti alokasi baru di setiap rekomposisi. GC pressure meningkat.

**Rekomendasi:** State harus datang dari ViewModel via `collectAsState()`.
Untuk sementara, gunakan `remember { ... }`:
```kotlin
val state = remember { TransferUiState(...) }
```

### M4. PNG Logo Instead of Vector Drawable

**File:** `res/drawable/bca_logo_white.png`, `bca_logo_white_transparent.png`

**Dampak:**
- PNG harus di-decode oleh BitmapFactory — operasi CPU-intensive
- PNG tidak scalable — bisa blur di density tinggi atau waste memory di density rendah
- PNG di-load di multiple screen: Splash, Login, Beranda TopBar, Akun TopBar
- Setiap `painterResource(R.drawable.bca_logo_white)` bisa trigger decode ulang
  jika bitmap sudah di-evict dari cache

**Rekomendasi:** Konversi ke VectorDrawable (XML) jika logo cukup sederhana.
Atau gunakan WebP yang lebih ringan dari PNG. Pastikan ukuran file reasonable
(< 50KB untuk logo).

### M5. Font TTF Loading

**File:** `res/font/inter_bold.ttf`, `inter_light.ttf`, `inter_medium.ttf`,
`inter_regular.ttf`

**File:** `ui/theme/Type.kt:11-17`
```kotlin
val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_medium, FontWeight.SemiBold),  // ← duplikat file
    Font(R.font.inter_bold, FontWeight.Bold),
)
```

**Dampak:**
- 4 file TTF terpisah, masing-masing bisa 100-400KB
- Font loading terjadi synchronous saat pertama kali dipakai
- Semua 4 weight dipakai di first frame (typography diterapkan di seluruh app)
- `inter_medium` di-map ke Medium DAN SemiBold — bukan masalah performa,
  tapi SemiBold tidak akan terlihat beda dari Medium

**Rekomendasi:**
- Gunakan Inter Variable Font (1 file, semua weight) — jauh lebih kecil total
- Atau gunakan `GoogleFont` provider untuk async loading dengan fallback
- Fix mapping: SemiBold seharusnya file berbeda atau dihapus dari FontFamily

### M6. Bottom Bar Dibuat Ulang per Tab

**File:** `ui/navigation/MainGraph.kt:78, 102, 117, 130`

Setiap tab screen dibungkus `MainScaffold` sendiri-sendiri:
```kotlin
composable<Beranda> {
    MainScaffold(navController, onScanClick = {}) { innerPadding ->
        BerandaScreen(...)
    }
}
composable<Mutasi> {
    MainScaffold(navController, onScanClick = {}) { innerPadding ->
        MutasiScreen(...)
    }
}
// ...seterusnya untuk Riwayat dan Akun
```

**Dampak:** Setiap kali user pindah tab, `MainScaffold` yang lama di-dispose dan
yang baru di-compose. Ini termasuk:
- `NavigationBar` + 4 `NavigationBarItem` dibuat ulang
- `FloatingActionButton` dibuat ulang
- `currentBackStackEntryAsState()` di-observe ulang
- Transisi tidak smooth — ada "kedipan" bottom bar

**Rekomendasi:** Pindahkan Scaffold ke level atas, screen tab ditempatkan sebagai
konten yang berubah di dalamnya. Bottom bar tetap, content berganti.

---

## MODERATE

### MO1. UiState Mengandung `List<>` — Compose Instability

**Files:**
- `BerandaUiState.promoItems: List<PromoItem>` (HomeScreen.kt:76)
- `MutasiUiState.transactionGroups: List<TransactionGroup>` (MutasiScreen.kt:82)
- `TransferUiState.recentTransfers: List<RecentTransferItem>` (TransferScreen.kt:58)
- `TopUpEWalletUiState.walletOptions: List<EWalletOption>` (TopUpEWalletScreen.kt:81)
- `TopUpEWalletUiState.presetAmounts: List<PresetAmount>` (TopUpEWalletScreen.kt:84)

**Dampak:** `List` di Kotlin adalah interface — Compose compiler tidak bisa
menjamin stabilitas. Setiap recomposition dari parent akan me-recompose child
meskipun data tidak berubah, karena Compose tidak bisa membuktikan bahwa
`List` tidak berubah.

**Rekomendasi:** Gunakan `kotlinx.collections.immutable.ImmutableList` atau
tambahkan `@Immutable`/`@Stable` annotation pada UiState class.

### MO2. Promo Cards Pakai `Row` + `horizontalScroll`, Bukan `LazyRow`

**File:** `ui/screen/home/HomeScreen.kt:595-608`
```kotlin
Row(
    modifier = Modifier
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = Spacing.s4)
) {
    promoItems.forEach { promo ->
        PromoCard(promo = promo, onClick = { onPromoClick(promo) })
    }
}
```

**Dampak:** Semua promo card di-compose sekaligus meskipun tidak terlihat.
Dengan 3 promo ini masih OK, tapi pattern ini tidak scalable. Jika nanti promo
ada 10-20 item, akan ada lag yang terasa.

**Rekomendasi:** Ganti dengan `LazyRow` yang hanya meng-compose item yang terlihat.

### MO3. `verticalScroll` Column untuk Konten yang Bisa Panjang

**Files:**
- `HomeScreen.kt:117-148` — seluruh Beranda
- `TransferScreen.kt:137-174` — termasuk list recent transfers
- `AkunScreen.kt:169-312` — seluruh settings

**Dampak:** Semua composable dalam Column di-compose sekaligus saat pertama kali
ditampilkan, meskipun sebagian di luar layar. Untuk Beranda dengan grid 8 menu +
promo section, ini berarti ~30+ composable di-compose di first frame.

**Rekomendasi:** Pertimbangkan `LazyColumn` untuk screen dengan konten panjang,
terutama Transfer screen yang list-nya bisa tumbuh.

### MO4. Inline Lambda di Composable Graph

**File:** `ui/navigation/MainGraph.kt:84-89`
```kotlin
onQuickAction = { action ->
    when (action) {
        QuickAction.TRANSFER -> navController.navigate(Transfer)
        QuickAction.E_WALLET -> navController.navigate(EWalletPilih)
        else -> {}
    }
},
```

Setiap lambda ini dibuat baru di setiap recomposition. Ada 10+ lambda per screen
di MainGraph.

**Dampak:** Lambda allocation overhead pada setiap recomposition. Kecil per-satu,
tapi dengan 10+ lambda per composable dan multiple composable aktif, ini bertambah.

**Rekomendasi:** Gunakan `remember` untuk lambda yang tidak berubah, atau
pindahkan ke ViewModel sebagai event handler.

---

## LOW

### L1. `@SuppressLint("EmptySuperCall")` yang Misleading

**File:** `session/SessionViewModel.kt:29`
```kotlin
@SuppressLint("EmptySuperCall")
override fun onCleared() {
    ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
    super.onCleared()
}
```

`super.onCleared()` DIPANGGIL — suppress-nya tidak seharusnya ada.

### L2. Decorative Box dengan matchParentSize

**File:** `ui/screen/home/HomeScreen.kt:265-276`
```kotlin
Box(
    modifier = Modifier
        .matchParentSize()
        .background(brush = Brush.linearGradient(...)),
)
```

Extra compose node + draw pass untuk efek dekoratif. Bisa dilakukan dengan
`Modifier.drawBehind` tanpa compose node tambahan.

### L3. `EWalletOption` Mengandung `Color` Property

**File:** `ui/screen/ewallet/TopUpEWalletScreen.kt:64-68`
```kotlin
data class EWalletOption(
    val id: String,
    val name: String,
    val brandColor: Color,  // ← Color = Long wrapper
)
```

`Color` dalam data class yang di-pass sebagai state. `Color` sendiri stable,
tapi ini mixing domain data dengan UI concern.

---

## Estimasi Dampak Perbaikan

| Perbaikan | Effort | Dampak Performa |
|---|---|---|
| C1. Aktifkan R8 | Rendah | **Sangat Tinggi** — APK ~50% lebih kecil, startup lebih cepat |
| C2. Hapus delay 2s | Rendah | **Sangat Tinggi** — 2 detik lebih cepat perceived |
| C3. Hapus blur | Rendah | **Tinggi** — scroll jadi smooth di semua device |
| M1. Fix infinite animations | Rendah | Sedang — hemat CPU saat idle |
| M2. Kurangi Crossfade | Rendah | Sedang — transisi lebih cepat |
| M3. State dari ViewModel | Sedang | Sedang — kurangi GC pressure |
| M4. PNG → Vector/WebP | Rendah | Sedang — kurangi decode time |
| M5. Variable font | Rendah | Sedang — kurangi total font size |
| M6. Shared Scaffold | Sedang | Sedang — transisi tab lebih smooth |
| MO1. Immutable collections | Rendah | Rendah-Sedang — kurangi recomposition |
| MO2-3. LazyRow/LazyColumn | Sedang | Rendah (saat ini), Tinggi (saat data tumbuh) |

**Quick wins (bisa dikerjakan < 1 jam, dampak besar):**
C1 + C2 + C3 = startup 2+ detik lebih cepat, scroll smooth, APK ~50% lebih kecil.