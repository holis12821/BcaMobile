# Brief Implementasi: Buka Rekening Baru & Ganti Kode Akses

Dokumen ini menjelaskan **workflow end-to-end** untuk dua fitur yang diakses dari
`LoginScreen.kt` — **Buka Rekening Baru** dan **Ganti Kode Akses** — berdasarkan
analisa flow perbankan digital BCA dan regulasi OJK yang berlaku di Indonesia.

Tanggal: 5 September 2026
Sumber referensi:
- Desain: `docs/design/` (wireframe, task flow, visual design)
- Navigasi: `.claude/skills/compose-architecture/references/navigation.md`
- Audit: `docs/design/missing-screens-audit.md` (item A3 & A4)
- Login entry: `app/src/main/java/id/bca/bcamobile/ui/screen/login/LoginScreen.kt`
- Auth graph: `app/src/main/java/id/bca/bcamobile/ui/navigation/AuthGraph.kt`
- Regulasi: POJK No. 12/POJK.01/2017 (eKYC), SEOJK No. 12/SEOJK.07/2014

---

## Status Saat Ini

Kedua halaman sudah terdaftar di navigasi tetapi masih **placeholder**:

| Halaman | Route | File | Baris | Isi Saat Ini |
|---|---|---|---|---|
| Buka Rekening | `BukaRekening` | `AuthGraph.kt` | 93-97 | `Box` + `Text("Buka Rekening")` |
| Ganti Kode Akses | `GantiKodeAkses` | `AuthGraph.kt` | 99-103 | `Box` + `Text("Ganti Kode Akses")` |

Entry point dari `LoginScreen.kt`:
- `onBukaRekeningClick` -> `navController.navigate(BukaRekening)` (AuthGraph.kt:40)
- `onGantiKodeAksesClick` -> `navController.navigate(GantiKodeAkses)` (AuthGraph.kt:41)

Route di `Route.kt`:
```kotlin
@Serializable data object BukaRekening   // baris 11
@Serializable data object GantiKodeAkses // baris 12
```

---

# A. BUKA REKENING BARU — Flow Lengkap End-to-End

## A.1 Analisa Flow Pembukaan Rekening Digital BCA

Berdasarkan praktik perbankan digital di Indonesia dan regulasi OJK, pembukaan rekening
via aplikasi mobile wajib melalui proses **eKYC (electronic Know Your Customer)**.
Regulasi POJK mewajibkan verifikasi identitas nasabah secara digital yang setara dengan
tatap muka, termasuk **video call** dengan petugas bank.

Flow pembukaan rekening BCA secara digital umumnya:

```
Login Screen
  |
  v
[1. Pilih Jenis Rekening]     -> User memilih tipe (Tahapan, Tahapan Xpresi, TabunganKu)
  |
  v
[2. Syarat & Ketentuan]       -> User membaca dan menyetujui S&K
  |
  v
[3. Data Pribadi - Bagian 1]  -> NIK, nama lengkap, tempat/tanggal lahir, jenis kelamin
  |
  v
[4. Data Pribadi - Bagian 2]  -> Alamat KTP, alamat domisili, pekerjaan, penghasilan
  |
  v
[5. Data Tambahan]            -> Nama ibu kandung, tujuan buka rekening, sumber dana
  |
  v
[6. Upload KTP]               -> Foto KTP (OCR membaca data otomatis)
  |
  v
[7. Selfie + Liveness]        -> Foto selfie untuk verifikasi wajah cocok dengan KTP
  |
  v
[8. Video Call Verifikasi]    -> Petugas bank memverifikasi identitas (WAJIB OJK)
  |
  v
[9. Buat Kode Akses & PIN]   -> User membuat kode akses m-BCA (6 digit) + PIN transaksi
  |
  v
[10. Pilih Desain Kartu]     -> Pilih desain kartu debit (opsional, tergantung jenis)
  |
  v
[11. Setoran Awal]           -> Info metode setoran awal (transfer/ATM/cabang)
  |
  v
[12. Ringkasan & Konfirmasi] -> Review semua data sebelum submit
  |
  v
[13. Proses Pembuatan]       -> Loading, backend memproses
  |
  v
[14. Rekening Berhasil]      -> Nomor rekening ditampilkan, rekening aktif
  |
  v
[Kembali ke Login]           -> User bisa login dengan kode akses yang baru dibuat
```

## A.2 Detail Setiap Step

### Step 1 — Pilih Jenis Rekening

**Tujuan:** User memilih jenis rekening yang ingin dibuka.

| Komponen | Detail |
|---|---|
| TopBar | Judul "Buka Rekening" + tombol back |
| Hero | Ilustrasi/ikon pembukaan rekening |
| Card list | Daftar jenis rekening sebagai card yang bisa dipilih |

**Jenis rekening yang ditampilkan:**

| Jenis | Deskripsi | Setoran Awal | Fitur Utama |
|---|---|---|---|
| Tahapan BCA | Tabungan utama BCA | Rp 500.000 | Debit, m-BCA, KlikBCA |
| Tahapan Xpresi | Tabungan digital untuk anak muda | Rp 50.000 | Desain kartu custom, tanpa biaya admin |
| TabunganKu | Tabungan nasional tanpa biaya admin | Rp 20.000 | Tanpa biaya admin bulanan |

**Interaksi:**
- Tap card -> pilih jenis, lanjut ke step 2
- Tap back -> kembali ke Login

### Step 2 — Syarat & Ketentuan

**Tujuan:** User membaca dan menyetujui syarat & ketentuan pembukaan rekening.

| Komponen | Detail |
|---|---|
| TopBar | Judul "Syarat & Ketentuan" + back |
| Step indicator | Step 1 dari 6 (atau progress bar) |
| ScrollableText | Isi S&K lengkap (scrollable) |
| Checkbox | "Saya telah membaca dan menyetujui Syarat & Ketentuan" |
| Tombol "Lanjut" | Disabled sampai checkbox dicentang |

**Validasi:**
- Checkbox wajib dicentang sebelum lanjut
- User harus scroll sampai bawah (opsional, tapi best practice)

### Step 3 — Data Pribadi Bagian 1 (Identitas)

**Tujuan:** Mengumpulkan data identitas dasar.

| Field | Tipe | Validasi |
|---|---|---|
| NIK (No. KTP) | TextField, 16 digit, numerik | Wajib, tepat 16 digit, format valid |
| Nama Lengkap | TextField | Wajib, sesuai KTP, min 3 karakter |
| Tempat Lahir | TextField | Wajib |
| Tanggal Lahir | DatePicker | Wajib, usia min 17 tahun |
| Jenis Kelamin | RadioButton (Laki-laki / Perempuan) | Wajib |

**Interaksi:**
- Semua field wajib diisi sebelum tombol "Lanjut" aktif
- Inline error per field jika validasi gagal
- Back -> kembali ke step sebelumnya (data tersimpan di state)

### Step 4 — Data Pribadi Bagian 2 (Alamat & Pekerjaan)

**Tujuan:** Alamat dan informasi pekerjaan.

| Field | Tipe | Validasi |
|---|---|---|
| Alamat sesuai KTP | TextField (multiline) | Wajib |
| RT/RW | TextField | Wajib, format XX/XX |
| Kelurahan | TextField | Wajib |
| Kecamatan | TextField | Wajib |
| Kota/Kabupaten | Dropdown/Search | Wajib |
| Provinsi | Dropdown | Wajib |
| Kode Pos | TextField, 5 digit | Wajib |
| Alamat domisili sama? | Checkbox | Jika tidak, tampilkan form alamat domisili |
| Pekerjaan | Dropdown | Wajib (Karyawan Swasta, PNS, Wiraswasta, Pelajar, dll) |
| Penghasilan per bulan | Dropdown range | Wajib (< 5 juta, 5-10 juta, 10-50 juta, > 50 juta) |

### Step 5 — Data Tambahan (Keamanan & Tujuan)

**Tujuan:** Informasi keamanan dan kepatuhan.

| Field | Tipe | Validasi |
|---|---|---|
| Nama ibu kandung | TextField | Wajib, min 3 karakter (pertanyaan keamanan bank) |
| Tujuan pembukaan | Dropdown | Wajib (Simpanan, Transaksi bisnis, Investasi, dll) |
| Sumber dana | Dropdown | Wajib (Gaji, Usaha, Warisan, dll) |
| Email | TextField | Wajib, format email valid (untuk e-statement) |
| No. Handphone | TextField + prefix +62 | Wajib, format valid, untuk OTP & notifikasi |

### Step 6 — Upload Foto KTP

**Tujuan:** Mengambil foto KTP untuk verifikasi identitas via OCR.

| Komponen | Detail |
|---|---|
| Instruksi | "Siapkan KTP asli Anda. Pastikan foto jelas dan tidak terpotong." |
| Preview area | Bingkai panduan ukuran KTP |
| Tombol "Ambil Foto" | Membuka kamera |
| Hasil foto | Preview foto yang diambil |
| OCR feedback | Data yang berhasil dibaca dari KTP (NIK, nama, dll) ditampilkan untuk konfirmasi |
| Tombol "Ulangi" | Jika foto kurang jelas |
| Tombol "Lanjut" | Setelah foto berhasil dan data OCR dikonfirmasi |

**Catatan teknis:**
- Membutuhkan izin kamera (`android.permission.CAMERA`)
- OCR dilakukan di backend (upload foto, terima respons data)
- Data OCR ditampilkan untuk user konfirmasi — jika tidak cocok, user bisa edit
- Validasi: foto harus cukup terang, tidak blur, KTP terlihat utuh

### Step 7 — Selfie + Deteksi Liveness

**Tujuan:** Verifikasi bahwa user adalah orang yang sama dengan foto di KTP.

| Komponen | Detail |
|---|---|
| Instruksi | "Posisikan wajah Anda di dalam bingkai" |
| Oval frame | Bingkai wajah di tengah layar |
| Liveness check | Instruksi gerakan: "Kedipkan mata", "Tolehkan kepala ke kiri", dsb |
| Indikator proses | Progress bar atau animasi saat memproses |
| Feedback | "Wajah terdeteksi" / "Posisikan ulang wajah Anda" |

**Catatan teknis:**
- Membutuhkan kamera depan
- Liveness detection mencegah penggunaan foto cetak/screenshot
- Pemrosesan bisa lokal (ML Kit) atau di backend
- Hasil: match score wajah selfie vs foto KTP

**State yang mungkin:**
- `Idle` — menunggu user memposisikan wajah
- `Detecting` — memproses liveness
- `LivenessAction` — instruksi gerakan (kedip, toleh)
- `Processing` — mengirim ke backend untuk face matching
- `Success` — wajah cocok, lanjut
- `Failed` — wajah tidak cocok / liveness gagal, tombol "Coba Lagi"

### Step 8 — Video Call Verifikasi (eKYC)

**Tujuan:** Verifikasi identitas oleh petugas bank melalui video call. **Ini wajib
berdasarkan regulasi OJK** (POJK No. 12/POJK.01/2017 tentang Penerapan Program
Anti Pencucian Uang).

```
                          +---------------------------+
                          |     Video Call Screen     |
                          |                           |
                          |  +---------------------+  |
                          |  |                     |  |
                          |  |   Video Petugas     |  |
                          |  |   Bank BCA           |  |
                          |  |                     |  |
                          |  +---------------------+  |
                          |                           |
                          |  +--------+               |
                          |  | Self   |               |
                          |  | View   |               |
                          |  +--------+               |
                          |                           |
                          |  [Mic]  [Camera]  [End]   |
                          +---------------------------+
```

| Komponen | Detail |
|---|---|
| Pre-call screen | "Petugas bank akan menghubungi Anda untuk verifikasi" |
| Antrian | "Anda berada di antrian ke-X. Estimasi tunggu: Y menit" |
| Video area | Video petugas (besar) + video user (kecil, pojok) |
| Kontrol | Toggle mic, toggle kamera, end call |
| Timer | Durasi panggilan |
| Post-call | "Verifikasi sedang diproses" atau "Verifikasi berhasil" |

**Flow detail Video Call:**

1. **Layar Antrian** — User melihat posisi antrian dan estimasi waktu tunggu
   - Opsi: "Jadwalkan nanti" (pilih waktu video call)
   - Opsi: "Tunggu sekarang" (masuk antrian langsung)
2. **Notifikasi masuk** — Petugas siap, user menerima panggilan
3. **Video call berlangsung** — Petugas melakukan:
   - Konfirmasi nama dan data yang diisi
   - Minta user menunjukkan KTP asli ke kamera
   - Verifikasi wajah user cocok dengan KTP
   - Tanya beberapa pertanyaan keamanan
4. **Selesai** — Petugas menutup call, status berubah ke "Verifikasi Berhasil"

**State yang mungkin:**
- `Idle` — belum mulai
- `Scheduling` — user memilih jadwal
- `Queuing` — sedang antri, menunggu petugas
- `Connecting` — petugas terhubung, loading
- `InCall` — video call berlangsung
- `Ended` — call selesai
- `Approved` — verifikasi disetujui petugas
- `Rejected` — verifikasi ditolak (data tidak cocok, KTP tidak jelas, dll)
- `Rescheduled` — perlu dijadwalkan ulang

**Catatan penting:**
- Video call membutuhkan izin kamera + mikrofon
- Koneksi internet stabil (tampilkan warning jika sinyal lemah)
- Jika call terputus, user bisa reconnect atau reschedule
- Jam operasional: Senin-Jumat 08:00-16:00 WIB (tampilkan info ini)
- Di luar jam operasional: hanya opsi jadwalkan tersedia

### Step 9 — Buat Kode Akses & PIN Transaksi

**Tujuan:** User membuat kredensial untuk mengakses m-BCA.

**Bagian A — Kode Akses (6 digit alfanumerik):**

| Komponen | Detail |
|---|---|
| Instruksi | "Buat kode akses m-BCA Anda (6 digit)" |
| Dot indicator | 6 dot tersamar |
| Numpad | Tombol 0-9, hapus |
| Validasi realtime | Tidak boleh berurut, tidak boleh sama semua |

**Bagian B — Konfirmasi Kode Akses:**
- Ulangi input kode akses, harus cocok

**Bagian C — PIN Transaksi (6 digit):**
- Sama seperti kode akses, tapi untuk otorisasi transaksi
- PIN berbeda dari kode akses

**Bagian D — Konfirmasi PIN Transaksi:**
- Ulangi input PIN, harus cocok

**Validasi:**
- Kode akses tidak boleh berurut (123456) atau sama semua (111111)
- PIN transaksi tidak boleh sama dengan kode akses
- Konfirmasi harus cocok

### Step 10 — Pilih Desain Kartu Debit (Kondisional)

**Tujuan:** User memilih desain kartu debit (khusus Tahapan Xpresi).

| Komponen | Detail |
|---|---|
| Grid gallery | Koleksi desain kartu (gambar) |
| Preview | Kartu terpilih ditampilkan besar |
| Tombol "Pilih" | Konfirmasi desain |

**Catatan:** Step ini hanya muncul untuk jenis rekening Tahapan Xpresi.
Untuk Tahapan BCA dan TabunganKu, step ini dilewati.

### Step 11 — Informasi Setoran Awal

**Tujuan:** Memberitahu user cara melakukan setoran awal.

| Komponen | Detail |
|---|---|
| Info nominal | Minimal setoran awal sesuai jenis rekening |
| Metode setoran | Daftar cara setor: Transfer dari bank lain, Setor di ATM, Setor di cabang |
| Batas waktu | "Setoran awal harus dilakukan dalam 30 hari" |
| Tombol "Lanjut" | Ke ringkasan |

### Step 12 — Ringkasan & Konfirmasi

**Tujuan:** Review semua data sebelum disubmit.

| Komponen | Detail |
|---|---|
| Section data pribadi | Nama, NIK, TTL, dll (read-only) |
| Section alamat | Alamat KTP + domisili |
| Section rekening | Jenis rekening, desain kartu |
| Section kontak | Email, no. HP |
| Checkbox | "Saya menyatakan data di atas benar dan lengkap" |
| Tombol "Buka Rekening" | Submit (disabled sampai checkbox dicentang) |
| Tombol "Ubah Data" | Kembali ke step terkait untuk edit |

### Step 13 — Proses Pembuatan (Loading)

| Komponen | Detail |
|---|---|
| Animasi loading | Indikator proses di tengah layar |
| Teks | "Sedang memproses pembukaan rekening Anda..." |
| Sub-teks | "Mohon tunggu, jangan tutup aplikasi" |

**Catatan:** Back button dan gesture back dinonaktifkan di step ini.

### Step 14 — Rekening Berhasil Dibuat

**Tujuan:** Menampilkan informasi rekening baru yang berhasil dibuat.

| Komponen | Detail |
|---|---|
| Ikon sukses | Checkmark hijau besar (`AppColor.Success*`) |
| Judul | "Rekening Berhasil Dibuat!" |
| Nomor rekening | Ditampilkan besar, bisa di-copy |
| Nama pemilik | Nama sesuai data yang diisi |
| Jenis rekening | Tahapan / Xpresi / TabunganKu |
| Info setoran awal | Reminder nominal & batas waktu |
| Tombol "Salin Nomor Rekening" | Copy ke clipboard |
| Tombol "Selesai" | Kembali ke Login (user bisa login dengan kode akses baru) |

## A.3 Diagram Flow Lengkap

```
LoginScreen
    |
    | tap "Buka Rekening Baru"
    v
+---[Step 1: Pilih Jenis Rekening]
|       |
|       v
|   [Step 2: Syarat & Ketentuan]
|       |
|       v
|   [Step 3: Data Pribadi 1 - Identitas]
|       |
|       v
|   [Step 4: Data Pribadi 2 - Alamat & Pekerjaan]
|       |
|       v
|   [Step 5: Data Tambahan - Ibu Kandung, Tujuan, Sumber Dana]
|       |
|       v
|   [Step 6: Upload Foto KTP]  -----> (gagal/blur) ----> [Ulangi Foto]
|       |                                                      |
|       | (berhasil + OCR)                                     |
|       v                                                      |
|   [Step 7: Selfie + Liveness] <------------------------------+
|       |
|       | (wajah cocok)
|       v
|   [Step 8: Video Call Verifikasi - eKYC] ---------> (ditolak) ---> [Info Ditolak + Coba Lagi]
|       |       ^                                                          |
|       |       |--- (di luar jam) ---> [Jadwalkan Video Call]             |
|       |       |--- (terputus) ------> [Reconnect / Reschedule]          |
|       |                                                                  |
|       | (disetujui)                                                      |
|       v                                                                  |
|   [Step 9: Buat Kode Akses & PIN] <--------- (reschedule berhasil) -----+
|       |
|       v
|   [Step 10: Pilih Desain Kartu]  (hanya Tahapan Xpresi, lainnya skip)
|       |
|       v
|   [Step 11: Info Setoran Awal]
|       |
|       v
|   [Step 12: Ringkasan & Konfirmasi] ----> (ubah data) ---> kembali ke step terkait
|       |
|       | (submit)
|       v
|   [Step 13: Proses Pembuatan - Loading]
|       |
|       | (berhasil)                    (gagal)
|       v                                v
|   [Step 14: Rekening Berhasil]    [Error Screen + Retry]
|       |
|       | tap "Selesai"
|       v
+-----> LoginScreen (user bisa login)
```

## A.4 Dampak pada Navigasi

Flow ini multi-step dan cukup panjang. Ada dua pendekatan arsitektur:

**Opsi A — Satu composable, state-driven (DIREKOMENDASIKAN):**
- Satu route `BukaRekening` di `AuthGraph`
- Step dikelola via `BukaRekeningUiState.step` (enum)
- Back button mundur satu step, bukan popBackStack
- Lebih sederhana, tidak perlu banyak route baru

**Opsi B — Multi-route sebagai nested graph:**
- Buat `Graph.BukaRekening` bersarang di `Graph.Auth`
- Setiap step adalah route tersendiri
- Lebih kompleks, tapi sesuai jika step sangat berat (kamera, video call)

**Rekomendasi:** Campuran — step form (1-5, 9-12) sebagai state-driven dalam satu
composable, tapi step kamera (6-7) dan video call (8) sebagai route terpisah karena
membutuhkan izin sistem dan lifecycle khusus.

Route baru yang perlu ditambah ke `Route.kt`:
```kotlin
// ── Auth > Buka Rekening ────────────────────────────────────
@Serializable data object BukaRekening          // existing — jadi hub/form
@Serializable data object BukaRekeningKtp       // upload KTP (kamera)
@Serializable data object BukaRekeningSelfie    // selfie + liveness
@Serializable data object BukaRekeningVideoCall // video call eKYC
@Serializable data object BukaRekeningBerhasil  // success screen
```

**PENTING:** Menambah route berarti update `navigation.md` di commit yang sama.

## A.5 File yang Perlu Dibuat

| File | Tujuan |
|---|---|
| `ui/screen/buka_rekening/BukaRekeningScreen.kt` | Composable utama (step 1-5, 9-12) |
| `ui/screen/buka_rekening/BukaRekeningViewModel.kt` | State & validasi form |
| `ui/screen/buka_rekening/BukaRekeningKtpScreen.kt` | Upload foto KTP (step 6) |
| `ui/screen/buka_rekening/BukaRekeningSelfieScreen.kt` | Selfie + liveness (step 7) |
| `ui/screen/buka_rekening/BukaRekeningVideoCallScreen.kt` | Video call eKYC (step 8) |
| `ui/screen/buka_rekening/BukaRekeningBerhasilScreen.kt` | Success screen (step 14) |

## A.6 UiState

```
data class BukaRekeningUiState(
    // Step tracking
    val step: BukaRekeningStep = BukaRekeningStep.PILIH_JENIS,

    // Step 1
    val jenisRekening: JenisRekening? = null,

    // Step 2
    val syaratDisetujui: Boolean = false,

    // Step 3 - Data Pribadi 1
    val nik: String = "",
    val namaLengkap: String = "",
    val tempatLahir: String = "",
    val tanggalLahir: String = "",
    val jenisKelamin: JenisKelamin? = null,

    // Step 4 - Data Pribadi 2
    val alamatKtp: String = "",
    val rtRw: String = "",
    val kelurahan: String = "",
    val kecamatan: String = "",
    val kota: String = "",
    val provinsi: String = "",
    val kodePos: String = "",
    val alamatDomisiliSama: Boolean = true,
    val alamatDomisili: String = "",
    val pekerjaan: String = "",
    val penghasilan: String = "",

    // Step 5 - Data Tambahan
    val namaIbuKandung: String = "",
    val tujuanPembukaan: String = "",
    val sumberDana: String = "",
    val email: String = "",
    val nomorHp: String = "",

    // Step 9 - Kredensial
    val kodeAkses: String = "",
    val konfirmasiKodeAkses: String = "",
    val pinTransaksi: String = "",
    val konfirmasiPin: String = "",

    // Step 10 - Kartu
    val desainKartu: String? = null,

    // Step 12 - Konfirmasi
    val dataKonfirmasi: Boolean = false,

    // General
    val isLoading: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
)

enum class BukaRekeningStep {
    PILIH_JENIS,
    SYARAT_KETENTUAN,
    DATA_PRIBADI_1,
    DATA_PRIBADI_2,
    DATA_TAMBAHAN,
    // Step 6-8 ditangani route terpisah
    BUAT_KREDENSIAL,
    PILIH_DESAIN_KARTU,
    SETORAN_AWAL,
    RINGKASAN,
    PROSES,
}

enum class JenisRekening { TAHAPAN, TAHAPAN_XPRESI, TABUNGANKU }
enum class JenisKelamin { LAKI_LAKI, PEREMPUAN }
```

## A.7 String Resources

```xml
<!-- Buka Rekening Screen -->
<string name="buka_rekening_title">Buka Rekening</string>
<string name="buka_rekening_pilih_jenis">Pilih Jenis Rekening</string>
<string name="buka_rekening_pilih_jenis_desc">Pilih jenis rekening yang sesuai dengan kebutuhan Anda</string>

<string name="buka_rekening_tahapan">Tahapan BCA</string>
<string name="buka_rekening_tahapan_desc">Tabungan utama BCA untuk kebutuhan sehari-hari</string>
<string name="buka_rekening_tahapan_setoran">Setoran awal: Rp 500.000</string>
<string name="buka_rekening_xpresi">Tahapan Xpresi</string>
<string name="buka_rekening_xpresi_desc">Tabungan digital untuk anak muda, tanpa biaya admin</string>
<string name="buka_rekening_xpresi_setoran">Setoran awal: Rp 50.000</string>
<string name="buka_rekening_tabunganku">TabunganKu</string>
<string name="buka_rekening_tabunganku_desc">Tabungan nasional tanpa biaya administrasi bulanan</string>
<string name="buka_rekening_tabunganku_setoran">Setoran awal: Rp 20.000</string>

<string name="buka_rekening_syarat_ketentuan">Syarat &amp; Ketentuan</string>
<string name="buka_rekening_setuju_sk">Saya telah membaca dan menyetujui Syarat &amp; Ketentuan</string>

<string name="buka_rekening_data_pribadi">Data Pribadi</string>
<string name="buka_rekening_nik">NIK (Nomor KTP)</string>
<string name="buka_rekening_nik_hint">Masukkan 16 digit NIK</string>
<string name="buka_rekening_nama">Nama Lengkap (sesuai KTP)</string>
<string name="buka_rekening_tempat_lahir">Tempat Lahir</string>
<string name="buka_rekening_tanggal_lahir">Tanggal Lahir</string>
<string name="buka_rekening_jenis_kelamin">Jenis Kelamin</string>
<string name="buka_rekening_laki_laki">Laki-laki</string>
<string name="buka_rekening_perempuan">Perempuan</string>

<string name="buka_rekening_alamat_ktp">Alamat (sesuai KTP)</string>
<string name="buka_rekening_rt_rw">RT/RW</string>
<string name="buka_rekening_kelurahan">Kelurahan</string>
<string name="buka_rekening_kecamatan">Kecamatan</string>
<string name="buka_rekening_kota">Kota/Kabupaten</string>
<string name="buka_rekening_provinsi">Provinsi</string>
<string name="buka_rekening_kode_pos">Kode Pos</string>
<string name="buka_rekening_alamat_domisili_sama">Alamat domisili sama dengan KTP</string>
<string name="buka_rekening_alamat_domisili">Alamat Domisili</string>
<string name="buka_rekening_pekerjaan">Pekerjaan</string>
<string name="buka_rekening_penghasilan">Penghasilan per Bulan</string>

<string name="buka_rekening_ibu_kandung">Nama Ibu Kandung</string>
<string name="buka_rekening_tujuan">Tujuan Pembukaan Rekening</string>
<string name="buka_rekening_sumber_dana">Sumber Dana</string>
<string name="buka_rekening_email">Alamat Email</string>
<string name="buka_rekening_no_hp">Nomor Handphone</string>

<string name="buka_rekening_foto_ktp_title">Foto KTP</string>
<string name="buka_rekening_foto_ktp_instruksi">Siapkan KTP asli Anda. Pastikan foto jelas, tidak blur, dan tidak terpotong.</string>
<string name="buka_rekening_ambil_foto">Ambil Foto</string>
<string name="buka_rekening_ulangi_foto">Ulangi Foto</string>

<string name="buka_rekening_selfie_title">Verifikasi Wajah</string>
<string name="buka_rekening_selfie_instruksi">Posisikan wajah Anda di dalam bingkai</string>
<string name="buka_rekening_selfie_kedip">Kedipkan mata Anda</string>
<string name="buka_rekening_selfie_toleh">Tolehkan kepala ke kiri</string>
<string name="buka_rekening_selfie_proses">Memverifikasi wajah...</string>
<string name="buka_rekening_selfie_cocok">Wajah berhasil diverifikasi</string>
<string name="buka_rekening_selfie_gagal">Verifikasi wajah gagal. Silakan coba lagi.</string>

<string name="buka_rekening_videocall_title">Video Call Verifikasi</string>
<string name="buka_rekening_videocall_desc">Petugas bank akan menghubungi Anda untuk verifikasi identitas</string>
<string name="buka_rekening_videocall_antrian">Anda berada di antrian ke-%1$d</string>
<string name="buka_rekening_videocall_estimasi">Estimasi tunggu: %1$d menit</string>
<string name="buka_rekening_videocall_tunggu">Tunggu Sekarang</string>
<string name="buka_rekening_videocall_jadwalkan">Jadwalkan Nanti</string>
<string name="buka_rekening_videocall_jam_operasional">Jam operasional: Senin - Jumat, 08.00 - 16.00 WIB</string>
<string name="buka_rekening_videocall_berhasil">Verifikasi berhasil</string>
<string name="buka_rekening_videocall_ditolak">Verifikasi ditolak. Silakan hubungi Halo BCA 1500888.</string>
<string name="buka_rekening_videocall_terputus">Koneksi terputus</string>
<string name="buka_rekening_videocall_reconnect">Sambungkan Ulang</string>

<string name="buka_rekening_buat_kode_akses">Buat Kode Akses m-BCA</string>
<string name="buka_rekening_konfirmasi_kode_akses">Konfirmasi Kode Akses</string>
<string name="buka_rekening_buat_pin">Buat PIN Transaksi</string>
<string name="buka_rekening_konfirmasi_pin">Konfirmasi PIN Transaksi</string>
<string name="buka_rekening_pin_beda_kode_akses">PIN transaksi tidak boleh sama dengan kode akses</string>

<string name="buka_rekening_desain_kartu">Pilih Desain Kartu</string>
<string name="buka_rekening_desain_kartu_desc">Pilih desain kartu debit Anda</string>

<string name="buka_rekening_setoran_awal">Setoran Awal</string>
<string name="buka_rekening_setoran_minimal">Minimal setoran awal: %1$s</string>
<string name="buka_rekening_setoran_batas">Setoran awal harus dilakukan dalam 30 hari</string>
<string name="buka_rekening_setoran_transfer">Transfer dari bank lain</string>
<string name="buka_rekening_setoran_atm">Setor tunai di ATM BCA</string>
<string name="buka_rekening_setoran_cabang">Setor di kantor cabang BCA</string>

<string name="buka_rekening_ringkasan">Ringkasan Data</string>
<string name="buka_rekening_konfirmasi_data">Saya menyatakan data di atas benar dan lengkap</string>
<string name="buka_rekening_ubah_data">Ubah Data</string>
<string name="buka_rekening_submit">Buka Rekening</string>
<string name="buka_rekening_proses">Sedang memproses pembukaan rekening Anda...</string>
<string name="buka_rekening_jangan_tutup">Mohon tunggu, jangan tutup aplikasi</string>

<string name="buka_rekening_berhasil_title">Rekening Berhasil Dibuat!</string>
<string name="buka_rekening_nomor_rekening">Nomor Rekening Anda</string>
<string name="buka_rekening_salin">Salin Nomor Rekening</string>
<string name="buka_rekening_selesai">Selesai</string>
<string name="buka_rekening_reminder_setoran">Lakukan setoran awal dalam 30 hari agar rekening tetap aktif</string>

<string name="buka_rekening_lanjut">Lanjut</string>
<string name="buka_rekening_kembali">Kembali</string>
<string name="buka_rekening_error_nik">NIK harus 16 digit</string>
<string name="buka_rekening_error_usia">Usia minimal 17 tahun</string>
<string name="buka_rekening_error_email">Format email tidak valid</string>
<string name="buka_rekening_error_hp">Nomor handphone tidak valid</string>
<string name="cd_buka_rekening_back">Kembali</string>
```

---

# B. GANTI KODE AKSES — Flow Lengkap End-to-End

## B.1 Analisa Flow Ganti Kode Akses BCA

Berdasarkan praktik BCA mobile, **Ganti Kode Akses** diakses dari login screen dan
melayani dua skenario: user ingat kode lama (ganti biasa) dan user lupa kode lama
(reset via verifikasi kartu + OTP). Karena tombol ini ada di halaman login (user
belum terautentikasi), flow harus memverifikasi identitas terlebih dahulu.

Flow sesuai task flow di `docs/design/workflow_bca.webp` yang menunjukkan proses
login menggunakan kode akses 6 digit:

```
LoginScreen
  |
  | tap "Ganti Kode Akses"
  v
[1. Input Nomor Kartu ATM]    -> Identifikasi nasabah (16 digit nomor kartu debit)
  |
  v
[2. Verifikasi OTP]           -> OTP dikirim ke nomor HP terdaftar
  |
  v
[3. Input Kode Akses Lama]    -> Verifikasi kode akses saat ini
  |     |
  |     +--- (lupa?) ---------> [3b. Verifikasi PIN ATM] sebagai alternatif
  |                                  |
  v                                  v
[4. Buat Kode Akses Baru]     -> Input 6 digit kode akses baru
  |
  v
[5. Konfirmasi Kode Akses]    -> Ulangi input, harus cocok
  |
  v
[6. Proses]                   -> Loading, backend memproses
  |
  v
[7. Berhasil]                 -> Konfirmasi sukses, kembali ke Login
```

## B.2 Detail Setiap Step

### Step 1 — Input Nomor Kartu ATM/Debit

**Tujuan:** Identifikasi nasabah melalui nomor kartu.

| Komponen | Detail |
|---|---|
| TopBar | "Ganti Kode Akses" + back button |
| Step indicator | Step 1 dari 5 |
| Instruksi | "Masukkan nomor kartu ATM/Debit BCA Anda" |
| Ilustrasi kartu | Gambar kartu ATM dengan highlight posisi nomor 16 digit |
| TextField | 16 digit, numerik, auto-format 4-4-4-4 (XXXX XXXX XXXX XXXX) |
| Tombol "Lanjut" | Disabled sampai 16 digit valid |

**Validasi:**
- Tepat 16 digit
- Format numerik
- Luhn check (opsional, untuk validasi format kartu)

**Error state:**
- "Nomor kartu tidak ditemukan" — jika backend tidak mengenali
- "Kartu telah diblokir" — kartu tidak aktif

### Step 2 — Verifikasi OTP

**Tujuan:** Verifikasi bahwa user memiliki akses ke nomor HP terdaftar.

| Komponen | Detail |
|---|---|
| TopBar | "Ganti Kode Akses" + back button |
| Step indicator | Step 2 dari 5 |
| Info | "Kode OTP telah dikirim ke nomor 0812-XXXX-XX89" (nomor disamarkan) |
| Input OTP | 6 digit, auto-focus, auto-read SMS (opsional) |
| Timer countdown | "Kirim ulang dalam 00:59" |
| Link "Kirim Ulang" | Aktif setelah timer habis |
| Tombol "Verifikasi" | Auto-submit setelah 6 digit terisi |

**Validasi:**
- OTP harus cocok dengan yang dikirim server
- OTP memiliki masa berlaku (biasanya 5 menit)
- Maksimal 3 percobaan, setelah itu blokir sementara

**State:**
- `Sending` — OTP sedang dikirim
- `Sent` — OTP terkirim, timer berjalan
- `Verifying` — sedang memverifikasi
- `Expired` — OTP kadaluarsa, tampilkan "Kirim Ulang"
- `MaxAttempts` — percobaan habis, "Coba lagi dalam 30 menit"

### Step 3 — Input Kode Akses Lama

**Tujuan:** Verifikasi user mengetahui kode akses saat ini.

| Komponen | Detail |
|---|---|
| TopBar | "Ganti Kode Akses" + back button |
| Step indicator | Step 3 dari 5 |
| Instruksi | "Masukkan kode akses lama Anda" |
| 6 dot indicator | Menunjukkan digit terisi (tersamar) |
| Numpad | 0-9, hapus (reuse dari `KodeAksesScreen.kt`) |
| Link "Lupa Kode Akses?" | Navigasi ke verifikasi alternatif (PIN ATM) |

**Percabangan "Lupa Kode Akses?":**

Jika user tidak ingat kode akses lama, arahkan ke **Step 3b — Verifikasi PIN ATM**:

| Komponen | Detail |
|---|---|
| Instruksi | "Masukkan PIN ATM Anda sebagai verifikasi" |
| 6 dot indicator | PIN ATM (tersamar) |
| Numpad | 0-9, hapus |

Ini memberikan alternatif verifikasi bagi user yang lupa kode akses tapi masih
ingat PIN ATM mereka.

**Validasi:**
- Kode akses lama / PIN ATM harus cocok
- Maksimal 3 percobaan salah
- Setelah 3x salah: "Akun Anda diblokir sementara. Hubungi Halo BCA 1500888."

### Step 4 — Buat Kode Akses Baru

**Tujuan:** User membuat kode akses baru.

| Komponen | Detail |
|---|---|
| TopBar | "Ganti Kode Akses" + back button |
| Step indicator | Step 4 dari 5 |
| Instruksi | "Buat kode akses baru Anda" |
| Sub-instruksi | "Kode akses terdiri dari 6 digit angka" |
| 6 dot indicator | Menunjukkan digit terisi |
| Numpad | 0-9, hapus |
| Aturan | Teks kecil: syarat kode akses yang valid |

**Validasi realtime (tampilkan sebagai checklist):**

| Aturan | Contoh yang ditolak |
|---|---|
| Tidak boleh angka berurut naik | 123456 |
| Tidak boleh angka berurut turun | 654321 |
| Tidak boleh angka sama semua | 111111, 222222 |
| Tidak boleh sama dengan kode lama | (kode yang baru diinput di step 3) |
| Harus 6 digit | kurang dari 6 |

### Step 5 — Konfirmasi Kode Akses Baru

**Tujuan:** Memastikan user mengetik kode akses baru dengan benar.

| Komponen | Detail |
|---|---|
| TopBar | "Ganti Kode Akses" + back button |
| Step indicator | Step 5 dari 5 |
| Instruksi | "Ulangi kode akses baru Anda" |
| 6 dot indicator | Menunjukkan digit terisi |
| Numpad | 0-9, hapus |

**Validasi:**
- Harus sama persis dengan input di Step 4
- Jika tidak cocok: "Kode akses tidak cocok. Silakan ulangi." + reset kedua field

### Step 6 — Proses (Loading)

| Komponen | Detail |
|---|---|
| Loading indicator | Spinner/progress di tengah layar |
| Teks | "Sedang mengubah kode akses Anda..." |

Back button dinonaktifkan.

### Step 7 — Berhasil

**Tujuan:** Konfirmasi perubahan kode akses berhasil.

| Komponen | Detail |
|---|---|
| Ikon sukses | Checkmark hijau besar di lingkaran (`AppColor.Success500`) |
| Judul | "Kode Akses Berhasil Diubah" |
| Deskripsi | "Silakan login kembali menggunakan kode akses baru Anda." |
| Info keamanan | "Jangan bagikan kode akses Anda kepada siapapun." |
| Tombol "Login Sekarang" | Kembali ke LoginScreen |

## B.3 Diagram Flow Lengkap

```
LoginScreen
    |
    | tap "Ganti Kode Akses"
    v
+---[Step 1: Input Nomor Kartu ATM (16 digit)]
|       |
|       | (valid)                          (tidak ditemukan)
|       v                                       v
|   [Step 2: Verifikasi OTP]              [Error: Kartu tidak ditemukan]
|       |                                       |
|       | (OTP cocok)      (salah 3x)           | retry
|       v                    v                  v
|   [Step 3: Input Kode Akses Lama] <-----------+
|       |               |
|       | (benar)       | tap "Lupa Kode Akses?"
|       |               v
|       |         [Step 3b: Input PIN ATM]
|       |               |
|       | (benar)       | (benar)
|       v               v
|   [Step 4: Buat Kode Akses Baru (6 digit)]
|       |
|       | (valid, lolos semua aturan)
|       v
|   [Step 5: Konfirmasi Kode Akses Baru]
|       |
|       | (cocok)                    (tidak cocok)
|       v                                v
|   [Step 6: Proses - Loading]     [Error + Reset ke Step 4]
|       |
|       | (berhasil)        (gagal)
|       v                     v
|   [Step 7: Berhasil]   [Error + Retry]
|       |
|       | tap "Login Sekarang"
|       v
+-----> LoginScreen
```

## B.4 UiState

```
data class GantiKodeAksesUiState(
    // Step tracking
    val step: GantiKodeAksesStep = GantiKodeAksesStep.INPUT_NOMOR_KARTU,

    // Step 1
    val nomorKartu: String = "",
    val nomorKartuError: String? = null,

    // Step 2 - OTP
    val nomorHpSamar: String = "",    // "0812-XXXX-XX89" dari server
    val otpCode: String = "",
    val otpTimerSeconds: Int = 60,
    val otpAttempts: Int = 0,
    val otpError: String? = null,

    // Step 3
    val kodeAksesLama: String = "",
    val kodeAksesLamaAttempts: Int = 0,
    val kodeAksesLamaError: String? = null,
    val usePinAtm: Boolean = false,   // true jika user pilih "Lupa Kode Akses"

    // Step 3b
    val pinAtm: String = "",
    val pinAtmError: String? = null,

    // Step 4
    val kodeAksesBaru: String = "",
    val kodeAksesBaruErrors: List<String> = emptyList(), // validasi realtime

    // Step 5
    val konfirmasiKodeAkses: String = "",
    val konfirmasiError: String? = null,

    // General
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBlocked: Boolean = false,   // true jika terlalu banyak percobaan salah
)

enum class GantiKodeAksesStep {
    INPUT_NOMOR_KARTU,   // Step 1
    VERIFIKASI_OTP,      // Step 2
    INPUT_KODE_LAMA,     // Step 3
    INPUT_PIN_ATM,       // Step 3b (alternatif)
    BUAT_KODE_BARU,      // Step 4
    KONFIRMASI_KODE,     // Step 5
    PROSES,              // Step 6
    BERHASIL,            // Step 7
}
```

## B.5 Composable Signature

```
@Composable
fun GantiKodeAksesScreen(
    state: GantiKodeAksesUiState,
    onBackClick: () -> Unit,
    // Step 1
    onNomorKartuChange: (String) -> Unit,
    // Step 2
    onOtpChange: (String) -> Unit,
    onKirimUlangOtp: () -> Unit,
    // Step 3 & 3b
    onKodeAksesLamaDigitClick: (Int) -> Unit,
    onKodeAksesLamaDeleteClick: () -> Unit,
    onLupaKodeAksesClick: () -> Unit,
    onPinAtmDigitClick: (Int) -> Unit,
    onPinAtmDeleteClick: () -> Unit,
    // Step 4 & 5
    onKodeAksesBaruDigitClick: (Int) -> Unit,
    onKodeAksesBaruDeleteClick: () -> Unit,
    onKonfirmasiDigitClick: (Int) -> Unit,
    onKonfirmasiDeleteClick: () -> Unit,
    // Navigation
    onLanjutClick: () -> Unit,
    onSelesaiClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
)
```

## B.6 String Resources

```xml
<!-- Ganti Kode Akses Screen -->
<string name="ganti_kode_akses_title">Ganti Kode Akses</string>

<!-- Step 1 -->
<string name="ganti_kode_akses_nomor_kartu_instruksi">Masukkan nomor kartu ATM/Debit BCA Anda</string>
<string name="ganti_kode_akses_nomor_kartu_hint">XXXX XXXX XXXX XXXX</string>
<string name="ganti_kode_akses_error_kartu_invalid">Nomor kartu harus 16 digit</string>
<string name="ganti_kode_akses_error_kartu_tidak_ditemukan">Nomor kartu tidak ditemukan</string>
<string name="ganti_kode_akses_error_kartu_diblokir">Kartu telah diblokir. Hubungi Halo BCA 1500888.</string>

<!-- Step 2 - OTP -->
<string name="ganti_kode_akses_otp_dikirim">Kode OTP telah dikirim ke %1$s</string>
<string name="ganti_kode_akses_otp_hint">Masukkan 6 digit kode OTP</string>
<string name="ganti_kode_akses_otp_kirim_ulang">Kirim Ulang</string>
<string name="ganti_kode_akses_otp_countdown">Kirim ulang dalam %1$02d:%2$02d</string>
<string name="ganti_kode_akses_otp_expired">Kode OTP kadaluarsa. Silakan kirim ulang.</string>
<string name="ganti_kode_akses_otp_salah">Kode OTP salah. Sisa percobaan: %1$d</string>
<string name="ganti_kode_akses_otp_max_attempt">Terlalu banyak percobaan. Coba lagi dalam 30 menit.</string>

<!-- Step 3 -->
<string name="ganti_kode_akses_masukkan_lama">Masukkan kode akses lama Anda</string>
<string name="ganti_kode_akses_lupa">Lupa Kode Akses?</string>
<string name="ganti_kode_akses_lama_salah">Kode akses salah. Sisa percobaan: %1$d</string>

<!-- Step 3b -->
<string name="ganti_kode_akses_masukkan_pin_atm">Masukkan PIN ATM Anda sebagai verifikasi</string>
<string name="ganti_kode_akses_pin_atm_salah">PIN ATM salah. Sisa percobaan: %1$d</string>

<!-- Step 4 -->
<string name="ganti_kode_akses_buat_baru">Buat kode akses baru Anda</string>
<string name="ganti_kode_akses_baru_hint">Kode akses terdiri dari 6 digit angka</string>
<string name="ganti_kode_akses_rule_berurut">Tidak boleh angka berurut (contoh: 123456)</string>
<string name="ganti_kode_akses_rule_sama">Tidak boleh angka sama semua (contoh: 111111)</string>
<string name="ganti_kode_akses_rule_beda_lama">Tidak boleh sama dengan kode akses lama</string>

<!-- Step 5 -->
<string name="ganti_kode_akses_konfirmasi">Ulangi kode akses baru Anda</string>
<string name="ganti_kode_akses_error_tidak_cocok">Kode akses tidak cocok. Silakan ulangi.</string>

<!-- Step 6 & 7 -->
<string name="ganti_kode_akses_proses">Sedang mengubah kode akses Anda...</string>
<string name="ganti_kode_akses_berhasil_title">Kode Akses Berhasil Diubah</string>
<string name="ganti_kode_akses_berhasil_desc">Silakan login kembali menggunakan kode akses baru Anda.</string>
<string name="ganti_kode_akses_keamanan">Jangan bagikan kode akses Anda kepada siapapun.</string>
<string name="ganti_kode_akses_login_sekarang">Login Sekarang</string>
<string name="ganti_kode_akses_lanjut">Lanjut</string>

<!-- Error -->
<string name="ganti_kode_akses_akun_diblokir">Akun Anda diblokir sementara. Hubungi Halo BCA 1500888.</string>
<string name="cd_ganti_kode_akses_back">Kembali</string>
```

## B.7 Pola yang Bisa Di-reuse

| Komponen | Sumber | Lokasi |
|---|---|---|
| Numpad + dot indicator | `KodeAksesScreen.kt` | `ui/screen/kode_akses/` |
| Step indicator | `TransferAntarRekeningScreen.kt` | `ui/screen/transfer/` |
| Error banner | `LoginScreen.kt` -> `ErrorBanner` | `ui/screen/login/` |
| Surface button | `LoginScreen.kt` -> `SecondaryActionButton` | `ui/screen/login/` |

## B.8 File yang Perlu Dibuat

| File | Tujuan |
|---|---|
| `ui/screen/ganti_kode_akses/GantiKodeAksesScreen.kt` | Composable utama (semua step) |
| `ui/screen/ganti_kode_akses/GantiKodeAksesViewModel.kt` | State, validasi, OTP timer |

---

# C. Aturan Visual (Berlaku untuk Kedua Flow)

| Aspek | Aturan | Referensi |
|---|---|---|
| Warna | Token `AppColor.*` dan `M3Color.*` | `ui/theme/Color.kt` |
| Spacing | Token `Spacing.s*` | `ui/theme/Dimens.kt` |
| Shape | Token `AppShape.*` | `ui/theme/Shape.kt` |
| Tipografi | `MaterialTheme.typography.*` atau `AppTypography.*` | `ui/theme/Type.kt` |
| Stroke | Token `StrokeWidth.*` | `ui/theme/Dimens.kt` |
| Alpha | Token `AppAlpha.*` | `ui/theme/Color.kt` |
| Ukuran | Token `AppSize.*` | `ui/theme/Dimens.kt` |

**Token tidak ada? STOP dan lapor.** Jangan buat token baru sendiri.

---

# D. Catatan Navigasi (Berlaku untuk Kedua Flow)

- Kedua flow berada di `Graph.Auth` — diakses **sebelum** login
- Back button di step 1 kembali ke Login (`navController.popBackStack()`)
- Back button di step 2+ mundur satu step (dikelola oleh ViewModel, bukan popBackStack)
- Setelah selesai, kembali ke LoginScreen
- Per `navigation.md` section 3: `Auth.BukaRekening` dan `Auth.GantiKodeAkses`
- Menambah route baru wajib update `navigation.md` di commit yang sama

---

# E. Urutan Pengerjaan (Saran)

### Fase 1 — Persiapan (shared)

1. Review token yang tersedia di `ui/theme/`
2. Jika token kurang -> STOP, lapor, tunggu keputusan
3. Tentukan pendekatan arsitektur Buka Rekening (opsi A vs B di A.4)

### Fase 2 — Ganti Kode Akses (dikerjakan duluan, lebih kecil scope-nya)

4. Tambah string resources ke `strings.xml`
5. Buat `GantiKodeAksesScreen.kt` dengan UiState dan semua step
6. Buat `GantiKodeAksesViewModel.kt` (state, validasi, OTP timer)
7. Ganti placeholder di `AuthGraph.kt`
8. Buat preview per step (min 7: satu per step + error)
9. Validasi: `check-hardcoded-ui.sh` + `assembleDebug`

### Fase 3 — Buka Rekening (lebih besar, multi-screen)

10. Tambah string resources ke `strings.xml`
11. Tambah route baru ke `Route.kt`
12. Buat `BukaRekeningScreen.kt` (form steps 1-5, 9-12)
13. Buat `BukaRekeningViewModel.kt`
14. Buat `BukaRekeningKtpScreen.kt` (kamera KTP)
15. Buat `BukaRekeningSelfieScreen.kt` (selfie + liveness)
16. Buat `BukaRekeningVideoCallScreen.kt` (video call eKYC)
17. Buat `BukaRekeningBerhasilScreen.kt` (success)
18. Update `AuthGraph.kt` dengan semua route baru
19. Update `navigation.md`
20. Buat preview per screen dan per step
21. Validasi: `check-hardcoded-ui.sh` + `assembleDebug`

### Fase 4 — Finalisasi

22. `./gradlew testDebugUnitTest` — test existing tidak rusak
23. `./gradlew lintDebug` — tidak ada warning baru
24. Review manual: navigasi back tiap step berfungsi benar
25. Update `docs/design/missing-screens-audit.md` — status A3 & A4

---

# F. Constraint & Batasan

- **Tidak ada dependency baru** tanpa persetujuan (per CLAUDE.md)
- **Tidak mengubah `build.gradle.kts`** tanpa diminta
- **Composable UI stateless** — tanpa ViewModel, repository, atau network call di dalamnya
- **String ke `strings.xml`** — additive only
- **Token tidak ada = STOP**
- **Jangan commit/push** kecuali diminta
- Perubahan navigasi mengikuti `navigation.md`, update di commit yang sama
- Fitur kamera, video call, dan OTP memerlukan izin runtime — handle gracefully
- Data sensitif (NIK, PIN, kode akses) **tidak boleh** disimpan di `SavedStateHandle`
  (per `navigation.md` section 11)

---

# G. Referensi Desain Visual

| File | Isi Relevan |
|---|---|
| `docs/design/design_view_bca.webp` | Visual design — pola UI halaman auth, Kode Akses screen |
| `docs/design/workflow_bca.webp` | Task flow login (kode akses 6 digit), e-wallet, mutasi |
| `docs/design/workflow_2_bca.webp` | Task mapping (steps, emotions, design opportunity) |
| `docs/design/hivi_bca.webp` | Hi-fi wireframe, typography (Inter), color palette, spacing, radius |
| `docs/design/bca_ui_description.webp` | Problem statement, target audience 18-60, possible solution |
| `docs/design/bca_solving_problem.webp` | Key insight: limited login options, outdated UI |
| `docs/design/bca_ui_statement.webp` | Design thinking process, project timeline |
| `docs/design/key_inside_drive.webp` | User research, key insight derived |

---

# H. Kriteria Selesai (Definition of Done)

## Ganti Kode Akses
- [ ] `GantiKodeAksesScreen.kt` — composable stateless, 7 step lengkap
- [ ] `GantiKodeAksesViewModel.kt` — state, validasi, OTP countdown timer
- [ ] Step 1: Input nomor kartu (16 digit, format 4-4-4-4)
- [ ] Step 2: OTP verifikasi (6 digit, timer countdown, kirim ulang)
- [ ] Step 3: Input kode lama + alternatif PIN ATM (lupa kode akses)
- [ ] Step 4: Buat kode baru (validasi realtime: berurut, sama, beda lama)
- [ ] Step 5: Konfirmasi kode baru
- [ ] Step 6: Loading state
- [ ] Step 7: Success screen
- [ ] Error handling: max attempts, akun diblokir, koneksi gagal
- [ ] Preview per step (min 7)
- [ ] Placeholder di `AuthGraph.kt` diganti

## Buka Rekening
- [ ] `BukaRekeningScreen.kt` — composable utama (form steps)
- [ ] `BukaRekeningViewModel.kt` — state & validasi per step
- [ ] Step 1: Pilih jenis rekening (3 opsi card)
- [ ] Step 2: S&K (scroll + checkbox)
- [ ] Step 3-5: Form data pribadi (validasi per field)
- [ ] `BukaRekeningKtpScreen.kt` — kamera + preview KTP
- [ ] `BukaRekeningSelfieScreen.kt` — selfie + liveness detection
- [ ] `BukaRekeningVideoCallScreen.kt` — antrian + video call eKYC
- [ ] Step 9: Buat kode akses + PIN (validasi)
- [ ] Step 10: Pilih desain kartu (kondisional Xpresi)
- [ ] Step 11-12: Setoran awal + ringkasan
- [ ] `BukaRekeningBerhasilScreen.kt` — success + nomor rekening
- [ ] Route baru di `Route.kt`
- [ ] `navigation.md` diupdate

## Shared
- [ ] String resources ditambahkan ke `strings.xml`
- [ ] `check-hardcoded-ui.sh` — PASS
- [ ] `assembleDebug` — PASS
- [ ] `testDebugUnitTest` — PASS
- [ ] `lintDebug` — PASS
- [ ] `docs/design/missing-screens-audit.md` — A3 & A4 diupdate