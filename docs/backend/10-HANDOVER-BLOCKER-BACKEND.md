# Handover — Yang Ditunggu dari Backend

> Daftar hal yang menghentikan pekerjaan client Android. Setiap butir punya:
> apa yang dibutuhkan, bentuk persisnya, apa yang macet tanpa itu, dan cara
> memastikan sudah benar.
>
> Urut dari yang paling memblokir. Butir 1 dan 2 menahan fitur yang kodenya
> **sudah selesai ditulis** dan hanya menunggu bahan dari backend.

---

## Ringkasan

| # | Butir | Menahan apa | Pemilik |
|---|---|---|---|
| 1 | Kunci publik PIN (`pin_public.pem`) | Login kode akses, seluruh transaksi finansial | Backend / Security |
| 2 | Algoritma tanda tangan biometrik | Login Face ID dan Touch ID | Backend / Security |
| 3 | Pengikatan `X-Device-ID` + keputusan §0 | Keamanan sesi onboarding | Backend |
| 4 | Biaya dan limit resmi kartu Paspor | Layar pilih kartu | Product Owner |
| 5 | Hash SPKI certificate pinning | Rilis produksi | Infrastruktur |
| 6 | Nilai `period` mutasi yang diterima | Filter Mutasi selain 7 hari | Backend |
| 7 | TURN/STUN + izin dependency WebRTC | Video call e-KYC | Backend + Engineering Manager |
| 8 | Konflik `/registration/*` vs `/onboarding/*` | Kejelasan kontrak jangka panjang | Backend / Arsitek |

---

## 1. Kunci publik PIN — `assets/pin_public.pem`

**Ini blocker paling mahal.** Kodenya sudah jadi; yang hilang hanya berkas kuncinya.

### Yang dibutuhkan

Kunci publik RSA-2048 dalam format PEM X.509 (`SubjectPublicKeyInfo`):

```
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
-----END PUBLIC KEY-----
```

Pasangan kuncinya dibuat di sisi server; **privat tidak pernah meninggalkan backend**.

```bash
# Backend menjalankan ini, lalu mengirim HANYA berkas .pub
openssl genrsa -out pin_private.pem 2048
openssl rsa -in pin_private.pem -pubout -out pin_public.pem
```

### Kenapa memblokir

`PinEncryptor` membaca `assets/pin_public.pem`. Berkas itu belum ada, jadi
`encrypt()` mengembalikan null dan repository menolak lebih awal dengan
`CLIENT_PIN_KEY_MISSING`. Ini disengaja — PIN tidak boleh dikirim apa adanya.

Yang ikut terhenti: `POST /auth/login/pin`, `POST /auth/pin/verify`,
`POST /auth/pin/change`. Artinya **login, transfer, dan top up e-wallet tidak bisa
diuji ujung ke ujung** meski layar dan repositorinya sudah selesai.

### Pertanyaan yang perlu dijawab

1. **Padding.** Client memakai `RSA/ECB/OAEPWithSHA-256AndMGF1Padding`
   (RSA-OAEP-SHA256), sama dengan onboarding. Pastikan dekripsi server memakai
   OAEP-SHA256, **bukan** PKCS#1 v1.5. Salah padding = `CRED_DECRYPTION_FAILED`
   yang sulit dilacak karena gejalanya "PIN selalu salah".
2. **Rotasi kunci.** Kunci di `assets/` berarti setiap rotasi butuh rilis APK baru.
   Onboarding sudah menghindari ini lewat `GET /onboarding/credentials/public-key`.
   **Rekomendasi kami:** sediakan juga `GET /auth/pin/public-key` dengan bentuk
   respons yang sama (`algorithm`, `key_id`, `public_key_pem`), lalu berkas di
   `assets/` dipakai hanya sebagai cadangan saat endpoint tidak terjangkau.
   Kalau disetujui, client akan mengirim balik `encryption_key_id` seperti di
   onboarding.
3. **Apakah kunci PIN sama dengan kunci kredensial onboarding?** Kalau sama, satu
   endpoint cukup dan butir 2 di atas jadi lebih sederhana.

### Cara mengirim

Jangan lewat chat, email, atau tiket publik. Walaupun ini kunci **publik**,
ketertelusuran versinya penting: taruh di secret manager atau repo artefak
internal, beri nama bersama `key_id`-nya, mis. `pin-key-v1`.

### Selesai bila

- [ ] `assets/pin_public.pem` ada di repo Android
- [ ] Enkripsi client dengan kunci itu berhasil didekripsi server (uji satu PIN dummy)
- [ ] Server menolak ciphertext PKCS#1 v1.5 — memastikan padding benar-benar OAEP
- [ ] `key_id` tercatat, dan cara rotasinya disepakati

---

## 2. Algoritma tanda tangan biometrik

### Yang dibutuhkan

`docs/backend/01-API-SPECIFICATION.md` §2 mendefinisikan alur biometrik:

```
GET  /auth/biometric/challenge?device_id=...   → { challenge_id, challenge }
POST /auth/biometric/register                  ← { public_key, key_id, attestation }
POST /auth/login/biometric                     ← { challenge_id, signed_challenge, key_id }
```

Tetapi **tidak menyebut algoritma apa pun**. Client tidak bisa menandatangani
tanpa jawaban lima hal ini:

| Pertanyaan | Pilihan | Rekomendasi kami |
|---|---|---|
| Jenis kunci | EC P-256 atau RSA-2048 | **EC P-256** — lebih cepat, kunci lebih kecil, didukung semua perangkat minSdk 26 |
| Algoritma tanda tangan | `SHA256withECDSA` atau `SHA256withRSA/PSS` | `SHA256withECDSA` |
| Format `signed_challenge` | Base64 dari DER, atau raw r‖s | **Base64 dari DER** (keluaran bawaan `java.security.Signature`) |
| Format `public_key` | Base64 X.509 `SubjectPublicKeyInfo`, atau PEM | Base64 X.509 tanpa header PEM |
| Yang ditandatangani | `challenge` mentah, atau gabungan `challenge‖device_id` | **`challenge` apa adanya**, di-decode dari Base64 lebih dulu — kalau perlu pengikatan tambahan, sebut eksplisit |

### Attestation

Field `attestation` di `/auth/biometric/register` diisi rantai sertifikat Android
Key Attestation (`KeyStore.getCertificateChain(alias)`). Yang perlu diputuskan:

1. Apakah server **memverifikasi** rantai itu ke akar Google, atau hanya menyimpannya?
2. Kalau memverifikasi: apakah perangkat tanpa dukungan attestation (emulator,
   perangkat lama) ditolak, atau diizinkan dengan penanda risiko?
3. Apakah `security_level` minimal ditetapkan — `TEE` saja, atau `StrongBox` wajib
   untuk nasabah tertentu?

Jawaban nomor 3 memengaruhi perangkat mana yang bisa memakai login biometrik.

### Perilaku yang perlu disepakati

- **Kunci hangus saat sidik jari baru didaftarkan.** Client memakai
  `setInvalidatedByBiometricEnrollment(true)`, jadi menambah sidik jari membuat
  kunci lama tidak bisa dipakai. Client akan menghapus kunci dan memanggil
  `/auth/biometric/register` lagi. Server perlu memutuskan: apakah pendaftaran
  ulang **mengganti** kunci lama untuk `device_id` itu, atau menambah kunci baru?
  Kalau mengganti, kunci lama harus dicabut.
- **Masa berlaku challenge.** Spec menyebut 60 detik sekali pakai. Pastikan
  server benar-benar menolak challenge yang sudah dipakai — bukan hanya yang
  kedaluwarsa.
- **Beberapa perangkat per nasabah.** Apakah satu nasabah boleh mendaftarkan
  biometrik di lebih dari satu perangkat?

### Selesai bila

- [ ] Kelima baris tabel di atas terjawab dan masuk ke `01-API-SPECIFICATION.md` §2
- [ ] Kebijakan attestation ditulis (verifikasi atau tidak, level minimal)
- [ ] Ada satu pasangan uji: challenge contoh + tanda tangan valid, supaya client
      bisa memverifikasi implementasinya tanpa menunggu server
- [ ] Perilaku pendaftaran ulang setelah kunci hangus disepakati

Detail sisi Android ada di skill `android-biometric-keystore`.

---

## 3. Pengikatan perangkat dan keputusan terbuka onboarding

`docs/backend/06-BUKA-REKENING-API-SPEC.md` §0 memuat empat keputusan terbuka
yang belum dijawab. Client **sudah** mengirim `X-Device-ID` dan `X-Request-ID` di
semua request, jadi bagian client selesai — yang ditunggu sisi server:

1. Verifikasi `X-Device-ID` terhadap `device_id` yang tersimpan saat sesi dibuat,
   dan balas `403 ONBOARDING_DEVICE_MISMATCH` bila berbeda.
2. Keputusan soal sesi setelah aplikasi dipasang ulang — `ANDROID_ID` berubah,
   sehingga draf lama tidak bisa dilanjutkan. Perlu ditegaskan apakah itu memang
   yang diinginkan.
3. Apakah `X-Device-ID` juga wajib untuk `GET /products/{type}/cards`.
4. `X-Request-ID` dipantulkan di `meta.request_id` atau server membuat sendiri.

### Selesai bila

- [ ] Server menolak request dengan device id berbeda dari pemilik sesi
- [ ] Empat keputusan di §0 terjawab dan §0 diperbarui

---

## 4. Biaya dan limit resmi kartu Paspor

`docs/backend/09-PILIH-KARTU-SKILL-PROMPTS.md` Bagian 2 memuat sembilan
pertanyaan. Dua yang paling menahan:

- Biaya administrasi, penerbitan, dan penggantian per kartu
- Empat limit per kartu (tarik tunai, transfer BCA, antar bank, debit)

Angka yang ada di `strings.xml` client (Rp14.000 / 16.000 / 19.000) adalah **data
desain**, bukan tarif resmi. Jangan disalin ke database produksi.

### Selesai bila

- [ ] Angka resmi diterima tertulis dari Product Owner
- [ ] Tabel `card_products` terisi angka itu di staging

---

## 5. Hash SPKI untuk certificate pinning

`NetworkModule.certificatePinner()` sudah siap tetapi daftar pinnya kosong,
sehingga pinning tidak aktif walau `CERTIFICATE_PINNING_ENABLED = true` di release.

### Yang dibutuhkan

Hash SPKI base64 untuk `api.bcamobile.id` — **sertifikat aktif dan minimal satu
cadangan**. Tanpa cadangan, perpanjangan sertifikat akan mematikan aplikasi di
lapangan sampai ada rilis baru.

```bash
openssl s_client -servername api.bcamobile.id -connect api.bcamobile.id:443 \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64
```

### Selesai bila

- [ ] Minimal dua hash (aktif + cadangan) diterima beserta tanggal kedaluwarsanya
- [ ] Prosedur rotasi disepakati: siapa memberi tahu, berapa lama sebelum ganti

---

## 6. Nilai `period` mutasi yang diterima

`GET /transactions/mutations` di spec hanya mencontohkan `LAST_7_DAYS` dan
`CUSTOM`. Layar Mutasi punya empat pilihan: 7 hari, bulan ini, bulan lalu, dan
rentang khusus. Client saat ini mengirim `LAST_7_DAYS`, `THIS_MONTH`,
`LAST_MONTH`, `CUSTOM` apa adanya.

### Selesai bila

- [ ] Daftar lengkap nilai `period` yang diterima ditulis di spec
- [ ] Bila `LAST_MONTH` tidak didukung, sebutkan penggantinya

---

## 7. Video call e-KYC

Dua hal tertahan sekaligus:

- **Kredensial TURN/STUN** dan masa berlakunya. Tanpa TURN, panggilan gagal di
  jaringan seluler ber-NAT ketat — yaitu mayoritas nasabah.
- **Izin menambah dependency WebRTC** di sisi Android. Ini keputusan Engineering
  Manager, bukan backend, tetapi menahan pekerjaan yang sama.

Protokol signaling sudah lengkap di `06-BUKA-REKENING-API-SPEC.md` §5b. Yang
belum: ketentuan token `signaling_url` (§0 sudah mengusulkan maksimal 5 menit,
sekali pakai) perlu dikonfirmasi sudah diterapkan.

### Selesai bila

- [ ] Kredensial TURN untuk staging diterima
- [ ] Ketentuan masa berlaku token signaling dikonfirmasi
- [ ] Dependency WebRTC disetujui

---

## 8. Dua keluarga endpoint untuk buka rekening

`01-API-SPECIFICATION.md` §9 mendefinisikan `/v1/registration/*` dengan
**Registration Token**, sementara `06-BUKA-REKENING-API-SPEC.md` mendefinisikan
`/v1/onboarding/*` dengan `session_id`. Keduanya menggambarkan fitur yang sama.

Client Android mengimplementasikan yang kedua. Ini bukan blocker harian — kode
berjalan — tetapi membiarkannya berarti dua kontrak yang bisa berbeda diam-diam.

### Selesai bila

- [ ] Satu keluarga dinyatakan berlaku
- [ ] Yang tidak dipakai ditandai usang di dokumennya, bukan dihapus diam-diam

---

## Cara memakai dokumen ini

Butir 1 dan 2 bisa diselesaikan tanpa menunggu yang lain, dan keduanya membuka
fitur yang kodenya sudah jadi. Kalau kapasitas backend terbatas minggu ini,
kerjakan dua itu saja.

Setiap butir yang selesai: perbarui tabel Ringkasan di atas, lalu perbarui juga
tabel status di `CLAUDE.md` dan peta workflow di
`docs/buka-rekening-android-prompts.md` supaya sesi berikutnya tidak mengulang
pertanyaan yang sudah terjawab.