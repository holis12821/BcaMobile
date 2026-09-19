# Dokumentasi Alur Pengambilan e-KTP & Buka Rekening BCA Mobile

Pertanyaan dan observasi sebelumnya sangat tepat. Pada layar sebelumnya (`SCREEN_32`), foto e-KTP dan data OCR tiba-tiba sudah berstatus **"Selesai"** tanpa memperlihatkan bagaimana pengguna menyiapkan fisik kartu, mengarahkan kamera, dan memvalidasi hasil pembacaan OCR terlebih dahulu.

Dalam standar perbankan digital Indonesia (**POJK e-KYC**), pengambilan identitas memiliki rantai alur yang logis dan tidak boleh melompat.

---

## 🔍 Mengapa Layar Sebelumnya Terasa Kurang?

Sebelum formulir data diri terisi otomatis, aplikasi harus melalui **3 fase teknis**:

### 1. Instruksi Persiapan Dokumen

Memberitahu pengguna agar:

- Menyiapkan e-KTP fisik asli.
- Memastikan pencahayaan cukup.
- Memastikan seluruh bagian kartu terlihat.
- Tidak menutupi atau memotong sudut kartu.

### 2. Layar Kamera / Viewfinder Real-Time

Menampilkan kamera untuk mengambil foto e-KTP dengan:

- Bingkai khusus berformat KTP.
- Deteksi tepi dokumen secara otomatis (*auto-boundary detection*).
- Sensor atau indikator anti-silau (*anti-glare*).
- Panduan posisi kartu secara real-time.

### 3. Pratinjau Hasil & Verifikasi OCR

Setelah foto berhasil diambil, aplikasi menampilkan:

- Foto e-KTP hasil tangkapan.
- Hasil ekstraksi OCR.
- NIK 16 digit.
- Nama.
- Tanggal lahir.
- Alamat.
- Validasi apakah data berhasil terbaca dengan benar.

Pengguna kemudian dapat memilih untuk menggunakan foto tersebut atau mengambil ulang.

---

# 📱 3 Layar Baru yang Telah Ditambahkan

## SCREEN_183 — Panduan & Persiapan Pengambilan Foto e-KTP

**Nama layar:**  
`Buka Rekening - Panduan Foto e-KTP`

### Komponen

- Ilustrasi posisi e-KTP.
- Empat kriteria wajib:
      1. KTP fisik asli.
      2. NIK harus terbaca.
      3. Bebas dari silau.
      4. Keempat sudut kartu harus masuk ke dalam bingkai.
- Komparasi visual:
      - Contoh foto **Benar**.
      - Contoh foto **Salah**.
- CTA:
      - **"Mulai Ambil Foto KTP"**

---

## SCREEN_188 — Kamera Pemindaian e-KTP

**Nama layar:**  
`Buka Rekening - Kamera Foto e-KTP`

### Komponen

- Antarmuka kamera interaktif.
- Viewfinder dengan rasio ID-card.
- Panduan area pasfoto.
- Panduan area NIK.
- Deteksi dokumen secara real-time.
- Status deteksi, contoh:

> "Tahan posisi, sistem mendeteksi dokumen..."

### Kontrol Kamera

- Tombol shutter utama.
- Toggle flash otomatis.
- Opsi pengambilan foto otomatis ketika dokumen sudah fokus dan terdeteksi dengan baik.

---

## SCREEN_189 — Hasil Foto & Validasi OCR

**Nama layar:**  
`Buka Rekening - Hasil Foto & OCR e-KTP`

### Komponen

- Pratinjau foto e-KTP.
- Status ketajaman/resolusi foto.
- Hasil ekstraksi OCR.
- Validasi NIK 16 digit.
- Nama.
- Tanggal lahir.
- Alamat lengkap.

### Aksi

**Gunakan Foto Ini & Lanjut**

Melanjutkan proses ke formulir data pribadi.

**Ambil Foto Ulang**

Mengembalikan pengguna ke kamera untuk mengambil foto baru.

---

# 🗺️ Urutan Lengkap Workflow Buka Rekening BCA Mobile

Berikut urutan workflow dari awal hingga rekening berhasil dibuat:

```text
[1] SCREEN_33
    Pilih Jenis Rekening
    ├── Tahapan BCA
    ├── Tahapan Xpresi
    └── TabunganKu
          │
          ▼
[2] SCREEN_18
    Syarat & Ketentuan Pembukaan Rekening Digital
    Persetujuan S&K
          │
          ▼
[3] SCREEN_183
    Panduan & Persiapan Pengambilan Foto e-KTP
          │
          ▼
[4] SCREEN_188
    Kamera Pemindaian e-KTP
    Real-Time Viewfinder
          │
          ▼
[5] SCREEN_189
    Hasil Tangkapan Foto & Konfirmasi OCR
          │
          ▼
[6] SCREEN_15
    Form Data Pribadi & Alamat
    Auto-fill dari OCR
    + Input pekerjaan & gaji
          │
          ▼
[7] SCREEN_32
    Verifikasi Biometrik Wajah
    Liveness Detection
    └── Kedipkan Mata
          │
          ▼
[8] SCREEN_30
    Video Call e-KYC
    Bersama Petugas Halo BCA
          │
          ▼
[9] SCREEN_16
    Buat Kredensial
    ├── Kode Akses m-BCA 6 karakter
    └── PIN Finansial 6 digit
          │
          ▼
[10] SCREEN_13
     Ringkasan & Konfirmasi
     Seluruh Data Sebelum Dikirim
          │
          ▼
[11] SCREEN_28
     Rekening Berhasil Dibuat
     ├── Nomor Rekening Aktif
     └── Panduan Setoran Awal


# Penyesuaian & Kelengkapan Alur Point 7–11

Sebelumnya, tahapan verifikasi biometrik dan video call masih tergabung atau belum memperlihatkan fase antrean dan interaksi tatap muka secara mandiri.

Kini alur tersebut telah dipisah dan didesain secara spesifik, runtut, serta disesuaikan dengan kebutuhan proses verifikasi perbankan digital.

---

## Point 7 — Verifikasi Biometrik Wajah (Liveness Detection)

### Tampilan
- Viewfinder berbentuk oval dengan desain modern.
- Efek pemindaian aktif secara real-time.
- Real-time facial boundary untuk mendeteksi posisi dan keberadaan wajah.

### Indikator & Instruksi
- Indikator akurasi deteksi:
  - `Wajah Terdeteksi`
  - `98% Presisi`
- Instruksi dinamis, contoh:
  - `Kedipkan Kedua Mata Anda Secara Perlahan`

### Checklist Verifikasi
- Tanpa kacamata.
- Tanpa masker.
- Pencahayaan wajah merata.
- Wajah berada di dalam area viewfinder.
- Posisi wajah menghadap kamera.

### Compliance
- Referensi standar liveness detection:
  - ISO/IEC 30107-3
- Proses disesuaikan dengan kebutuhan verifikasi identitas perbankan digital dan ketentuan OJK yang relevan.

---

## Point 8A — Antrean Video Call Customer Service BCA

### Tampilan
Menampilkan tiket antrean virtual yang memberikan informasi posisi nasabah secara real-time.

### Informasi Antrean
- Nomor antrean:
  - `A-042`
- Posisi antrean:
  - `2 antrean di depan Anda`
- Estimasi waktu tunggu:
  - `~3 menit`

### Checklist Persiapan Nasabah
Nasabah diminta memastikan:

- KTP fisik asli tersedia.
- Koneksi internet stabil.
- Berada di ruangan yang tenang.
- Kamera dan mikrofon perangkat dapat digunakan dengan baik.
- Pencahayaan wajah cukup.

### Jam Layanan
- `06.00 – 22.00 WIB`

### Pilihan Tindakan
Nasabah dapat memilih salah satu opsi:

1. `Tunggu Panggilan Sekarang`
2. `Jadwalkan Panggilan Nanti`

---

## Point 8B — Video Call e-KYC bersama Petugas Halo BCA

### Tampilan
Menampilkan proses tatap muka langsung antara nasabah dengan petugas Customer Service BCA.

### Informasi Petugas
- Nama petugas:
  - `Sarah Adisti`
- Status:
  - `Terhubung`
- Status keamanan:
  - `Koneksi terenkripsi 256-bit`
- Durasi panggilan:
  - Ditampilkan secara real-time.

### Fitur Video Call

#### Picture-in-Picture (PiP)
Menampilkan preview kamera nasabah yang memperlihatkan:

- Wajah nasabah.
- Nasabah sedang memegang KTP fisik asli.
- Posisi KTP dapat diperiksa oleh petugas.

#### Instruksi Petugas
Petugas dapat memberikan instruksi secara langsung melalui verbal instruction bubble, misalnya:

- Meminta nasabah menunjukkan sisi depan KTP.
- Meminta nasabah memiringkan KTP untuk memastikan keaslian fisik.
- Meminta nasabah menghadap kamera.
- Melakukan konfirmasi data pribadi.
- Melakukan konfirmasi tujuan pembukaan rekening.

### Kontrol Panggilan
- `Mute Mic`
- `Balik Kamera`
- `Akhiri Panggilan`

### Compliance Reference
- Mengacu pada kebutuhan proses verifikasi nasabah secara tatap muka melalui video call.
- Referensi regulasi:
  - `POJK No. 12/POJK.01/2017`

---

## Point 9 — Buat Kredensial Keamanan Akun m-BCA

### Kode Akses m-BCA

Nasabah membuat Kode Akses untuk login ke aplikasi m-BCA.

### Aturan Kode Akses
- Panjang: `6 karakter`
- Kombinasi:
  - Alfanumerik.
- Tidak diperbolehkan menggunakan:
  - Kombinasi karakter berurutan.
  - Kombinasi karakter kembar/repetitif.
  - Pola yang mudah ditebak.

### Validasi Real-Time
Validasi ditampilkan secara langsung ketika nasabah memasukkan Kode Akses.

Contoh:

- `✓ 6 karakter`
- `✓ Mengandung huruf`
- `✓ Mengandung angka`
- `✕ Tidak boleh menggunakan kombinasi berurutan`
- `✕ Tidak boleh menggunakan karakter yang sama berulang`

---

### PIN Transaksi Finansial

Nasabah membuat PIN untuk transaksi finansial.

### Aturan PIN
- Panjang: `6 digit`
- Hanya menggunakan angka.
- Input ditampilkan dalam bentuk dot/karakter tersamar.

### Konfirmasi PIN
Nasabah memasukkan PIN sebanyak dua kali untuk memastikan kesesuaian.

Status validasi:

- `PIN Cocok`
- `PIN Tidak Cocok`

---

## Point 10 — Ringkasan & Konfirmasi Data

### Rekapitulasi Data

Menampilkan seluruh informasi yang telah dikumpulkan dan diverifikasi selama proses pembukaan rekening.

### Informasi Rekening
- Produk rekening yang dipilih.
- Jenis rekening.
- Informasi minimum setoran awal.

### Data Identitas
- Nama lengkap.
- NIK.
- Tanggal lahir.
- Alamat.
- Data identitas lain yang telah diverifikasi.

### Status e-KYC

Menampilkan status keberhasilan seluruh tahapan verifikasi:

| Tahapan | Status |
|---|---|
| OCR KTP | `✓ Terverifikasi` |
| Biometrik Wajah | `✓ Terverifikasi` |
| Video Call | `✓ Terverifikasi` |

### Persetujuan
Menampilkan kembali pernyataan persetujuan nasabah terhadap:

- Syarat dan ketentuan.
- Kebijakan privasi.
- Pernyataan pembukaan rekening.
- Penggunaan data untuk kebutuhan verifikasi dan layanan perbankan.

### Finalisasi

Nasabah melakukan konfirmasi terakhir sebelum rekening dibuat.

**CTA:**

`Buka Rekening`

---

## Point 11 — Rekening Berhasil Dibuat

### Status Pembukaan Rekening

Menampilkan konfirmasi bahwa rekening berhasil dibuat dan telah aktif.

Contoh:

> **Rekening Berhasil Dibuat**

Rekening telah berhasil dibuka dan dapat digunakan setelah memenuhi ketentuan setoran awal yang berlaku.

### Nomor Rekening

Menampilkan nomor rekening yang telah dibuat.

Contoh:

`1234567890`

Fitur:

- `Salin Nomor Rekening`

### Panduan Setoran Awal

Nasabah diberikan beberapa pilihan metode untuk melakukan setoran awal:

#### 1. Transfer dari Bank Lain
Transfer dana ke rekening BCA menggunakan nomor rekening yang telah diberikan.

#### 2. ATM BCA Cardless
Melakukan setoran melalui ATM BCA menggunakan fitur Cardless.

#### 3. Kantor Cabang BCA
Nasabah dapat melakukan setoran awal melalui kantor cabang BCA.

### CTA

Tersedia tombol utama:

`Masuk ke m-BCA`

---

# Konsistensi UI/UX

Seluruh layar Point 7–11 menggunakan sistem desain yang konsisten.

### Spacing

Menggunakan kelipatan spacing:

- `4 dp`
- `8 dp`
- `12 dp`
- `16 dp`
- `24 dp`

### Visual Hierarchy

Konsistensi diterapkan pada:

- Typography hierarchy.
- Primary dan secondary CTA.
- Card dan container.
- Form field.
- Status indicator.
- Progress indicator.
- Error dan validation state.
- Bottom sheet/dialog.
- Navigation pattern.

### Design Token

Seluruh layar menggunakan token visual yang konsisten dengan konsep desain **BCA Mobile**, termasuk:

- Color token.
- Typography token.
- Spacing token.
- Shape/radius token.
- Elevation/shadow.
- Component state.

---

# Final Alur Point 7–11

```text
Point 7
Verifikasi Biometrik Wajah
        ↓
Liveness Detection
        ↓
Point 8A
Antrean Video Call
        ↓
Menunggu Petugas
        ↓
Point 8B
Video Call e-KYC
        ↓
Verifikasi oleh Customer Service
        ↓
Point 9
Buat Kode Akses & PIN Transaksi
        ↓
Point 10
Ringkasan & Konfirmasi Data
        ↓
Finalisasi Pembukaan Rekening
        ↓
Point 11
Rekening Berhasil Dibuat
        ↓
Setoran Awal
        ↓
Masuk ke m-BCA