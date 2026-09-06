# Audit Halaman yang Belum Ada — bca_mobile

Dokumen ini berisi daftar lengkap halaman yang direferensikan di kode (navigasi, callback,
menu) tetapi belum memiliki desain atau implementasi screen. Gunakan sebagai checklist saat
membuat desain di Stitch.

Tanggal audit: 2 September 2026
Sumber: `Route.kt`, `AuthGraph.kt`, `MainGraph.kt`, `EWalletGraph.kt`, semua file di
`ui/screen/`, `screen-inventory.md`, `navigation.md`.

---

## Legenda Status

| Status | Arti |
|---|---|
| PLACEHOLDER | Route ada, tapi isinya hanya `Box` + `Text` — belum ada desain/UI |
| MISSING | Tidak ada route maupun screen — hanya callback `{}` kosong |
| INCOMPLETE | Screen ada tapi ada state/step yang belum diimplementasi |

---

## A. Halaman Placeholder (Route Ada, UI Belum)

Halaman ini sudah didaftarkan di `Route.kt` dan graph navigasi, tapi isinya hanya
placeholder `Box` dengan `Text` di tengah layar.

### A1. Riwayat

- **Route:** `Riwayat` di `MainGraph.kt:116-127`
- **Lokasi tab:** Bottom nav tab ke-3
- **Deskripsi desain:** Daftar riwayat transaksi dengan ikon, judul, bank + nomor,
  No. Referensi, timestamp, dan badge status (sukses/gagal). Menggunakan `AppBottomNav`.
- **Referensi:** `screen-inventory.md` item #13
- **Elemen yang dibutuhkan:**
  - List transaksi dengan icon per jenis
  - Badge status: Sukses (`Success*` token) dan Gagal (`Danger*` token)
  - No. Referensi per transaksi
  - Timestamp
  - Empty state
  - Loading state
  - Error state dengan retry

### A2. Rentang Waktu

- **Route:** `RentangWaktu` di `MainGraph.kt:195-199`
- **Dibuka dari:** Mutasi > chip "Pilih Tanggal"
- **Deskripsi desain:** Filter mutasi dengan segmen Hari Ini / 7 Hari Terakhir,
  dropdown Pilih Bulan, date picker Pilih Tanggal, dropdown Jenis Transaksi,
  tombol `Terapkan` menempel di bawah.
- **Referensi:** `screen-inventory.md` item #8
- **Elemen yang dibutuhkan:**
  - Segmented control untuk periode cepat
  - Dropdown bulan
  - Date range picker (dari-sampai)
  - Dropdown jenis transaksi
  - Tombol `Terapkan` sticky di `bottomBar`
  - `AppTopBar` dengan judul dan back button

### A3. Buka Rekening

- **Route:** `BukaRekening` di `AuthGraph.kt:93-97`
- **Dibuka dari:** Login > tombol "Buka Rekening Baru"
- **Deskripsi desain:** Belum ada di screen inventory. Perlu desain dari nol.
- **Elemen yang dibutuhkan (saran):**
  - Info jenis rekening yang tersedia
  - CTA untuk memulai proses pembukaan
  - Link ke syarat & ketentuan
  - Back button ke Login

### A4. Ganti Kode Akses

- **Route:** `GantiKodeAkses` di `AuthGraph.kt:99-103`
- **Dibuka dari:** Login > tombol "Ganti Kode Akses"
- **Deskripsi desain:** Belum ada di screen inventory. Perlu desain dari nol.
- **Elemen yang dibutuhkan (saran):**
  - Input kode akses lama (6 digit tersamar)
  - Input kode akses baru (6 digit tersamar)
  - Input konfirmasi kode akses baru
  - Tombol `Simpan`
  - Validasi error state
  - Back button ke Login

---

## B. Halaman dari Menu Beranda (QuickAction)

Delapan menu grid di Beranda. Hanya **Transfer** dan **e-Wallet** yang sudah ada.
Sisanya mengarah ke `else -> {}` kosong di `MainGraph.kt:88`.

### B1. m-Info

- **Menu:** `QuickAction.M_INFO`
- **Status:** MISSING
- **Deskripsi:** Informasi saldo dan rekening, cek saldo tanpa harus ke Mutasi.
- **Elemen yang dibutuhkan (saran):**
  - Daftar rekening milik user
  - Saldo per rekening
  - Informasi nomor rekening, jenis, status
  - Refresh/reload

### B2. Pulsa

- **Menu:** `QuickAction.PULSA`
- **Status:** MISSING
- **Deskripsi:** Pembelian pulsa dan paket data.
- **Elemen yang dibutuhkan (saran):**
  - Input nomor handphone
  - Pilih provider (auto-detect dari prefix)
  - Pilih nominal / paket data
  - Sumber rekening
  - Tombol `Beli`
  - Flow konfirmasi + PIN + bukti transaksi

### B3. Cardless

- **Menu:** `QuickAction.CARDLESS`
- **Status:** MISSING
- **Deskripsi:** Tarik tunai tanpa kartu di ATM BCA.
- **Elemen yang dibutuhkan (saran):**
  - Pilih nominal penarikan (preset)
  - Sumber rekening
  - Generate kode tarik tunai (dengan timer)
  - Instruksi cara pakai di ATM
  - Flow PIN transaksi

### B4. m-Admin

- **Menu:** `QuickAction.M_ADMIN`
- **Status:** MISSING
- **Deskripsi:** Administrasi rekening (ganti PIN, atur limit, dll).
- **Elemen yang dibutuhkan (saran):**
  - Menu administrasi: Ganti PIN, Atur Limit, Aktivasi Kartu, dsb.
  - Masing-masing sub-menu punya flow sendiri

### B5. m-Commerce

- **Menu:** `QuickAction.M_COMMERCE`
- **Status:** MISSING
- **Deskripsi:** Pembayaran merchant dan e-commerce.
- **Elemen yang dibutuhkan (saran):**
  - Daftar kategori merchant / pembayaran
  - Input kode bayar / scan
  - Detail pembayaran
  - Flow konfirmasi + PIN + bukti

### B6. Lainnya

- **Menu:** `QuickAction.LAINNYA`
- **Status:** MISSING
- **Deskripsi:** Menu lengkap semua fitur yang tidak masuk 7 shortcut utama.
- **Elemen yang dibutuhkan (saran):**
  - Grid/list semua fitur aplikasi
  - Pencarian fitur
  - Kategori: Pembayaran, Pembelian, Investasi, dll.

---

## C. Halaman dari Sub-Menu Transfer

Empat jenis transfer di `TransferScreen.kt`. Hanya **Antar Rekening** yang sudah ada.
Sisanya mengarah ke `else -> {}` di `MainGraph.kt:168`.

### C1. Transfer Antar Bank

- **Menu:** `TransferType.ANTAR_BANK`
- **Status:** MISSING
- **Deskripsi:** Transfer ke rekening bank lain.
- **Elemen yang dibutuhkan:**
  - Sumber rekening (dropdown)
  - Pilih bank tujuan (daftar bank dengan search)
  - Input nomor rekening tujuan
  - Nama penerima (auto-fetch)
  - Input nominal
  - Catatan (opsional)
  - Step indicator (Input > Konfirmasi)
  - Flow PIN + bukti transaksi

### C2. Transfer Virtual Account

- **Menu:** `TransferType.VIRTUAL_ACCOUNT`
- **Status:** MISSING
- **Deskripsi:** Pembayaran ke nomor Virtual Account.
- **Elemen yang dibutuhkan:**
  - Sumber rekening (dropdown)
  - Input nomor VA (nomor panjang)
  - Auto-detect: nama merchant, nominal (jika closed amount)
  - Detail tagihan
  - Flow konfirmasi + PIN + bukti

### C3. Transfer Sakuku

- **Menu:** `TransferType.SAKUKU`
- **Status:** MISSING
- **Deskripsi:** Top-up atau transfer ke Sakuku (uang elektronik BCA).
- **Elemen yang dibutuhkan:**
  - Sumber rekening
  - Input nomor Sakuku / handphone
  - Input nominal
  - Flow konfirmasi + PIN + bukti

---

## D. Halaman Transfer Antar Rekening — State Belum Lengkap

### D1. Transfer Konfirmasi (Step 2)

- **Status:** INCOMPLETE
- **Lokasi:** `TransferAntarRekeningScreen.kt` — Step indicator ada (1=Input, 2=Konfirmasi)
  tapi hanya Step 1 yang diimplementasi.
- **Elemen yang dibutuhkan:**
  - Ringkasan detail transfer (dari, ke, nominal, catatan)
  - Nama penerima (hasil validasi)
  - Biaya admin
  - Total
  - Tombol `Konfirmasi` yang mengarah ke input PIN
  - Tombol `Ubah` untuk kembali ke Step 1

### D2. Transfer PIN Input

- **Status:** MISSING
- **Deskripsi:** Input PIN transaksi setelah konfirmasi transfer.
- **Catatan:** Bisa reuse layout `KodeAksesScreen` seperti di e-Wallet flow.

### D3. Transfer Bukti Transaksi

- **Status:** MISSING (flow-nya belum ada, tapi `BuktiTransaksiScreen` sudah ada)
- **Deskripsi:** Setelah transfer berhasil, tampilkan bukti. Bisa reuse
  `BuktiTransaksiScreen` dengan data transfer.

---

## E. Halaman dari Menu Akun

Menu di `AkunScreen.kt` via `AkunMenuItem`. Semua mengarah ke callback `{}` kosong.

### E1. Ubah PIN

- **Menu:** `AkunMenuItem.UBAH_PIN`
- **Status:** MISSING
- **Deskripsi:** Ganti PIN transaksi.
- **Elemen yang dibutuhkan:**
  - Input PIN lama (6 digit tersamar)
  - Input PIN baru
  - Input konfirmasi PIN baru
  - Validasi (tidak boleh berurut, tidak boleh sama semua)
  - Tombol `Simpan`
  - State sukses / gagal

### E2. Atur Limit Transaksi

- **Menu:** `AkunMenuItem.ATUR_LIMIT`
- **Status:** MISSING
- **Deskripsi:** Atur limit harian per jenis transaksi.
- **Elemen yang dibutuhkan:**
  - Daftar jenis transaksi (Transfer, e-Wallet, Pulsa, dll.)
  - Limit saat ini vs limit maksimum
  - Slider atau input nominal untuk ubah limit
  - Tombol `Simpan`
  - Konfirmasi PIN

### E3. Notifikasi Push

- **Menu:** `AkunMenuItem.NOTIFIKASI_PUSH`
- **Status:** MISSING
- **Deskripsi:** Pengaturan notifikasi push (transaksi, promo, info).
- **Elemen yang dibutuhkan:**
  - Toggle per kategori notifikasi
  - Deskripsi per kategori

### E4. Email e-Statement

- **Menu:** `AkunMenuItem.EMAIL_STATEMENT`
- **Status:** MISSING
- **Deskripsi:** Pengaturan email untuk e-statement rekening.
- **Elemen yang dibutuhkan:**
  - Input/edit alamat email
  - Pilih rekening yang dikirimi e-statement
  - Toggle aktif/non-aktif
  - Tombol `Simpan`

### E5. Pusat Bantuan

- **Menu:** `AkunMenuItem.PUSAT_BANTUAN`
- **Status:** MISSING
- **Deskripsi:** FAQ dan panduan penggunaan.
- **Elemen yang dibutuhkan:**
  - Daftar kategori FAQ
  - Search bar
  - Expandable FAQ items
  - Link ke Hubungi CS

### E6. Hubungi CS

- **Menu:** `AkunMenuItem.HUBUNGI_CS`
- **Status:** MISSING
- **Deskripsi:** Kontak customer service BCA.
- **Elemen yang dibutuhkan:**
  - Nomor telepon Halo BCA (1500888)
  - Email CS
  - Live chat (jika ada)
  - Jam operasional

### E7. Tentang Aplikasi

- **Menu:** `AkunMenuItem.TENTANG_APLIKASI`
- **Status:** MISSING
- **Deskripsi:** Info versi, lisensi, legal.
- **Elemen yang dibutuhkan:**
  - Logo BCA
  - Versi aplikasi (sudah ada di state: `appVersion`)
  - Syarat & ketentuan (link)
  - Kebijakan privasi (link)
  - Lisensi open source

### E8. Profil Lengkap

- **Callback:** `onLihatProfilClick` di `AkunScreen.kt`
- **Status:** MISSING
- **Deskripsi:** Detail profil user.
- **Elemen yang dibutuhkan:**
  - Foto profil (atau avatar default)
  - Nama lengkap
  - Nomor handphone
  - Email terdaftar
  - Daftar rekening
  - Tombol edit (jika diizinkan)

---

## F. Halaman dari Login Quick Links

Tiga quick link di bagian bawah `LoginScreen.kt`. Semua mengarah ke callback `{}` kosong.

### F1. Info BCA

- **Callback:** `onInfoBcaClick`
- **Status:** MISSING
- **Deskripsi:** Informasi umum BCA (lokasi ATM, kurs, suku bunga).
- **Elemen yang dibutuhkan (saran):**
  - Lokasi ATM/kantor cabang terdekat
  - Kurs mata uang
  - Suku bunga
  - Promo terbaru

### F2. Flazz

- **Callback:** `onFlazzClick`
- **Status:** MISSING
- **Deskripsi:** Cek saldo dan top-up Flazz via NFC.
- **Elemen yang dibutuhkan (saran):**
  - Instruksi tempelkan kartu
  - Info saldo Flazz
  - Top-up nominal
  - Riwayat top-up

### F3. KlikBCA

- **Callback:** `onKlikBcaClick`
- **Status:** MISSING
- **Deskripsi:** Akses ke layanan KlikBCA (internet banking).
- **Elemen yang dibutuhkan (saran):**
  - WebView atau deep link ke KlikBCA
  - Atau info/panduan akses KlikBCA

---

## G. Halaman Umum / Cross-Screen

Fitur yang direferensikan di banyak tempat tapi belum ada.

### G1. Notifikasi

- **Direferensikan di:** Beranda, Mutasi, Akun (ikon notif di top bar)
- **Status:** MISSING
- **Deskripsi:** Daftar notifikasi (transaksi, promo, info sistem).
- **Elemen yang dibutuhkan:**
  - List notifikasi per tanggal
  - Ikon per jenis (transaksi, promo, info)
  - Status baca/belum baca
  - Swipe to dismiss / mark as read
  - Empty state

### G2. QRIS / Scan QR

- **Direferensikan di:** FAB di bottom nav (`onScanClick` di `MainGraph.kt:78`)
- **Status:** MISSING
- **Deskripsi:** Scanner QR code untuk pembayaran QRIS.
- **Elemen yang dibutuhkan:**
  - Kamera dengan viewfinder QR
  - Toggle flash
  - Galeri (upload QR dari foto)
  - Setelah scan: detail pembayaran merchant
  - Input nominal (jika dynamic QR)
  - Flow konfirmasi + PIN + bukti

### G3. Promo Detail

- **Direferensikan di:** Beranda > `onPromoClick`
- **Status:** MISSING
- **Deskripsi:** Detail promo individual.
- **Elemen yang dibutuhkan:**
  - Gambar promo (hero image)
  - Judul promo
  - Deskripsi lengkap
  - Periode berlaku
  - Syarat & ketentuan
  - CTA (jika ada)

### G4. Semua Promo

- **Direferensikan di:** Beranda > `onLihatSemuaPromo`
- **Status:** MISSING
- **Deskripsi:** Daftar semua promo.
- **Elemen yang dibutuhkan:**
  - Grid/list promo cards
  - Filter kategori
  - Search
  - Pagination

### G5. Isi Saldo / Top Up Rekening

- **Direferensikan di:** Beranda > `onIsiSaldo`
- **Status:** MISSING
- **Deskripsi:** Top-up saldo rekening (dari sumber lain atau fitur setor tunai).
- **Elemen yang dibutuhkan:** Perlu didefinisikan — ini mungkin mengarah ke Transfer
  atau fitur terpisah.

### G6. Daftar Semua Transfer Terakhir

- **Direferensikan di:** Transfer > `onLihatSemua`
- **Status:** MISSING
- **Deskripsi:** Daftar lengkap kontak/rekening yang pernah ditransfer.
- **Elemen yang dibutuhkan:**
  - List kontak transfer (avatar, nama, bank + nomor)
  - Search
  - Filter per bank
  - Tap untuk langsung transfer ulang

### G7. Pilih Rekening (Bottom Sheet / Dialog)

- **Direferensikan di:** Transfer, e-Wallet, Mutasi (`onAccountClick`,
  `onSourceAccountClick`)
- **Status:** MISSING
- **Deskripsi:** Picker untuk memilih rekening sumber.
- **Elemen yang dibutuhkan:**
  - Daftar rekening user (jenis, nomor, saldo)
  - Rekening terpilih ditandai
  - Bottom sheet atau dialog

### G8. Pilih Kontak (Bottom Sheet / Dialog)

- **Direferensikan di:** Transfer Antar Rekening (`onContactsClick`), e-Wallet
  (`onContactPickerClick`)
- **Status:** MISSING
- **Deskripsi:** Picker kontak dari buku telepon atau daftar tersimpan.
- **Elemen yang dibutuhkan:**
  - Daftar kontak tersimpan
  - Search
  - Akses buku telepon perangkat
  - Bottom sheet atau full screen

---

## Ringkasan Jumlah

| Kategori | Jumlah |
|---|---|
| A. Placeholder (route ada, UI belum) | 4 |
| B. Menu Beranda yang belum ada | 6 |
| C. Sub-menu Transfer yang belum ada | 3 |
| D. Transfer flow belum lengkap | 3 |
| E. Menu Akun yang belum ada | 8 |
| F. Login quick links yang belum ada | 3 |
| G. Cross-screen / shared yang belum ada | 8 |
| **TOTAL** | **35** |

---

## Prioritas Pengerjaan Desain (Saran)

### Prioritas 1 — Melengkapi flow yang sudah ada

Halaman ini dibutuhkan untuk membuat flow yang sudah ada bisa end-to-end.

1. **Riwayat** (A1) — tab bottom nav, harus ada
2. **Rentang Waktu** (A2) — dibutuhkan Mutasi
3. **Transfer Konfirmasi Step 2** (D1) — flow transfer tidak lengkap
4. **Notifikasi** (G1) — ikon sudah ada di banyak screen
5. **QRIS / Scan QR** (G2) — FAB sudah ada di bottom nav
6. **Pilih Rekening** (G7) — dibutuhkan di Transfer dan e-Wallet

### Prioritas 2 — Menu utama Beranda

7. **m-Info** (B1)
8. **Pulsa** (B2)
9. **Cardless** (B3)

### Prioritas 3 — Menu sekunder dan pengaturan

10. **Profil Lengkap** (E8)
11. **Ubah PIN** (E1)
12. **Atur Limit** (E2)
13. Sisanya dari Akun (E3-E7)
14. Transfer Antar Bank, VA, Sakuku (C1-C3)
15. m-Admin, m-Commerce, Lainnya (B4-B6)
16. Login quick links (F1-F3)
17. Promo (G3, G4)