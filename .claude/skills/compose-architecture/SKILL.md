---
name: compose-architecture
description: Aturan arsitektur Jetpack Compose untuk bca_mobile — navigasi, state, batas ViewModel, dan keamanan sesi. Gunakan saat menyentuh NavHost, NavGraph, route, back stack, popUpTo, deep link, splash, login/logout, biometrik (BiometricPrompt), auto-lock atau session timeout, ViewModel, UiState, state hoisting, bottom navigation, FAB scan, atau flow transaksi multi-langkah (e-Wallet, Transfer). Trigger juga pada "kenapa Back kembali ke Login", "navigasi setelah login", "bikin ViewModel", "state screen ini di mana", "tambah screen baru ke navigasi", "flow transaksi", "logout membersihkan apa", nama graph (Graph.Auth, Graph.Main, Graph.EWallet), dan review kode navigasi. JANGAN dipakai untuk penerjemahan visual desain ke Compose — itu tugas skill `stitch-to-compose`.
---

# Arsitektur Compose — bca_mobile

Skill ini mengatur **struktur**: navigasi, state, dan batas antar lapisan.
Penampilan visual (warna, spacing, layout dari desain) diatur skill `stitch-to-compose`.

## Batas dengan `stitch-to-compose`

| Pertanyaan | Skill |
|---|---|
| Warna, spacing, radius, tipografi, susunan elemen | `stitch-to-compose` |
| Elemen apa yang ada di sebuah screen | `stitch-to-compose` (`references/screen-inventory.md`) |
| Screen ini route atau dialog? Masuk graph mana? | **skill ini** |
| State screen tinggal di mana, siapa yang navigate | **skill ini** |
| Back stack, logout, auto-lock, deep link | **skill ini** |

Task flow di `screen-inventory.md` adalah **spesifikasi perilaku** — apa yang terjadi setelah
pengguna menekan sesuatu. Terjemahan flow itu menjadi graph dan back stack diatur di sini.
Kalau keduanya tampak bertentangan, **STOP dan lapor**; jangan pilih salah satu diam-diam.

## Aturan mutlak

1. **Screen composable tidak pernah memanggil `navController`.** Screen menerima lambda
   (`onContinue: () -> Unit`). Yang tahu tujuan adalah pemanggilnya di file graph.
2. **Navigasi mengikuti state sesi, bukan dipanggil dari layar login.** `LoginScreen` melapor
   berhasil ke `SessionRepository`; `BcaApp` yang memindahkan graph. Metode login baru tidak
   boleh menyentuh kode navigasi. Lihat `references/navigation.md` §2 dan §8.
3. **Perpindahan antar graph selalu `popUpTo(...) { inclusive = true }`.** Tanpa itu, Back
   dari Beranda kembali ke Login, dan setelah logout bisa kembali ke layar saldo. Ini sifat
   keamanan, bukan preferensi.
4. **Composable UI stateless.** Tanpa `hiltViewModel()`/`viewModel()` di dalam screen
   composable. ViewModel di-resolve di file graph, screen menerima `state` + lambda.
5. **Satu `UiState` per screen**, sebagai `data class` immutable, mencakup loading, empty,
   error, dan konten. Bukan lima `mutableStateOf` terpisah.
6. **Prompt biometrik bukan route.** `BiometricPrompt` adalah dialog sistem. Layar Face ID /
   Touch ID di desain adalah keadaan pada `Auth.Login`.
7. **Rahasia tidak masuk `SavedStateHandle`.** Kode akses, PIN, dan token tidak ikut
   `onSaveInstanceState`.
8. **Struktur di `references/navigation.md` mengikat.** Menambah route, graph, atau mengubah
   perilaku back stack berarti memperbarui file itu **di commit yang sama**.

## Menambah screen baru

Urutan ini, jangan dibalik:

1. Tentukan **graph**-nya: `Auth` (sebelum login), `Main` (tab dan turunannya), atau flow
   transaksi bersarang sendiri.
2. Tentukan **bentuknya**: `composable()` layar penuh, atau `dialog()` untuk modal di atas
   layar lain. Rujuk desainnya — modal di desain tidak boleh jadi route layar penuh.
3. Tentukan **cara keluarnya**: Back biasa, atau `popUpTo` yang membersihkan flow.
4. Tulis di file graph yang sesuai (`AuthGraph.kt`, `MainGraph.kt`, `EWalletGraph.kt`) —
   bukan di `BcaNavHost.kt`.
5. Perbarui `references/navigation.md` §3.
6. Tambahkan skenario Back-nya ke rencana uji §13.

## ViewModel dan state

- ViewModel mengekspos satu `StateFlow<XxxUiState>`, tidak lebih.
- Event dari UI masuk sebagai fungsi (`fun onSubmit(kode: String)`), bukan `Flow` dua arah.
- Kejadian sekali jalan (navigasi, snackbar) **tidak** disimpan di `UiState` sebagai boolean
  yang harus di-reset. Pakai `Channel`/`SharedFlow`, atau lambda yang dipanggil dari graph.
- ViewModel tidak mengenal `NavController`, `Context`, atau tipe Android UI apa pun.
- Data yang dipakai lintas screen dalam satu flow transaksi disimpan di ViewModel yang
  di-scope ke graph flow-nya, bukan dioper sebagai argumen berantai.

## Yang khas perbankan

Empat hal ini wajib dan sering terlewat. Detail di `references/navigation.md` §11.

- **Auto-lock** setelah idle di background → state `Locked`.
- **`FLAG_SECURE`** minimal di layar auth dan transaksi.
- **Kematian proses** memulihkan state ke `Locked`/`LoggedOut`, tidak pernah `Authenticated`.
- **Deep link** saat belum login disimpan sebagai tujuan tertunda, dilanjutkan setelah auth.

## Definition of Done

Untuk pekerjaan yang menyentuh navigasi atau state:

- [ ] Screen composable tidak menerima maupun memanggil `navController`
- [ ] Tidak ada `hiltViewModel()`/`viewModel()` di dalam screen composable
- [ ] Perpindahan graph memakai `popUpTo(...) { inclusive = true }`
- [ ] `UiState` mencakup loading, empty, error, konten
- [ ] `references/navigation.md` diperbarui kalau ada route/graph baru
- [ ] Skenario Back yang relevan ditambahkan ke rencana uji §13 dan lulus
- [ ] Uji Back setelah login, Back setelah logout, dan buka ulang setelah proses dibunuh

## Referensi

- `references/navigation.md` — struktur graph, state sesi, pola `popUpTo`, keamanan,
  struktur file, dan rencana uji 12 skenario.
