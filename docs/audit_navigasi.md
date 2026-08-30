# Audit Navigasi — bca_mobile

Dibandingkan terhadap arsitektur mengikat di
`.claude/skills/compose-architecture/references/navigation.md`
dan task flow di `.claude/skills/stitch-to-compose/references/screen-inventory.md`.

Tanggal audit: 2026-08-30

---

## Daftar Penyimpangan

### 1. Graph datar tanpa nesting

- **File & baris:** `AppNavigation.kt:68-106`
- **Aturan dilanggar:** navigation.md §3 — dua graph bersarang (`Graph.Auth`, `Graph.Main`,
  `Graph.EWallet`), bukan sebelas route datar.
- **Kondisi saat ini:** 11 route semua sejajar di satu `NavHost` tanpa `navigation()` bersarang.
- **Risiko:** Tidak ada batas graph yang bisa dipakai untuk "bersihkan semua di belakang setelah
  login" atau "kunci aplikasi, kembali ke auth". Setiap operasi itu memerlukan `popUpTo` manual
  dengan route hardcode — lupa satu saja berarti tombol Back mengembalikan pengguna ke layar
  saldo setelah logout.
- **Perbaikan:** Pecah route ke `navigation(route = Graph.Auth) { ... }`,
  `navigation(route = Graph.Main) { ... }`, dan `navigation(route = Graph.EWallet) { ... }`
  sesuai §3.

### 2. Scaffold membungkus seluruh NavHost

- **File & baris:** `AppNavigation.kt:51-107`
- **Aturan dilanggar:** navigation.md §9 — "Jangan taruh Scaffold membungkus seluruh NavHost."
- **Kondisi saat ini:** `Scaffold` dengan `bottomBar` membungkus seluruh `NavHost`. `innerPadding`
  diterapkan ke **semua** screen termasuk Splash, Login, Kode Akses, Face ID, dan Finger Print.
- **Risiko:** Layar penuh berlatar primary (Splash, Login) mendapat padding bottom bar yang tidak
  seharusnya. Menambah screen baru menuntut perawatan daftar `TAB_ROUTES` supaya bottom bar
  tersembunyi di screen non-tab — mudah tertinggal.
- **Perbaikan:** Pindahkan `Scaffold` + bottom nav ke dalam graph Main saja, atau turunkan
  visibility dari hierarki graph (`destination.hierarchy`) bukan dari daftar route hardcode
  (baris 53).

### 3. SPLASH sebagai route

- **File & baris:** `AppRoute.kt:7`, `AppNavigation.kt:70,73-75`
- **Aturan dilanggar:** navigation.md §5 — "Splash bukan route."
- **Kondisi saat ini:** `SPLASH` adalah `startDestination` dan `composable` route, dengan komentar
  `// TODO: auto-navigate to LOGIN`.
- **Risiko:** Entri back stack yang harus dibuang manual. Pengguna bisa menekan Back dan kembali ke
  layar splash kosong. Komentar TODO adalah gejala bahwa desain route ini tidak sustainable.
- **Perbaikan:** Gunakan `androidx.core:core-splashscreen` atau render splash sebagai state
  composable sebelum `NavHost` dipasang — bukan route. Keputusan dependency perlu persetujuan
  (navigation.md §14 #1).

### 4. FACE_ID dan FINGER_PRINT sebagai route

- **File & baris:** `AppRoute.kt:10-11`, `AppNavigation.kt:82-87`
- **Aturan dilanggar:** navigation.md §6 — "Biometrik bukan route."
- **Kondisi saat ini:** Keduanya adalah `composable` route.
- **Risiko:** Di Android, prompt biometrik adalah dialog sistem (`BiometricPrompt`) yang tidak bisa
  digambar sendiri. Membuat route untuk ini menghasilkan layar yang tidak pernah cocok dengan apa
  yang benar-benar muncul di perangkat. Juga menciptakan entri back stack yang tidak bermakna.
- **Perbaikan:** Hapus kedua route. Jadikan keduanya **keadaan pada `Auth.Login`** — panggil
  `BiometricPrompt` dari Activity, modelkan hasilnya sebagai
  `AuthUiState(biometricStatus = Idle | Prompting | Failed)`.

### 5. KODE_AKSES sebagai composable, bukan dialog

- **File & baris:** `AppRoute.kt:9`, `AppNavigation.kt:79-81`
- **Aturan dilanggar:** navigation.md §7 — "Kode Akses adalah dialog."
- **Kondisi saat ini:** `composable(AppRoute.KODE_AKSES)` — route layar penuh.
- **Risiko:** Desain menunjukkan Kode Akses sebagai **modal di atas layar Login yang diredupkan**.
  Menjadikannya route layar penuh berarti transisi, back stack, dan tampilan yang berbeda dari
  desain.
- **Perbaikan:** Ganti ke `dialog(Auth.KodeAkses) { KodeAksesDialog(...) }` di dalam graph Auth.

### 6. Tidak ada popUpTo saat perpindahan graph

- **File & baris:** `AppNavigation.kt` — tidak ada sama sekali
- **Aturan dilanggar:** navigation.md §8 — "`popUpTo(inclusive = true)` bukan opsional."
- **Kondisi saat ini:** Tidak ada `LaunchedEffect` yang mengamati session state dan menavigasi
  antar graph. Tidak ada `popUpTo(navController.graph.id) { inclusive = true }` di mana pun untuk
  transisi auth -> main atau main -> auth.
- **Risiko:** **Cacat keamanan.** Tombol Back dari Beranda bisa kembali ke Login yang masih hidup
  di back stack. Setelah logout, Back bisa membawa pengguna kembali ke layar saldo.
- **Perbaikan:** Implementasikan pola `BcaApp(sessionState)` dengan
  `LaunchedEffect(sessionState)` yang menavigasi antar graph dan selalu memakai
  `popUpTo(navController.graph.id) { inclusive = true }`.

### 7. Navigasi tidak mengikuti state

- **File & baris:** `AppNavigation.kt:46` — `BcaApp` tidak menerima `SessionState`
- **Aturan dilanggar:** navigation.md §2 — "Navigasi mengikuti state, bukan sebaliknya."
- **Kondisi saat ini:** Tidak ada `SessionState`, `SessionRepository`, atau `AuthViewModel`.
  Navigasi akan dipicu langsung dari screen (imperative navigate), bukan reaktif terhadap
  perubahan state.
- **Risiko:** Setiap metode login baru harus tahu ke mana harus pergi. Auto-lock, logout, dan
  sesi kedaluwarsa masing-masing butuh kode navigasi sendiri — inkonsisten dan rawan bug.
- **Perbaikan:** Buat `SessionState` sealed interface, `SessionRepository` dengan `StateFlow`,
  dan `BcaApp` mengamati state-nya untuk menentukan graph aktif.

### 8. Struktur file tidak sesuai

- **File & baris:** `AppRoute.kt`, `AppNavigation.kt` — hanya 2 file
- **Aturan dilanggar:** navigation.md §12 — struktur 6 file.
- **Kondisi saat ini:** Semua navigasi di dua file. Seharusnya: `Graph.kt`, `Route.kt`,
  `BcaNavHost.kt`, `AuthGraph.kt`, `MainGraph.kt`, `EWalletGraph.kt`.
- **Risiko:** Saat screen bertambah, `AppNavigation.kt` akan membengkak. Setiap penambahan screen
  menyentuh satu file besar — rentan merge conflict.
- **Perbaikan:** Pecah sesuai struktur di §12.

### 9. Bottom bar visibility dari daftar route hardcode

- **File & baris:** `AppNavigation.kt:53`, `AppRoute.kt:19`
- **Aturan dilanggar:** navigation.md §9 — turunkan dari sifat route/hierarki graph, bukan
  daftar hardcode.
- **Kondisi saat ini:** `if (currentRoute in AppRoute.TAB_ROUTES)` — daftar statis.
- **Risiko:** Menambah screen baru di graph Main menuntut pemeliharaan daftar `TAB_ROUTES` secara
  manual. Lupa memperbarui berarti bottom bar muncul atau hilang di tempat yang salah.
- **Perbaikan:** Gunakan `destination.hierarchy` untuk cek apakah route saat ini ada di
  `Graph.Main` dan termasuk tab.

---

## Perbandingan Route vs Task Flow (screen-inventory.md)

### Route yang tidak punya padanan di task flow

| Route | Status |
|---|---|
| `TRANSFER` | Tidak ada task flow untuk Transfer. Ada sebagai tab di kode, tapi navigation.md §3 tidak memasukkannya sebagai tab (arsitektur punya Beranda, Mutasi, Riwayat, Akun). |
| `AKUN` | Tidak ada task flow spesifik (wajar sebagai placeholder tab). |

### Flow yang tidak punya route

| Flow / Screen | Sumber | Status |
|---|---|---|
| **Rentang Waktu** | screen-inventory #8, task flow B | Tidak ada route. Arsitektur mendefinisikan `Main.RentangWaktu` — dibuka dari Mutasi. |
| **Input PIN transaksi** | screen-inventory #11, task flow C | Tidak ada route. Dibutuhkan di flow e-wallet antara Nominal dan Bukti. Arsitektur mendefinisikan `EWallet.Pin`. |
| **Bukti Transaksi** | screen-inventory #12, task flow C | Tidak ada route. Arsitektur mendefinisikan `EWallet.Bukti`. |
| **Riwayat** | screen-inventory #13 | Tidak ada route terpisah. Tab RIWAYAT di kode mengarah ke route `MUTATION`. Arsitektur mendefinisikan `Main.Riwayat` sebagai tab terpisah dari `Main.Mutasi`. |
| **Buka Rekening Baru** | screen-inventory #2 | Tidak ada route. Arsitektur mendefinisikan `Auth.BukaRekening`. |
| **Ganti Kode Akses** | screen-inventory #2 | Tidak ada route. Arsitektur mendefinisikan `Auth.GantiKodeAkses`. |
| **e-Wallet Nominal** | screen-inventory #10 | Flow e-wallet saat ini hanya punya `TOP_UP_EWALLET` dan `CONFIRM_EWALLET`. Arsitektur mendefinisikan 4 langkah: `EWallet.Pilih`, `EWallet.Nominal`, `EWallet.Pin`, `EWallet.Bukti`. |

### Tab mismatch

| Arsitektur (navigation.md §3) | Kode saat ini | Masalah |
|---|---|---|
| Beranda, Mutasi, Riwayat, Akun | Beranda, Transfer, Riwayat->Mutasi, Akun | **Transfer** ada di kode tapi tidak di arsitektur. **Mutasi** dan **Riwayat** digabung jadi satu route. |

---

## Ringkasan

| Kategori | Jumlah |
|---|---|
| Penyimpangan arsitektur | **9** |
| Route tanpa padanan task flow | 2 |
| Flow/screen tanpa route | 7 |
| Bersifat keamanan (kritis) | 3 (#3 splash back, #6 popUpTo, #7 state) |

---

*Tidak ada kode yang diubah. Tidak ada file skill yang diubah.*