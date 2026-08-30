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
| 8 | Rentang Waktu | Segmen `Hari Ini` / `7 Hari Terakhir`, dropdown Pilih Bulan, pemilih Pilih Tanggal, dropdown Jenis Transaksi, tombol `Terapkan` menempel di bawah | Tombol di slot `bottomBar`, bukan di akhir `Column` |
| 9 | e-Wallet — pilih | Dropdown `Pilih e-Wallet`, input `Nomor Telepon`, tombol `Lanjut` | Validasi nomor sebelum `Lanjut` aktif |
| 10 | e-Wallet — form top-up | Sumber Rekening (nomor + saldo), Nomor Tujuan (avatar, nama, provider + nomor), input Nominal, tombol `Beli` | Butuh state saldo tidak cukup |
| 11 | Input PIN transaksi | Input PIN tersamar | Tidak tergambar di visual design, tapi ada di task flow — konfirmasi tampilannya sebelum dibuat |
| 12 | Bukti Transaksi | Kartu struk: logo, `Transaksi Berhasil` + badge sukses, Tanggal, Sumber Rekening, Jenis Transaksi, Nomor Tujuan, Nama Tujuan, Nominal, Biaya Admin, pemisah, Total; tombol `Simpan` dengan ikon unduh | Watermark di belakang kartu; Total memakai penekanan lebih besar |
| 13 | Riwayat | Daftar transaksi: ikon, judul, bank + nomor, No. Ref, timestamp, badge status (sukses/gagal), bottom nav | Dua varian badge wajib ada |

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
