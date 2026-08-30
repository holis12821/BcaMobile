# Navigasi — bca_mobile

Rancangan navigasi untuk aplikasi mobile banking dengan dua metode login (kode akses dan
biometrik), ditujukan untuk kemudahan perawatan jangka panjang.

Ini adalah **keputusan arsitektur yang mengikat**, bukan usulan. Penyimpangan darinya butuh
persetujuan manusia dan pembaruan file ini di commit yang sama.

---

## 1. Masalah pada struktur saat ini

Lima hal di `NavHost` yang ada sekarang akan menyulitkan dalam 3–6 bulan.

**1. Graph datar.** Sebelas route sejajar, screen auth bersebelahan dengan screen pasca-login.
Tidak ada batas yang bisa dipakai untuk "bersihkan semua yang di belakang setelah login" atau
"kunci aplikasi, kembali ke auth". Setiap kali butuh operasi itu, kamu akan menulis
`popUpTo` manual dengan route yang di-hardcode — dan lupa satu saja berarti tombol Back
mengembalikan pengguna ke halaman saldo setelah logout.

**2. `Scaffold` membungkus `NavHost`.** `innerPadding` diterapkan ke **semua** screen,
termasuk Splash, Login, dan Kode Akses — padahal ketiganya layar penuh berlatar primary tanpa
bottom nav. Ini memaksa logika kondisional "sembunyikan bottom bar kalau route X" yang
tersebar dan mudah tertinggal saat menambah screen.

**3. `SPLASH` sebagai route.** Splash bukan tujuan navigasi, melainkan kondisi *aplikasi belum
siap*. Menjadikannya route menciptakan entri back stack yang harus dibuang manual, dan
komentar `// TODO: auto-navigate to LOGIN` adalah gejalanya.

**4. `FACE_ID` dan `FINGER_PRINT` sebagai route.** Di Android, prompt biometrik adalah
**dialog sistem** (`BiometricPrompt`) yang tidak bisa kamu gambar sendiri dan tidak bisa
di-style. Layar Face ID / Touch ID di desain adalah ilustrasi keadaan, bukan layar yang bisa
kamu bangun. Kalau dijadikan route, kamu akan membuat layar yang tidak pernah cocok dengan
apa yang benar-benar muncul di perangkat.

**5. `KODE_AKSES` sebagai route layar penuh.** Desainmu menunjukkannya sebagai **modal di atas
layar Login yang diredupkan** — bukan layar terpisah. Menjadikannya route berarti transisi,
back stack, dan tampilan yang berbeda dari desain.

---

## 2. Prinsip: navigasi mengikuti state, bukan sebaliknya

Ini keputusan tunggal yang paling menentukan kemudahan perawatan.

**Jangan** panggil `navController.navigate(HOME)` dari dalam `LoginScreen`. Kalau begitu,
setiap metode login baru — kode akses, sidik jari, wajah, nanti mungkin PIN atau pola —
harus tahu ke mana harus pergi, dan tiap satu bisa salah.

**Sebagai gantinya:** layar login melapor "autentikasi berhasil" ke satu tempat, satu tempat
itu mengubah state sesi, dan navigasi bereaksi terhadap perubahan state.

```
LoginScreen ──onSuccess──> AuthViewModel ──> SessionRepository (StateFlow)
                                                      │
                                          BcaApp mengamati ──> pindah graph
```

Konsekuensinya: menambah metode login ketiga **tidak menyentuh kode navigasi sama sekali**.
Begitu juga auto-lock, logout, dan sesi kedaluwarsa — semuanya lewat jalur yang sama.

---

## 3. Struktur graph

Dua graph bersarang, bukan sebelas route datar.

```
NavHost
├── navigation(route = Graph.Auth)
│   ├── Auth.Login              ← layar sambutan + tombol login
│   │   └── dialog Auth.KodeAkses   ← modal di atas Login
│   ├── Auth.BukaRekening
│   └── Auth.GantiKodeAkses
│
└── navigation(route = Graph.Main)
    ├── Main.Beranda            ← tab
    ├── Main.Mutasi             ← tab
    ├── Main.Riwayat            ← tab
    ├── Main.Akun               ← tab
    ├── Main.Transfer           ← dibuka dari Beranda quick action
    ├── Main.RentangWaktu       ← dibuka dari Mutasi
    └── navigation(route = Graph.EWallet)   ← flow transaksi
        ├── EWallet.Pilih
        ├── EWallet.Nominal
        ├── EWallet.Pin
        └── EWallet.Bukti
```

**Kenapa bersarang.** Batas graph memberi kamu satu nama untuk seluruh wilayah. Logout jadi
satu operasi terhadap `Graph.Main`, bukan daftar route yang harus dijaga manual.

Perhatikan `Auth.BukaRekening` dan `Auth.GantiKodeAkses` — dua screen yang **hilang** dari
artefak Stitch tapi ada di desain. Keduanya masuk graph auth karena diakses sebelum login.

---

## 4. State sesi

```kotlin
sealed interface SessionState {
    /** Aplikasi belum selesai membaca penyimpanan. Tampilkan splash sistem. */
    data object Loading : SessionState

    /** Belum pernah login, atau sudah logout. */
    data object LoggedOut : SessionState

    /** Sesi ada tapi perlu autentikasi ulang (timeout / kembali dari background). */
    data object Locked : SessionState

    /** Terautentikasi penuh. */
    data class Authenticated(val displayName: String) : SessionState
}
```

`Locked` adalah yang membedakan aplikasi perbankan dari aplikasi biasa. Setelah idle beberapa
menit, pengguna kembali ke layar login **tanpa** kehilangan konteks — dan setelah re-auth bisa
dikembalikan ke tempat terakhirnya. Kalau kamu hanya punya `LoggedOut`, satu-satunya pilihan
adalah membuang semuanya.

---

## 5. Splash bukan route

Pakai `androidx.core:core-splashscreen`. Splash sistem tampil sejak proses dimulai — tidak ada
kedipan layar putih, tidak ada entri back stack, tidak ada `TODO: auto-navigate`.

```kotlin
// MainActivity.onCreate, sebelum setContent
installSplashScreen().setKeepOnScreenCondition {
    viewModel.sessionState.value is SessionState.Loading
}
```

Desain splash-mu — logo di tengah, latar primary penuh — persis bentuk yang didukung API ini
(`windowSplashScreenBackground` + `windowSplashScreenAnimatedIcon`).

> Ini menambah satu dependency. Per `CLAUDE.md`, minta persetujuan lebih dulu.
> Alternatif tanpa dependency: jadikan `Loading` sebagai composable di dalam `BcaApp`
> sebelum `NavHost` dipasang — tetap bukan route.

---

## 6. Biometrik bukan route

`BiometricPrompt` adalah dialog milik sistem. Kamu memanggilnya dari `Activity`/`Fragment`,
bukan menavigasi ke sana.

```kotlin
// Dipicu dari tombol Face ID / sidik jari di Auth.Login
BiometricPrompt(activity, executor, callback).authenticate(
    BiometricPrompt.PromptInfo.Builder()
        .setTitle(getString(R.string.login_biometrik_judul))
        .setSubtitle(getString(R.string.login_biometrik_subjudul))
        .setAllowedAuthenticators(BIOMETRIC_STRONG)
        .setNegativeButtonText(getString(R.string.batal))
        .build()
)
```

`BIOMETRIC_STRONG` itu wajib untuk perbankan — `BIOMETRIC_WEAK` menerima pengenalan wajah
berbasis kamera biasa yang bisa dibohongi foto.

Layar Face ID dan Touch ID di desain jadi **keadaan pada `Auth.Login`**, bukan route:
`AuthUiState(biometricStatus = Idle | Prompting | Failed(reason))`.

Sebelum menampilkan tombolnya, cek ketersediaan dengan `BiometricManager.canAuthenticate()` —
kalau perangkat tidak punya sensor atau pengguna belum mendaftarkan sidik jari, tombolnya
jangan ditampilkan sama sekali.

---

## 7. Kode Akses adalah dialog

Sesuai desain — modal di atas Login yang diredupkan:

```kotlin
dialog(Auth.KodeAkses) { KodeAksesDialog(...) }
```

Tetap jadi tujuan navigasi (jadi tombol Back menutupnya, dan state-nya selamat dari rotasi),
tapi dirender sebagai dialog di atas Login. Cocok dengan desain tanpa kompromi.

---

## 8. Perpindahan graph

Satu tempat, satu aturan. Ini inti keamanan back stack.

```kotlin
@Composable
fun BcaApp(sessionState: SessionState) {
    val navController = rememberNavController()

    val startGraph = remember {
        if (sessionState is SessionState.Authenticated) Graph.Main else Graph.Auth
    }

    LaunchedEffect(sessionState) {
        when (sessionState) {
            is SessionState.Authenticated ->
                navController.navigate(Graph.Main) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            SessionState.LoggedOut, SessionState.Locked ->
                navController.navigate(Graph.Auth) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            SessionState.Loading -> Unit
        }
    }

    NavHost(navController, startDestination = startGraph) { /* ... */ }
}
```

**`popUpTo(inclusive = true)` bukan opsional.** Tanpa itu, tombol Back dari Beranda kembali ke
layar Login yang masih hidup di back stack — dan setelah logout, Back bisa membawa pengguna
kembali ke layar saldo. Di aplikasi perbankan itu cacat keamanan, bukan sekadar bug UX.

**Jebakan yang harus diketahui:** `LaunchedEffect(sessionState)` ikut berjalan pada komposisi
pertama, sehingga bisa menavigasi ke graph yang sudah jadi `startDestination`. `launchSingleTop`
meredamnya, tapi cara paling bersih adalah membandingkan dengan graph yang sedang aktif
sebelum menavigasi. Uji ini secara khusus — gejalanya berupa kedipan atau back stack ganda
yang hanya muncul di perangkat lambat.

---

## 9. Bottom bar milik graph Main

Jangan taruh `Scaffold` membungkus seluruh `NavHost`. Bottom nav hanya milik empat tab.

Kalau tetap ingin satu `Scaffold` di level aplikasi, jangan tentukan tampilnya dari daftar
route yang di-hardcode. Turunkan dari sifat route itu sendiri:

```kotlin
val showBottomBar = navController.currentBackStackEntryAsState().value
    ?.destination?.hierarchy?.any { it.route == Graph.Main.route } == true
    && currentRoute in MainTab.routes
```

Menambah screen baru di graph Main tidak menuntut kamu ingat memperbarui daftar tersembunyi.

> Catatan desain: FAB scan di tengah bottom nav **ada** di desain tapi hilang dari artefak
> Stitch. Rancang `AppBottomNav` dengan FAB sejak awal — menambahkannya belakangan berarti
> mengubah tata letak semua tab.

---

## 10. Flow transaksi dan `popUpTo`

Flow e-wallet punya sifat khusus: setelah **Bukti Transaksi**, tombol Back **tidak boleh**
kembali ke layar PIN.

```kotlin
// setelah transaksi berhasil, saat berpindah ke layar bukti
navController.navigate(EWallet.Bukti) {
    popUpTo(Graph.EWallet) { inclusive = true }
}
```

Ini juga alasan `Graph.EWallet` dibuat bersarang: satu nama untuk seluruh flow, sehingga
membersihkannya jadi satu baris. Flow transaksi berikutnya (Transfer, Pulsa) mengikuti pola
yang sama — dan itulah keuntungan konsistensinya.

---

## 11. Yang khas perbankan

**Auto-lock.** Amati `ProcessLifecycleOwner`; saat aplikasi ke background, catat waktunya.
Saat kembali, kalau lewat ambang batas, ubah state ke `Locked`. Navigasi ikut sendiri — tidak
ada kode navigasi tambahan.

**Cegah tangkapan layar.** `window.setFlags(FLAG_SECURE, FLAG_SECURE)` minimal pada layar auth
dan layar transaksi. Ini juga menyembunyikan isi layar dari daftar aplikasi terkini.

**Kematian proses.** Setelah proses dibunuh dan aplikasi dibuka lagi, state awal harus
`Locked` atau `LoggedOut` — **jangan pernah** `Authenticated` langsung dari penyimpanan.
Token boleh dipulihkan; status terautentikasi tidak.

**Jangan simpan rahasia di `SavedStateHandle`.** Kode akses dan PIN disimpan di state proses
saja, tidak ikut `onSaveInstanceState`, dan dibersihkan begitu dipakai.

**Deep link.** Tautan ke layar transaksi yang datang saat belum login harus disimpan sebagai
tujuan tertunda, dipulihkan **setelah** auth berhasil. Tanpa ini, pengguna mendarat di Beranda
dan tidak tahu apa yang barusan mereka klik.

---

## 12. Struktur file

```
ui/navigation/
├── Graph.kt              // Graph.Auth, Graph.Main, Graph.EWallet
├── Route.kt              // definisi tujuan
├── BcaNavHost.kt         // NavHost + graph bersarang
├── AuthGraph.kt          // navigation(Graph.Auth) { ... }
├── MainGraph.kt          // navigation(Graph.Main) { ... }
└── EWalletGraph.kt       // navigation(Graph.EWallet) { ... }
```

Setiap graph di file sendiri. Menambah screen menyentuh satu file, bukan `NavHost` raksasa
yang tumbuh terus.

**Pertimbangkan route type-safe** (Navigation Compose 2.8+, route sebagai `@Serializable`
object/data class) alih-alih konstanta `String` seperti `AppRoute.SPLASH`. Argumen dicek
compiler, salah ketik route jadi error kompilasi, bukan crash saat runtime. Butuh plugin
`kotlinx-serialization` — keputusan dependency, jadi minta persetujuan dulu.

---

## 13. Rencana uji

Navigasi jarang diuji dan itulah kenapa sering rusak. Minimal ini:

| # | Skenario | Hasil yang benar |
|---|---|---|
| 1 | Login berhasil, tekan Back di Beranda | Aplikasi keluar — **bukan** kembali ke Login |
| 2 | Logout dari Akun, tekan Back | Tetap di Login — **bukan** kembali ke Beranda |
| 3 | Login gagal 3× | Tetap di Login, pesan error, back stack tidak tumbuh |
| 4 | Batal di dialog biometrik | Kembali ke Login, bukan layar kosong |
| 5 | Perangkat tanpa sensor biometrik | Tombol biometrik tidak muncul sama sekali |
| 6 | Selesai transaksi, tekan Back di Bukti | Ke Beranda — **bukan** ke layar PIN |
| 7 | Background > ambang batas, kembali | Layar Login, state `Locked` |
| 8 | Proses dibunuh saat login, buka lagi | Layar Login — **bukan** Beranda |
| 9 | Rotasi di dialog Kode Akses | Dialog tetap terbuka, input tidak hilang |
| 10 | Pindah tab bolak-balik | Back stack tidak tumbuh tak terbatas |
| 11 | Deep link ke transaksi saat belum login | Login dulu, lalu lanjut ke tujuan |
| 12 | Tangkapan layar di layar login | Ditolak sistem |

Nomor 1, 2, 6, dan 8 adalah sifat keamanan, bukan preferensi UX. Jadikan tes otomatis
(`androidx.navigation:navigation-testing`), jangan hanya dicek manual.

---

## 14. Yang perlu kamu putuskan

1. **`core-splashscreen`** — tambah dependency, atau splash sebagai state composable?
2. **Route type-safe** — tambah `kotlinx-serialization`, atau tetap konstanta `String`?
3. **Ambang auto-lock** — 1, 3, atau 5 menit di background?
4. **Setelah re-auth dari `Locked`** — kembali ke layar terakhir, atau selalu ke Beranda?
5. **Cakupan `FLAG_SECURE`** — hanya layar auth dan transaksi, atau seluruh aplikasi?

Nomor 3 dan 4 adalah keputusan produk, bukan teknis. Sisanya keputusan dependency yang menurut
`CLAUDE.md` butuh persetujuanmu.
