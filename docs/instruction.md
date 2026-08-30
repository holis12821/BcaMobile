
## Instruction Font
Di <id.bca.bcamobile>/ui/theme/, siapkan file token sesuai design-tokens.md §6.

Color.kt dan Type.kt saat ini masih berisi template bawaan Android Studio
(Purple80/Purple40, Typography dengan FontFamily.Default).
GANTI seluruh isinya dengan definisi dari design-tokens.md §6 —
warna template tidak perlu dipertahankan.

Buat Dimens.kt dan Shape.kt yang belum ada.

Jangan sentuh Theme.kt dan MainActivity.kt.
Di Type.kt, gunakan parameter bernama pada TextStyle (fontFamily = Inter, ...)
— parameter posisional pertama adalah color, bukan fontFamily.

## Type scale — blocker utama
Type scale di design-tokens.md §5 masih PROPOSED.
Bantu saya tetapkan ukuran font dan line height final per peran teks,
lalu update design-tokens.md §5 dan Type.kt dalam commit yang sama.

## Pemetaan colorScheme
Usulan di design-tokens.md §7 juga PROPOSED. Tinjau tabelnya, setujui atau ubah.

## Theme.kt
Pemetaan colorScheme di design-tokens.md §7 sudah saya setujui.
Ubah Theme.kt yang sudah ada:
- LightColorScheme diisi dari pemetaan itu
- dynamicColor DIMATIKAN (default false) — lihat design-tokens.md §7
- skema gelap: kunci ke LightColorScheme, jangan pakai warna template ungu
- pertahankan nama composable BcaMobileTheme
- typography diarahkan ke AppTypography
  Jangan ubah MainActivity.kt.

## Perbandingan screen login
Bandingkan screen Login m-BCA dari Stitch dengan layar Welcome/Login di
references/screen-inventory.md dan screenshot docs/design/.

Untuk setiap elemen di artefak Stitch, tandai: ADA di desain, atau TAMBAHAN.
Khususnya: divider "or login with", ikon arrow_forward, dan ikon quick link.

Jangan usulkan token. Laporkan perbandingannya saja.

## Perbandingan screen lain
Ulangi perbandingan yang sama untuk dua screen lain di project Stitch —
pilih yang paling mendekati Mutasi dan Beranda di screen-inventory.md.

Laporkan jumlah elemen: ADA / BERBEDA / TAMBAHAN / HILANG per screen.
Jangan usulkan token, jangan tulis kode.

## Audit navigasi yang ada
Baca arsitektur navigasi yang berlaku, lalu audit kode navigasi di project ini
terhadap arsitektur itu.

Laporkan sebagai daftar penyimpangan. Untuk tiap penyimpangan sebutkan:
- file dan baris
- aturan mana yang dilanggar
- risikonya kalau dibiarkan
- perbaikan yang diusulkan

Periksa khusus: Scaffold yang membungkus NavHost, SPLASH sebagai route,
FACE_ID dan FINGER_PRINT sebagai route, KODE_AKSES sebagai composable
bukan dialog, graph datar tanpa nesting, dan popUpTo yang hilang.

Bandingkan juga dengan task flow di screen-inventory.md — kalau ada route
yang tidak punya padanan di flow, atau flow yang tidak punya route, laporkan.

Jangan ubah kode. Jangan ubah file skill. Laporan saja.

## Restrukturisasi graph
- Sesi 1 -> struktur graph dan route
```
Restrukturisasi navigasi sesuai navigation.md §3 dan §12.
Perbaiki penyimpangan audit #1, #3, #4, #5, #8.

Keputusan saya: splash pakai core-splashscreen, route type-safe
(kotlinx-serialization disetujui).

- Pecah ke 6 file: Graph.kt, Route.kt, BcaNavHost.kt,
  AuthGraph.kt, MainGraph.kt, EWalletGraph.kt
- Graph bersarang: Auth, Main, EWallet
- Hapus route SPLASH, FACE_ID, FINGER_PRINT
- KODE_AKSES jadi dialog()
- Tambah route yang hilang: Auth.BukaRekening, Auth.GantiKodeAkses,
  Main.RentangWaktu, Main.Riwayat, EWallet.Pilih/Nominal/Pin/Bukti
- Hapus tab TRANSFER; Riwayat dan Mutasi jadi dua route terpisah

Screen composable boleh placeholder. Jangan sentuh Scaffold dulu.
Build harus hijau.
```

- Sesi 2 — state sesi dan back stack (ini perbaikan keamanannya)
```
Perbaiki audit #6 dan #7 sesuai navigation.md §2, §4, §8.

- SessionState sealed interface: Loading, LoggedOut, Locked, Authenticated
- SessionRepository dengan StateFlow
- BcaApp menerima SessionState, LaunchedEffect memindahkan graph
- Setiap perpindahan graph pakai popUpTo(navController.graph.id) { inclusive = true }
- Auto-lock 3 menit via ProcessLifecycleOwner
- Setelah re-auth dari Locked: pulihkan layar terakhir, KECUALI flow
  transaksi yang dibuang ke Beranda
- FLAG_SECURE di Activity, seluruh aplikasi

Perhatikan jebakan di §8: LaunchedEffect ikut jalan di komposisi pertama.
Bandingkan dengan graph aktif sebelum menavigasi.
```

- Sesi 3 — Scaffold dan bottom bar
```
Perbaiki audit #2 dan #9 sesuai navigation.md §9.

- Scaffold tidak lagi membungkus seluruh NavHost
- Visibility bottom bar dari destination.hierarchy, bukan daftar TAB_ROUTES
- AppBottomNav: 4 tab (Beranda, Mutasi, Riwayat, Akun) + FAB scan di tengah
- Hapus konstanta TAB_ROUTES
```
