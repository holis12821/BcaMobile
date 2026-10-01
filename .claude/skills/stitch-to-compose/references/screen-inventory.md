# Screen Inventory & Task Flow

Diturunkan dari dokumen desain (studi kasus redesign BCA Mobile). Dipakai untuk menentukan
urutan pengerjaan, komponen yang dipakai bersama, dan state yang wajib ada.

> Catatan: logo dan aset merek pada desain adalah milik pihak ketiga. Project ini bersifat
> latihan/portofolio — jangan didistribusikan atau dipublikasikan sebagai aplikasi perbankan asli.

---

## 1. Komponen bersama

Bangun ini lebih dulu, sebelum screen mana pun. Semuanya stateless.

| Komponen | Dipakai di | Catatan |
|---|---|---|
| `AppTopBar` | Mutasi, Rentang Waktu, e-Wallet, Bukti Transaksi, Riwayat | Latar primary, judul putih di tengah, tombol back opsional di kiri |
| `AppBottomNav` | Beranda, Mutasi, Riwayat, Akun | 4 item + FAB scan di tengah; item aktif memakai warna primary |
| `PrimaryButton` | hampir semua screen | Lebar penuh, tinggi ≥ 48dp |
| `OutlinedActionButton` | Welcome, dialog Kode Akses | Varian outline di atas latar primary dan di atas latar putih |
| `AppTextField` | e-Wallet, Nominal, Nomor Telepon | Varian dengan leading icon |
| `AppDropdown` | Sumber Rekening, Pilih Bulan, Jenis Transaksi, Pilih e-Wallet | `ExposedDropdownMenuBox` |
| `MenuGridItem` | Beranda | Ikon dalam kotak + label di bawah |
| `TransactionRow` | Mutasi, Riwayat | Varian: dengan badge status, tanpa badge |
| `StatusBadge` | Riwayat, Bukti Transaksi | Sukses = `Success*`, gagal = `Danger*` |
| `SectionLabel` | form | Label kecil di atas field |
| `EmptyState` | Mutasi | Wajib — didefinisikan oleh task flow |

---

## 2. Daftar screen

Urutan yang disarankan: dari yang paling sedikit state ke yang paling banyak.

| # | Screen | Elemen utama | Catatan implementasi |
|---|---|---|---|
| 1 | Splash | Logo di tengah, latar primary penuh | Tanpa state |
| 2 | Welcome / Login | Logo, tombol `m-BCA Login` + ikon Face ID + ikon Touch ID, `Buka Rekening Baru`, `Ganti Kode Akses`, 3 aksi cepat (Info BCA, Flazz, Klik BCA) | Latar primary; tombol outline berwarna putih |
| 3 | Dialog Kode Akses | Judul, input 6 digit tersamar, `Batal` + `Login` | Modal di atas Welcome yang diredupkan; butuh state error untuk kode salah |
| 4 | Face ID | Ikon pemindai wajah + label | Latar putih; butuh state gagal |
| 5 | Touch ID | Judul, ikon sidik jari, label | Sama seperti #4 |
| 6 | Beranda | Header (logo + indikator status), sapaan + ikon notifikasi, Total Saldo dengan toggle mata, grid menu 4×2 (m-Info, Transfer, e-Wallet, Pulsa, Cardless, m-Admin, m-Commerce, Lainnya), bagian Promo, bottom nav | Saldo tersamar secara default; promo bisa lebih dari satu banner |
| 7 | Mutasi | Dropdown Sumber Rekening, chip `Rentang Waktu`, daftar mutasi (tanggal, deskripsi, nominal), bottom nav | Nominal negatif memakai `Danger*`; list wajib `LazyColumn`; **wajib punya empty state** |
| 8 | Rentang Waktu | Segmen `Hari Ini` / `7 Hari Terakhir`, dropdown Pilih Bulan, pemilih Pilih Tanggal, dropdown Jenis Transaksi, tombol `Terapkan` menempel di bawah | Tombol di slot `bottomBar`, bukan di akhir `Column` **Sudah diimplementasikan dan tersambung** (`ui/screen/rentang/`). Dropdown "Jenis Transaksi" **tidak dibuat**: `GET /transactions/mutations` tidak punya parameter jenis. Pilihan `Hari Ini` dikirim sebagai `period=CUSTOM` dengan tanggal mulai = akhir. |
| 9 | e-Wallet — pilih | Dropdown `Pilih e-Wallet`, input `Nomor Telepon`, tombol `Lanjut` | Validasi nomor sebelum `Lanjut` aktif |
| 10 | e-Wallet — form top-up | Sumber Rekening (nomor + saldo), Nomor Tujuan (avatar, nama, provider + nomor), input Nominal, tombol `Beli` | Butuh state saldo tidak cukup |
| 11 | Input PIN transaksi | Input PIN tersamar | Tidak tergambar di visual design, tapi ada di task flow — konfirmasi tampilannya sebelum dibuat |
| 12 | Bukti Transaksi | Kartu struk: logo, `Transaksi Berhasil` + badge sukses, Tanggal, Sumber Rekening, Jenis Transaksi, Nomor Tujuan, Nama Tujuan, Nominal, Biaya Admin, pemisah, Total; tombol `Simpan` dengan ikon unduh | Watermark di belakang kartu; Total memakai penekanan lebih besar |
| 13 | Riwayat | Daftar transaksi: ikon, judul, bank + nomor, No. Ref, timestamp, badge status (sukses/gagal), bottom nav | Dua varian badge wajib ada. **Sudah diimplementasikan dan tersambung** (`ui/screen/riwayat/`). Dua baris chip: jenis (`type`) dan rentang waktu (`period`) — keduanya dari `GET /transactions/history`. Rentang tanggal yang digambar artefak Stitch akhirnya bisa dibuat setelah endpoint riwayat memakai `resolvePeriod` yang sama dengan Mutasi; chip `Pilih Tanggal` membuka layar Rentang Waktu dan hasilnya kembali lewat `SavedStateHandle`. Tab/chip `Semua` **tidak** mengirim `type` maupun `period` — mengirim `type=ALL` disaring server apa adanya (`AND type = 'ALL'`) dan mengosongkan daftar. Tombol cari di artefak dihilangkan — belum ada endpoint pencarian. Baris yang ditekan membuka struk lewat `receipt`. |
| 14 | Notifikasi | App bar biru rata kiri + pil **Tandai Dibaca**, chip filter yang bisa digeser dengan badge angka, pengelompokan tanggal (HARI INI / KEMARIN / MINGGU INI), **kartu** per notifikasi: lingkaran ikon 44dp, judul, waktu, titik belum dibaca, baris nominal, isi 2 baris, kaki kartu (aksi pintas + chevron), garis aksen kiri 4dp untuk yang belum dibaca, footer akhir-daftar | **Sudah diimplementasikan dan tersambung** (`ui/screen/notifikasi/`). **Desainnya diganti total pada artefak Stitch terbaru** — dari daftar rata menjadi kartu. Implementasi mengikuti artefak apa adanya atas keputusan pemilik project, termasuk paletnya. **Palet artefak bukan palet design system**: 9 dari 9 abu-nya persis Tailwind `slate`-100…900 dan 3 dari 3 kuningnya persis Tailwind `amber`, sementara `tailwind.config` di file HTML yang sama justru mendeklarasikan palet M3 project ini lalu mengabaikannya. Nilainya dimasukkan sebagai token resmi `AppColor.NeutralCool*`, `AppColor.Warning*`, dan `IconTint` (lihat `design-tokens.md`), **bukan** hex inline — jadi `check-hardcoded-ui.sh` tetap bersih. Ukuran font di luar skala (19/17/15/13sp) masuk `AppTextStyle`; jarak setengah-langkah (6/10/14dp) masuk `SpacingHalfStep`; bayangan masuk kategori baru `Elevation`. `shadow-inner` pada lingkaran ikon dilewati — Compose hanya menggambar bayangan ke luar. **Tiga elemen desain tidak punya sumber data** dan dipasang sebagai field nullable yang hanya terisi di `@Preview`: baris nominal (`nominal`), tombol aksi pintas (`aksi`), dan keterangan sumber (`sumber`). `GET /notifications` hanya membalas `type`, `title`, `body`, `is_read`, dan `created_at` — angka rupiah dan tujuan aksi **tidak boleh** diturunkan dari judul atau isi pesan. Ini konsumen **kedua** dari field arah dana yang sudah lama ditunggu (`NotifikasiArah` tetap `TIDAK_DIKETAHUI`, ikonnya jatuh ke `ic_receipt` netral). Badge angka di chip memakai `tabCounts` yang **masih kosong**: server tidak mengirim jumlah per jenis, dan menurunkannya dari halaman yang kebetulan termuat akan salah begitu paginasi berjalan — badge disembunyikan saat nilainya nol. Pengelompokan tanggal **dihitung di client** dari `created_at` di zona waktu perangkat (alasan sama dengan label waktu relatif); "Minggu ini" berarti tujuh hari terakhir, bukan minggu kalender, supaya artinya tidak berubah setiap Senin. Tanggal yang tidak bisa dibaca jatuh ke kelompok paling lama supaya tidak pernah mengaku baru. Angka "n Belum Dibaca" per grup dihitung sekali per daftar, bukan di dalam tiap item. Tab menyaring **di server** lewat `?type=`; cursor direset setiap ganti tab; tab `Semua` tidak mengirim parameter itu (`ALL` dijawab `VALIDATION_ERROR`); jenis `SYSTEM` ikut tab Info Bank bersama Keamanan supaya tidak ada notifikasi yang kehilangan tab. `unread_count` **tidak dikirim** endpoint daftar, jadi hitungan untuk tombol `Tandai Dibaca` diturunkan dari yang sudah termuat; lencana Beranda tetap memakai `unread_notifications` dari `GET /account/dashboard`. App bar **tidak** memakai `AppTopBar` bersama: desain menaruh judul rata kiri di samping tombol kembali dan menggantikan ikon aksi dengan pil berlabel, dan memaksakan keduanya ke `AppTopBar` akan mengubah semua layar yang memakainya. Tombol kembali digambar 40dp seperti desain tapi area sentuhnya 48dp. Panah kembali memakai `ic_arrow_forward` yang diputar — project belum punya panah kiri. Dua drawable baru: `ic_chevron_right`, `ic_done_all`. `<nav>` bottom nav + FAB QRIS **muncul lagi di artefak baru dan tetap tidak dipasang** — layar ini push di atas app shell dan chrome-nya diurus satu gerbang. Pernah dicoba membungkus `composable<Notifikasi>` dengan `MainScaffold`, lalu **dibatalkan atas keputusan pemilik project**. Keputusan itu dikonfirmasi ulang saat redesign ini. Jangan diulang. Isi pesan digambar polos: artefak menebalkan nama dan nominal di tengah paragraf, tapi API mengirim `body` sebagai teks biasa tanpa penanda — menebak bagian mana yang tebal berarti menebak. Indikator load-more tiga titik berdenyut dipertahankan untuk paginasi (artefak tidak menggambarkannya); saat tidak ada halaman lagi, footer akhir-daftar desain yang tampil. Teks footer menyebut "30 hari terakhir" mengikuti artefak — **belum dikonfirmasi** apakah retensi server memang 30 hari. Empty state dan error tetap dipakai dari implementasi sebelumnya, warnanya diselaraskan ke ramp baru. Entri `strings.xml` lama (`notifikasi_tandai_semua`, `notifikasi_tab_promo`, `notifikasi_tab_info`) **tidak dihapus** meski kini tak terpakai — lint menandainya `UnusedResources`, dan itu memang konsekuensi aturan additive-only. Isi pesan **tidak dipotong**: artefak menampilkannya utuh (3–4 baris), berbeda dari daftar rata lama yang mengunci dua baris. Judul tetap satu baris dengan elipsis. **Label zona waktu artefak sengaja tidak disalin.** Artefak menulis `10:30 WIB`, tapi jam dihitung di zona waktu perangkat — menempelkan literal `WIB` akan mencetak label yang salah untuk nasabah di WITA atau WIT, dan itu kesalahan fakta pada waktu transaksi, bukan kosmetik. Kalau memang harus tampil, zonanya harus diturunkan dari `ZoneId`, bukan ditulis tetap. **Perlu direview pemilik desain**: paletnya menyimpang dari design system, dan tiga elemen di atas belum pernah terlihat dengan data sungguhan. |
| 15 | Layar blokir aplikasi | Logo, judul, pesan dari server, satu tombol aksi | Bukan dari artefak Stitch: dibutuhkan `maintenance_mode` dan `force_update` dari `GET /health`. Tanpa jalan melanjutkan — keputusan server. |

| 16 | Scan QRIS | Preview kamera, peredup, bingkai bidik dengan empat sudut kurung, teks instruksi, tombol Flash dan Galeri | **Sudah diimplementasikan dan tersambung** (`ui/screen/qris/`). Bingkai memakai `AppSize.ScannerFrame` (256dp) — artefak menyebut 288dp, tidak ada tokennya. Garis animasi pemindai di artefak diganti indikator saat decode berjalan. Flash = torch CameraX, Galeri = PickVisualMedia + ML Kit. |
| 17 | Konfirmasi QRIS | Kartu merchant, sumber rekening, saldo, nominal (terkunci untuk QR dinamis / input untuk QR statis), biaya admin, total, tombol Bayar | **Tidak ada di artefak Stitch.** Susunannya mengikuti `ConfirmEWalletScreen` supaya konfirmasi transaksi seragam — perlu direview pemilik desain. |

| 18 | Atur Limit Transaksi | Judul, kotak info, tiga kartu limit (ikon, judul, tombol Ubah, limit harian, terpakai, bilah pemakaian, sisa) | **Sudah diimplementasikan dan tersambung** (`ui/screen/limit/`). Kartu ketiga di artefak berjudul "Debit Online"; batas ketiga yang dilayani server adalah **e-Wallet** (`ewallet_daily`), jadi judulnya mengikuti endpoint. Verifikasi PIN (dialog) wajib — tidak tergambar di artefak tapi diminta endpoint. **Batas yang berlaku belum bisa ditampilkan**: kontrak tidak punya `GET` limit. |
| 19 | Profil Saya | Identitas (nama, nomor HP • rekening, email), manajemen kartu Paspor (gambar kartu, empat aksi, tiga baris pengaturan), KEAMANAN & AKUN, NOTIFIKASI & LAPORAN, BANTUAN & INFORMASI, Keluar Aplikasi | **Desainnya diterapkan pada layar Akun yang sudah ada** (`ui/screen/akun/AkunScreen.kt`) — tidak ada layar/route profil terpisah, karena tab Akun memang halaman profil. Nomor kartu, masa berlaku, status, dan gaya kartu kini dari `GET /account/cards`; `style` dipetakan ke grup token `CardArt` (server tidak mengirim hex maupun gambar). Badge tier memakai field `tier` di `GET /account/profile` — **absen untuk nasabah reguler**, dan absen berarti badge disembunyikan. Dua sakelar kanal memakai `PUT /account/cards/{id}/settings`; Blokir Kartu dan Ganti Kartu memakai `block`/`replacement` dengan `verification_token` (`purpose` `BLOCK_CARD`/`REPLACE_CARD`). **Pemilih alasan blokir dan alasan + metode kirim penggantian tidak ada di artefak** — dirakit dari komponen dan token yang ada (`AksiKartuDialog.kt`), preseden layar #17, **perlu direview pemilik desain**. Biaya penggantian tidak dicetak sebelum server menyebutkannya. `KONTROL_AKSES` tetap "belum tersedia": tidak ada endpoint tersendiri untuknya. Sakelar Notifikasi Transaksi memakai `PUT /account/settings`. |
| 20 | Rekening & Kartu | Daftar rekening: label, nomor, saldo efektif, toggle mata, tombol Mutasi + Detail | **Sudah diimplementasikan dan tersambung** (`ui/screen/rekening/`). Bagian "Kartu Debit" kini terisi dari `GET /account/cards` (nama produk, nomor tersamar, masa berlaku, status). Aksinya sengaja **tidak** diduplikasi di sini — semuanya sudah ada di Profil Saya, dan dua tempat yang bisa memblokir kartu adalah dua tempat yang bisa berselisih. Tombol Detail membuka saldo tersedia + dana ditahan dari `GET /account/balance`. |
| 21 | Ubah Kode Akses | Stepper, input kode lama, input kode baru + konfirmasi, daftar kriteria keamanan, layar berhasil | **Tiga dari lima langkah** di artefak Stitch (`ui/screen/ubah_kode_akses/`). Verifikasi kartu ATM dan OTP **tidak dibuat**: tidak ada endpoint-nya, dan layar keamanan yang tidak memverifikasi apa pun lebih berbahaya daripada tidak ada. Hanya untuk nasabah yang sudah masuk — `POST /auth/pin/change` memakai access token, jadi tautan di layar Login tetap placeholder. |
| 22 | Pusat Bantuan | Daftar kategori FAQ, item pertanyaan yang bisa dibuka, tautan ke Hubungi CS | **Tidak ada di artefak Stitch** — `docs/design/missing-screens-audit.md` §E5 hanya mendaftar elemen. Dibuat dari komponen dan token yang ada (`ui/screen/bantuan/`), tersambung `GET /content/help-center` (publik, tanpa Authorization). **Kotak pencarian yang disebut audit tidak dibuat**: tidak ada endpoint pencarian, dan menyaring di client atas isi yang kebetulan termuat akan terlihat seperti pencarian yang gagal. `title` kategori dicetak dari server supaya `key` baru tetap punya teks. **Perlu direview pemilik desain.** |
| 23 | Hubungi CS | Kepala biru (ikon agen, "Halo BCA", satu baris penegasan), kartu Call Center + tombol telepon, grid 2×2 kanal (WhatsApp, live chat, email, nomor luar negeri), kartu jam operasional | **Sudah diimplementasikan dari artefak Stitch "Hubungi CS"** (`ui/screen/bantuan/HubungiCsScreen.kt`); layout lama yang dirakit sendiri dari audit §E6 diganti. Tersambung `GET /content/contact-cs` (publik). Kanal yang tidak dikirim server **disembunyikan** dan grid menggeser isinya, bukan meninggalkan kartu kosong. Semua kanal diserahkan ke aplikasi sistem lewat `Intent`; tidak ada nomor yang dirakit client. **Tiga penyimpangan artefak tidak diikuti:** (1) kartu **Twitter / X @HaloBCA** tidak dibuat — akun sosial media tidak ada di kontrak, jadi menampilkannya berarti menulis handle di client; slotnya diisi **nomor dari luar negeri** yang ada di kontrak tapi hilang dari artefak. (2) Nomor, email, dan jam layanan yang ditulis mati di artefak diambil dari state. (3) Jarak kosong 80dp di kaki artefak adalah siasat web untuk bottom bar — `Scaffold` yang mengurusnya. `backdrop-blur-sm` dilewati (tidak ada padanan Compose); gradien kepala layar dibuat dengan `Brush.verticalGradient` memakai `AppAlpha.A10`. Lima token ukuran baru lahir dari layar ini: `AppSize.Icon40`, `IconCircle`, `IconBox`, `ButtonCompact`, `ContactAvatar`. |

### Flow buka rekening

Tabel di atas memuat screen inti aplikasi; layar flow buka rekening belum
seluruhnya diinventarisasi. Yang sudah dicatat:

| Screen | Elemen utama | Catatan implementasi |
|---|---|---|
| Buka Rekening — Verifikasi OTP | Step indicator, kartu info (ikon SMS, judul, penjelasan, chip nomor tersamar), enam kotak digit, hitung mundur kirim ulang, kartu peringatan kerahasiaan, tombol `Verifikasi & Lanjut`, catatan enkripsi | Urutan ke-8: **sesudah Data Pribadi, sebelum Verifikasi Biometrik**. Layar penuh, bukan dialog — alasannya di `compose-architecture/references/navigation.md` §15. Input sesungguhnya satu `BasicTextField` tanpa tampilan dengan `decorationBox` = `OtpDigitBoxes`, supaya keyboard angka, tempel, dan autofill SMS tetap bekerja. Tiga error terpisah dari kontrak: `OTP_INVALID`, `OTP_EXPIRED`, `OTP_BLOCKED` (hitung mundur dari `details.retry_after_seconds`). **Sudah diimplementasikan dan tersambung** sebagai `BukaRekeningVerifikasiOtpScreen.kt`, route `Auth.BukaRekeningOtp`. Dua keadaan blokir dibedakan di state: `isInputDiblokir` (`OTP_BLOCKED`, input mati) dan `isKirimUlangDiblokir` (kuota kirim ulang habis, input tetap aktif). |
| Buka Rekening — Pilih Jenis Kartu Paspor BCA | Step indicator, judul + deskripsi, tiga kartu pilihan (Blue / Gold / Platinum) berisi badge, indikator pilihan, render miniatur kartu (gradient, chip EMV, emblem Mastercard, contactless), `Biaya Administrasi`, grid 2×2 limit (Tarik Tunai, Transfer BCA, Antar Bank, Debit/Belanja); catatan pengiriman kartu fisik; tombol `Lanjut ke Syarat & Ketentuan` + footer OJK/LPS di `bottomBar` | Warna kartu memakai grup token `CardArt` (bukan lima ramp) — lihat catatan di `Color.kt`; emblem Mastercard adalah aset merek di `drawable/`, bukan token. Artefak Stitch menambahkan blur dekoratif, header kaca, dan avatar yang **tidak** diimplementasikan. |

---

## 3. Task flow

Flow ini adalah spesifikasi perilaku. Setiap cabang keputusan wajib punya penanganan di UI state.

### A. Login

```
Mulai → Masuk aplikasi
  ├─ m-BCA Login  → Input kode akses → Valid? ─no→ kembali ke input
  ├─ Touch ID     → Scan sidik jari  → Valid? ─no→ kembali ke scan
  └─ Face ID      → Scan wajah       → Valid? ─no→ kembali ke scan
                                        └─yes→ Halaman utama
```

Konsekuensi UI: ketiga jalur butuh state `gagal` yang mengembalikan pengguna ke langkah
sebelumnya, bukan keluar dari flow.

### B. Melihat mutasi rekening

```
Klik menu Mutasi → Tampilkan daftar mutasi (terbaru)
  → Klik Rentang Waktu → Atur rentang waktu
      → Tersedia? ─yes→ Tampilkan daftar mutasi (terfilter) → Selesai
                  └─no → Daftar mutasi kosong → kembali ke Rentang Waktu
```

Konsekuensi UI: empty state **bukan opsional** dan harus menyediakan jalan kembali ke
pengaturan rentang waktu.

### C. Top-up e-wallet

```
Menu e-Wallet → Pilih e-wallet & input no. telp → Masukkan nominal top-up
  → Saldo tercukupi? ─no → kembali ke nominal
                     └─yes→ Input PIN transaksi → Valid? ─no→ kembali ke PIN
                              └─yes→ Transaksi selesai → Tampilkan bukti transaksi
                                       → Simpan bukti? ─yes→ Bukti tersimpan → Beranda
                                                       └─no → Beranda
```

Konsekuensi UI: tiga titik kegagalan (saldo kurang, PIN salah, transaksi gagal) masing-masing
butuh pesan sendiri. Jangan digabung jadi satu error generik.

---

## 4. Design opportunity dari riset

Empat hal ini adalah alasan desain ini ada. Implementasi yang mengabaikannya kehilangan
maksud desainnya:

1. **Login cepat** — biometrik dan PIN setara di layar depan, bukan tersembunyi di pengaturan.
2. **Navigasi lebih jelas** — menu e-wallet dapat dijangkau dari beranda tanpa penelusuran.
3. **Validasi nomor e-wallet otomatis** — nama tujuan tampil sebelum konfirmasi.
4. **Simpan bukti transaksi** — aksi simpan tersedia langsung di layar bukti.

---

## 5. Yang belum didefinisikan desain

Semuanya kondisi STOP — tanyakan sebelum mengimplementasikan.

- Type scale (ukuran font, line height) per peran teks.
- Skema warna gelap.
- Tampilan layar Input PIN transaksi.
- Bentuk loading state di seluruh screen.
- Teks untuk empty state dan pesan error.
- Perilaku pull-to-refresh dan pagination pada Mutasi dan Riwayat.
