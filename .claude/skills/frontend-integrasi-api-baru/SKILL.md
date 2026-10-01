---
name: frontend-integrasi-api-baru
description: Integrasi sisi client (Android `bca_mobile`) untuk API yang SUDAH SIAP di backend tapi BELUM dipanggil aplikasi — kartu nasabah di Profil Saya (`GET /account/cards`, sakelar kanal, blokir, ganti kartu), field `tier`, Pusat Bantuan & Kontak CS, filter `period` di Riwayat, filter `type` di Notifikasi, rotasi kunci PIN (`encryption_key_id`), kontrak tanda tangan biometrik EC P-256, pilih kartu Paspor saat buka rekening, `ice_servers` video call, dan `403 ONBOARDING_DEVICE_MISMATCH`. Gunakan saat diminta "integrasikan API yang belum dipakai", "endpoint backend baru apa saja", "layar Rekening & Kartu masih dummy", "sambungkan Pusat Bantuan", "filter Riwayat per periode", "tab Notifikasi belum menyaring", "kirim encryption_key_id", atau "kerjakan sisa integrasi". Trigger juga pada "masked_number", "blokir kartu", "ganti kartu", "verification_token", "X-Idempotency-Key", "tier Prioritas", "help-center", "contact-cs", "AUTH_PIN_KEY_UNKNOWN", "signed_challenge", "ice_servers", "CARD_SELECTION". JANGAN gunakan untuk implementasi backend (itu `bca-mobile-backend`, `profil-saya-kartu-api`, `buka-rekening-backend`), untuk layar OTP buka rekening (itu `frontend-otp-verification`), untuk visual/token desain (`stitch-to-compose`), atau untuk navigasi/ViewModel (`compose-architecture`).
---

# Integrasi API yang Belum Dipakai Client

Daftar kerja untuk repo Android `bca_mobile`. Semua endpoint di bawah **sudah
berjalan di backend** dan bisa dipanggil hari ini — yang belum ada adalah
pemanggilnya di aplikasi.

Audit yang menghasilkan daftar ini dijalankan pada **2026-09-26** terhadap
`internal/router/router.go` (72 route terdaftar) dan dibandingkan dengan koleksi
Postman serta `docs/01-API-SPECIFICATION.md`.

---

## ATURAN #0 — Bentuk DTO diturunkan dari handler, bukan dari spec

Setiap potongan JSON di dokumen ini dibaca langsung dari struct Go di
`internal/domain/*/entity.go` dan `internal/handler/*.go`, bukan dari contoh di
`docs/01-API-SPECIFICATION.md`.

Alasannya bukan formalitas. Enam cacat kontrak yang sudah pernah menggigit
proyek ini — sakelar yang membalik sendiri, ID kosong di Profil Saya, rentang
kustom Mutasi yang mengembalikan semua data — **tiga di antaranya lahir dari
menyalin contoh di spec yang berbeda dari kode yang berjalan.** Kalau ada
keraguan antara dokumen ini dan spec, yang menang adalah handler; laporkan
selisihnya, jangan pilih salah satu diam-diam.

## ATURAN #1 — Envelope, bukan objek telanjang

Semua respons dibungkus. Jangan memetakan `data` langsung ke DTO layar tanpa
lapisan envelope:

```json
{
  "status": "success",
  "data": { },
  "pagination": { "cursor": "", "has_more": false, "limit": 20 },
  "meta": { "request_id": "req_...", "timestamp": "2026-09-26T10:00:00Z" }
}
```

Error memakai kunci `error`, dan `details` boleh berisi apa pun:

```json
{
  "status": "error",
  "error": { "code": "VALIDATION_ERROR", "message": "…", "details": { } },
  "meta": { }
}
```

`pagination` ada **di luar** `data`. `meta.catalog_outdated` bisa muncul di
respons endpoint apa pun (konsekuensi spec menempatkannya di meta) — abaikan
kalau bukan sedang di layar pilih kartu.

## ATURAN #2 — Urutan pengerjaan

Kerjakan dari atas. Urutannya dipilih berdasarkan hasil per satuan risiko, bukan
kemudahan:

1. **§1 Rekening & Kartu** — tujuh bagian layar yang sekarang menampilkan
   penanda kosong. Hasil terbesar, tidak menyentuh jalur uang.
2. **§2 Dua filter** — dua perubahan kecil yang menghapus bug yang terlihat
   seperti fitur ("Riwayat menampilkan semua", "tab Notifikasi tidak menyaring").
3. **§3 Kunci & biometrik** — menutup blocker login. Butuh satu berkas dari
   backend (lihat §3.0).
4. **§4 Buka rekening** — pilih kartu dan video call.
5. **§5 Sudah ada, belum dipakai** — kerjakan kalau layarnya sudah ada.

## ATURAN #3 — Kapan berhenti

Kalau layarnya belum ada, **jangan merancang layar sendiri.** Ada di Stitch →
buat layar dulu lewat skill `stitch-to-compose`, integrasi di sesi terpisah.
Tidak ada di Stitch → **STOP dan lapor.** Endpoint tanpa desain bukan izin
berimprovisasi.

---

## 1. Rekening & Kartu + Profil Saya

Tujuh kebutuhan `AkunScreen.kt` yang sebelumnya tidak punya endpoint. Backend
menutupnya pada 2026-09-25 (migrasi `000021`–`000024`).

### 1.1 `GET /v1/account/cards`

**Auth:** Bearer. Tidak ada parameter apa pun — user id diambil dari access
token, jadi tidak ada cara meminta kartu orang lain.

```json
// 200
{ "data": { "cards": [
  {
    "card_id": "9f1c…",
    "masked_number": "5221 •••• •••• 1234",
    "cardholder_name": "NURHOLIS MAJID",
    "card_type": "PASPOR_GOLD",
    "product_name": "Paspor BCA Gold",
    "network": "GPN",
    "tier_key": "GOLD",
    "style": "GOLD",
    "valid_thru": "12/28",
    "status": "ACTIVE",
    "is_primary": true,
    "settings": { "debit_online_enabled": true, "international_enabled": false },
    "blocked_reason": null
  }
] } }
```

Yang mengikat:

- **`status`**: `ACTIVE` · `BLOCKED` · `EXPIRED` · `REPLACEMENT_PENDING`.
  `EXPIRED` dihitung server dari `valid_thru` di zona WIB — jangan menghitung
  ulang di client, dua perhitungan yang beda zona akan berselisih satu hari.
- **`blocked_reason`** hanya terkirim saat `status == "BLOCKED"`
  (`LOST` · `STOLEN` · `DAMAGED` · `SUSPECTED_FRAUD`); selain itu field-nya
  absen. Modelkan nullable.
- **`style`** (`BLUE`/`GOLD`/`PLATINUM`) dipetakan client ke design token.
  Server **tidak** mengirim hex warna atau URL gambar, dan tidak akan.
- **`masked_number`** adalah satu-satunya bentuk nomor kartu yang ada. PAN utuh
  tidak pernah disimpan backend, jadi jangan buat layar yang menjanjikan
  "lihat nomor lengkap".
- **Nasabah tanpa kartu dijawab `200` dengan array kosong, bukan `404`.** Layar
  membedakan "belum punya kartu" dari "ada yang rusak"; hanya yang pertama punya
  empty state.

### 1.2 `PUT /v1/account/cards/{card_id}/settings`

Dua sakelar yang sekarang terpasang di UI tapi tidak bergerak.

```json
// Request — keduanya opsional, yang absen tidak diubah
{ "debit_online_enabled": true, "international_enabled": false }

// 200 — kartu sesudah perubahan
{ "data": { "card": { /* bentuk sama dengan §1.1 */ } } }
```

Kirim **hanya sakelar yang diubah**. Body kosong (kedua field absen) dijawab
`400 VALIDATION_ERROR`. Repaint dari respons, jangan panggil ulang `GET /account/cards`.

**Tidak** butuh `verification_token`: ini preferensi, bukan aksi keamanan.

### 1.3 `POST /v1/account/cards/{card_id}/block`

```json
// Request
{ "reason": "LOST", "verification_token": "vt_…" }

// 200
{ "data": { "card": { /* status kini BLOCKED, blocked_reason terisi */ } } }
```

`reason`: `LOST` · `STOLEN` · `DAMAGED` · `SUSPECTED_FRAUD`.

**`verification_token` wajib.** Alurnya dua langkah, sama seperti Atur Limit:

```
POST /v1/auth/pin/verify  { "pin_encrypted": "…", "purpose": "BLOCK_CARD" }
  → { "verification_token": "vt_…", "expires_in": 120 }
POST /v1/account/cards/{card_id}/block  { "reason": "LOST", "verification_token": "vt_…" }
```

Token berumur **120 detik dan sekali pakai**. Kalau nasabah termenung di dialog
konfirmasi lebih lama dari itu, minta PIN lagi — jangan cache token.

### 1.4 `POST /v1/account/cards/{card_id}/replacement`

**Berbiaya.** Perlakukan seperti transaksi uang.

```
Header: X-Idempotency-Key: <UUID v4, dibuat sekali per niat pengguna>
```

```json
// Request
{ "reason": "DAMAGED", "delivery_method": "COURIER", "verification_token": "vt_…" }

// 201 Created
{ "data": {
  "request_id": "b71e…",
  "card_id": "9f1c…",
  "status": "REQUESTED",
  "reason": "DAMAGED",
  "delivery_method": "COURIER",
  "fee": 25000,
  "estimated_arrival_from": "2026-10-01",
  "estimated_arrival_to": "2026-10-05",
  "masked_number": "5221 •••• •••• 1234"
} }
```

- `reason`: `DAMAGED` · `LOST` · `UPGRADE`. `delivery_method`: `COURIER` ·
  `BRANCH_PICKUP` (yang kedua tidak selalu tersedia → `CARD_DELIVERY_UNAVAILABLE`).
- `fee` **integer rupiah**, bukan string desimal — beda dari `amount` di
  transaksi. Angkanya dibekukan saat permintaan dibuat; tarif katalog yang
  berubah nanti tidak mengubah angka di struk yang sudah terbit.
- **`purpose` untuk token di sini `REPLACE_CARD`**, bukan `BLOCK_CARD`.
- **Ulangi dengan kunci yang SAMA.** Retry dijawab `200` (bukan `201`) dengan
  header `X-Idempotent-Replayed: true` dan body identik. Kunci **baru** untuk
  niat yang sama dijawab `409 CARD_REPLACEMENT_IN_PROGRESS` — itu penjaga kedua
  supaya nasabah tidak dibebani dua kartu berbayar. Jangan menerjemahkan 409 itu
  sebagai kegagalan: permintaan sebelumnya berhasil, arahkan ke statusnya.
- Simpan kunci idempotensi bersama state layar, bukan di variabel lokal
  ViewModel — rotasi layar tidak boleh melahirkan kunci baru.

### 1.5 Error kartu → perilaku UI

| HTTP | Code | Yang harus dilakukan layar |
|---|---|---|
| 404 | `CARD_NOT_FOUND` | Kartu tidak ada **atau bukan milik nasabah** — jawabannya sengaja sama. Muat ulang daftar. |
| 409 | `CARD_BLOCKED` | Kartu sudah diblokir. Sembunyikan aksi, tawarkan Halo BCA. |
| 409 | `CARD_REPLACEMENT_IN_PROGRESS` | Tampilkan status permintaan berjalan, jangan kirim ulang. |
| 422 | `CARD_DELIVERY_UNAVAILABLE` | Sembunyikan `BRANCH_PICKUP` untuk kartu ini. |
| 401 | `AUTH_TOKEN_INVALID` | Token verifikasi kedaluwarsa/terpakai → minta PIN lagi. |

### 1.6 Field `tier` di `GET /v1/account/profile`

**Bukan endpoint baru.** Tidak ada `GET /account/tier` dan tidak akan ada.

```json
{ "data": { "id": "…", "full_name": "…", "display_name": "…",
            "phone": "0812****5678", "email": "n***@gmail.com",
            "tier": "PRIORITAS", "last_login_at": "…", "accounts": [] } }
```

`tier`: `PRIORITAS` · `SOLITAIRE`. **Nasabah `REGULER` tidak mengirim field ini
sama sekali** — badge disembunyikan saat field absen. Modelkan `String?` dan
jangan default ke `"REGULER"` lalu membandingkannya; absen = tanpa badge.

### 1.7 `GET /v1/content/help-center` dan `GET /v1/content/contact-cs`

**Tanpa `Authorization`, sengaja.** Nasabah yang terkunci di luar aplikasi
justru yang paling butuh nomor Halo BCA. Jangan pasang interceptor auth di dua
panggilan ini, dan jangan letakkan di belakang gerbang login.

```json
// GET /v1/content/help-center → 200
{ "data": { "categories": [
  { "key": "TRANSFER", "title": "Transfer & Pembayaran",
    "items": [ { "question": "Kenapa transfer saya gagal?", "answer": "…" } ] }
] } }

// GET /v1/content/contact-cs → 200
{ "data": {
  "phone": "1500888", "phone_free": "+62 21 23588000",
  "whatsapp": "+62 811 1500 998", "email": "halobca@bca.co.id",
  "chat_url": "https://…", "hours": "24 jam setiap hari"
} }
```

`key` dipetakan ke ikon; **`title` ikut dikirim supaya key baru yang belum
dikenal client tetap punya teks untuk dicetak** — jatuhkan ke `title`, jangan
cetak raw key dan jangan sembunyikan kategorinya. Kedua respons di-cache server
24 jam; client tidak perlu cache tambahan selain untuk mode offline.

---

## 2. Dua filter yang baru ditambahkan (2026-09-26)

### 2.1 `period` di `GET /v1/transactions/history`

Sebelumnya Riwayat tidak punya filter tanggal sama sekali, jadi layar Rentang
Waktu hanya menyaring yang sudah termuat. Sekarang kosakatanya **sama persis**
dengan `GET /transactions/mutations`:

```
GET /v1/transactions/history?period=LAST_30_DAYS&type=&limit=20&cursor=
GET /v1/transactions/history?period=CUSTOM&from=2026-09-01&to=2026-09-26
```

| `period` | Rentang (tanggal WIB, kedua ujung inklusif) |
|---|---|
| `LAST_7_DAYS` | 6 hari lalu … hari ini |
| `LAST_30_DAYS` | 29 hari lalu … hari ini |
| `LAST_90_DAYS` | 89 hari lalu … hari ini |
| `THIS_MONTH` | tanggal 1 bulan ini … hari ini |
| `LAST_MONTH` | tanggal 1 bulan lalu … hari terakhir bulan lalu |
| `CUSTOM` | `from` … `to`, wajib keduanya, `YYYY-MM-DD` |

- **Pakai satu jalur kode dengan Mutasi.** Dua layar yang sama-sama menampilkan
  "7 hari terakhir" tapi berbeda cara memintanya adalah asal cacat #3 dulu.
- `start_date`/`end_date` diterima sebagai nama lain dari `from`/`to`. Pilih
  `from`/`to`; jangan kirim dua-duanya.
- **Nilai tak dikenal ditolak `400 VALIDATION_ERROR`** dengan
  `details.allowed_values`. Dulu salah tulis lolos sebagai "tanpa filter" dan
  layar menampilkan seluruh riwayat seolah-olah itu 7 hari terakhir. Kalau
  error ini muncul, itu bug client — jangan ditelan jadi empty state.
- Hari terakhir rentang ikut penuh: transaksi pukul 23.59 WIB hari ini tetap
  masuk `LAST_7_DAYS`.

### 2.2 `type` di `GET /v1/notifications`

```
GET /v1/notifications?type=PROMO&limit=20
GET /v1/notifications?type=PROMO,SECURITY
```

Nilai: `TRANSACTION` · `PROMO` · `SECURITY` · `SYSTEM` · `INFO`. Boleh beberapa
dipisah koma. **Tab "Semua" tidak mengirim parameter `type` sama sekali** —
jangan kirim `type=ALL`, itu bukan nilai yang dikenal dan akan dijawab
`400 VALIDATION_ERROR` dengan `details.allowed_values`.

Urutan nilai tidak berpengaruh (`PROMO,INFO` = `INFO,PROMO`). Penyaringan
sekarang terjadi di server, jadi **paginasi per tab akhirnya benar**: buang
logika filter sisi client dan reset cursor saat tab berganti.

---

## 3. Kunci PIN dan biometrik

### 3.0 Prasyarat yang masih menunggu manusia

`PinEncryptor` membaca `assets/pin_public.pem`. Berkas itu **belum ada di repo
Android**, jadi `encrypt()` mengembalikan null dan repository menolak lebih awal
dengan `CLIENT_PIN_KEY_MISSING`. Selama itu, login kode akses dan seluruh
transaksi finansial tidak bisa diuji ujung ke ujung.

Backend sudah menyediakan dua jalan: berkas (`make pin-public-key` di repo
backend) dan endpoint di §3.1. **Kerjakan §3.1 lebih dulu** — begitu endpointnya
dipakai, aplikasi tidak lagi bergantung pada berkas yang belum diserahkan.

### 3.1 `GET /v1/auth/pin/public-key` + `encryption_key_id`

Publik, tanpa token, `Cache-Control: public, max-age=300`.

```json
{ "data": {
  "algorithm": "RSA-2048/OAEP-SHA256",
  "key_id": "pin-key-v1",
  "public_key_pem": "-----BEGIN PUBLIC KEY-----\n…",
  "payload_shape": "{\"pin\":\"…\",\"nonce\":\"<uuid-v4>\",\"ts\":<unix>}",
  "encoding": "base64",
  "max_skew_sec": 60
} }
```

Yang mengikat:

- **Padding OAEP-SHA256** (`RSA/ECB/OAEPWithSHA-256AndMGF1Padding`). PKCS#1 v1.5
  **tidak akan pernah** terdekripsi — ada test backend yang menjaganya.
- Plaintext tetap `{"pin":"…","nonce":"<uuid-v4>","ts":<unix>}`. Nonce sekali
  pakai (diingat 120 detik), skew maksimal 60 detik. Jam perangkat yang meleset
  lebih dari itu akan terlihat seperti "PIN selalu salah" — tampilkan pesan yang
  menyebut jam, bukan PIN.
- **Mulai kirim `encryption_key_id`** di `/auth/login/pin`, `/auth/pin/verify`,
  `/auth/pin/change`, `/auth/access-code/change`, dan `/onboarding/credentials`.
  Field opsional, jadi aman untuk build lama. Begitu dikirim, kunci kedaluwarsa
  dijawab **`422 AUTH_PIN_KEY_UNKNOWN` beserta `details.expected_key_id`** —
  itu sinyal untuk memuat ulang kunci dan mencoba sekali lagi, **bukan** untuk
  memberi tahu nasabah bahwa PIN-nya salah.
- Endpoint ini dan `GET /v1/onboarding/credentials/public-key` menjawab
  **identik**. Satu jalur kode client cukup; jangan buat dua.
- Berkas di `assets/` tetap dipakai sebagai cadangan saat endpoint tak terjangkau.

### 3.2 Kontrak tanda tangan biometrik

Kelima hal yang dulu tidak disebut, sekarang pasti:

| Hal | Nilai |
|---|---|
| Jenis kunci | **EC P-256** (secp256r1). Lain → `422 AUTH_BIOMETRIC_KEY_UNSUPPORTED` |
| Algoritma | **`SHA256withECDSA`** |
| `signed_challenge` | **Base64 dari DER** (keluaran bawaan `java.security.Signature`). Raw `r‖s` 64 byte masih diterima, tapi DER yang didokumentasikan |
| `public_key` | **Base64 X.509 SPKI tanpa header PEM**; PEM tetap diterima |
| Yang ditandatangani | **`challenge` apa adanya**, di-decode dari Base64 lebih dulu. Tanpa `device_id`, tanpa prefiks panjang |

```json
// GET /v1/auth/biometric/challenge?device_id=… → 200
{ "data": { "challenge_id": "…", "challenge": "<base64 32 byte>",
            "expires_in": 60, "expires_at": "…",
            "algorithm": "EC-P256",
            "signature_format": "base64(DER ASN.1) of SHA256withECDSA over the raw 32 challenge bytes" } }

// POST /v1/auth/biometric/register → 201
{ "data": { "biometric_id": "…", "key_id": "…",
            "registered_at": "…", "replaced_keys": 1 } }
```

- Respons challenge **membawa kontraknya sendiri** (`algorithm`,
  `signature_format`) — baca dari sana, jangan hardcode dari prosa ini.
- Challenge **60 detik dan benar-benar sekali pakai** (server memakai `GETDEL`).
  Challenge yang sudah dipakai dijawab `401 AUTH_TOKEN_INVALID`, bukan hanya
  yang kedaluwarsa. Jangan pernah mencoba ulang dengan challenge yang sama;
  ambil yang baru.
- **Pendaftaran ulang MENGGANTI.** Sidik jari baru → daftar ulang; semua kunci
  aktif nasabah itu pada perangkat itu dicabut, jumlahnya dilaporkan sebagai
  `replaced_keys`. Respons register bukan lagi `{ "message": … }`.
- Beberapa perangkat per nasabah boleh: pencabutan dibatasi satu perangkat.
- `signature` masih diterima sebagai nama lain `signed_challenge`. Pakai
  `signed_challenge`.

---

## 4. Buka rekening

### 4.1 Pilih kartu Paspor — step `CARD_SELECTION`

```
GET /v1/onboarding/products/{product_type}/cards      ← katalog
PUT /v1/onboarding/sessions/{session_id}/card         ← simpan pilihan
```

- **`X-Device-ID` wajib untuk endpoint katalog**, walau belum ada sesi: header
  itu satu-satunya identitas sebelum sesi lahir, dan batas laju katalog
  bergantung padanya. Tanpa header → `400 VALIDATION_ERROR` dengan
  `details.missing_header`.
- Lineup berbeda per produk: `TAHAPAN_BCA` tiga kartu · `TAHAPAN_XPRESI` Blue +
  Gold · `TABUNGANKU` Blue saja. Default selalu Blue. Jangan hardcode tiga kartu.
- Biaya dan limit datang dari server dan **bukan** angka di `strings.xml`. Kalau
  keduanya berbeda, yang benar adalah server — hapus angka dari `strings.xml`,
  jangan "perbaiki" tampilan agar cocok.
- `meta.catalog_outdated: true` berarti `catalog_version` yang client kirim
  sudah bukan yang terkini → muat ulang katalog sebelum melanjutkan.
- Detail kontraknya ada di `docs/08-PILIH-KARTU-API-SPEC.md`.

### 4.2 `ice_servers` untuk video call

`POST /v1/onboarding/video-call/queue` dan `POST /v1/onboarding/video-call/agent-token`
sekarang mengirim:

```json
{ "data": { "queue_id": "…", "queue_number": "A-014", "position": 3,
  "estimated_wait_seconds": 420,
  "operating_hours": { "start": "08:00", "end": "20:00", "timezone": "Asia/Jakarta" },
  "signaling_url": "wss://…/v1/onboarding/video-call/signal?token=…",
  "signaling_expires_in": 300,
  "ice_servers": [ { "urls": ["stun:…"] },
                   { "urls": ["turn:…?transport=udp"], "username": "…", "credential": "…" } ] } }
```

- `ice_servers` sudah berbentuk `RTCIceServer` — **teruskan apa adanya** ke
  `PeerConnection`, jangan petakan ulang.
- **Token di `signaling_url` berumur 5 menit dan sekali pakai.** Ditandai
  terpakai pada sambungan WebSocket pertama, jadi URL yang tersalin tidak bisa
  dipakai ulang. Sambungan yang terputus **harus join antrean lagi** untuk
  mendapat token baru — jangan menyambung ulang dengan URL yang sama.
- `ice_servers` kosong adalah jawaban yang sah: artinya TURN belum
  dikonfigurasi. Panggilan masih jadi di jaringan ramah dan gagal di seluler
  ber-NAT ketat. Bedakan ini dari kegagalan.
- **Blocker client:** dependency WebRTC belum disetujui. Sampai itu selesai,
  hanya bagian antrean yang bisa diintegrasikan; itu **gap client**, bukan gap
  backend.

### 4.3 `403 ONBOARDING_DEVICE_MISMATCH`

Semua **sebelas** endpoint yang menerima `session_id` sekarang memverifikasi
`X-Device-ID` terhadap perangkat pembuat sesi: `GET`/`DELETE /sessions/{id}`,
`PUT /sessions/{id}/card`, `POST /ocr`, `GET /ocr/{session_id}`,
`POST /personal-data`, `POST /verify-otp`, `POST /resend-otp`,
`POST /biometric`, `POST /video-call/queue`, `POST /credentials`, `POST /submit`.

- Perilaku layar: sesi tidak bisa dilanjutkan dari perangkat ini → mulai ulang
  dari awal, dengan pesan yang menjelaskan **kenapa**. Jangan tampilkan sebagai
  error jaringan, dan jangan coba lagi otomatis.
- **Pasang ulang aplikasi memutus draf, dan itu memang yang diinginkan.**
  `ANDROID_ID` berganti, sesi lama kedaluwarsa sendiri setelah 24 jam.
- `X-Request-ID` **tidak dipantulkan**. Server membuat id sendiri; yang muncul
  di `meta.request_id` dan header respons adalah milik server. Untuk korelasi
  log, pakai nilai dari respons, bukan yang client kirim.

---

## 5. Sudah ada di backend, belum dipakai client

Jangan buat tiket backend untuk lima hal ini — endpointnya sudah ada.

| Endpoint | Yang kurang di client |
|---|---|
| `GET /v1/transactions/{id}/receipt/pdf` | Unduh struk. Repository siap, tombolnya belum ada. |
| `POST /v1/account/profile/otp` → `PUT /v1/account/profile` | Ubah email & nomor HP. Repository siap, **layarnya belum ada dan tidak ada di Stitch** → berlaku Aturan #3: STOP dan lapor. |
| `POST /v1/account/device/push-token` | **Project tidak memakai Firebase sama sekali**, jadi belum ada sumber token. Menambah `firebase-messaging` adalah keputusan dependency, bukan pekerjaan integrasi. |
| `GET /v1/onboarding/video-call/signal` | WebSocket signaling siap; menunggu dependency WebRTC (§4.2). |
| `/v1/registration/*` | **Usang.** Responsnya kini membawa `Deprecation: true` dan `Link: rel="successor-version"`. Yang berlaku `/v1/onboarding/*`. Jangan menambah pemanggil baru; header `Sunset` belum ada karena tanggal penghapusan belum disepakati. |

---

## 6. Yang BELUM ada di backend — jangan menunggu, jangan mengarang

| Layar | Status backend |
|---|---|
| `Pulsa & Paket Data` | Katalog + inquiry + pembelian **sedang dikerjakan** di repo backend. Jangan buat DTO dari tebakan; tunggu kontraknya terbit di `docs/01`. |
| `Ganti Kode Akses` (verifikasi kartu ATM + OTP) | **Sedang dikerjakan.** Dua layarnya sudah ada di Stitch. |
| `m-Commerce`, `m-Admin`, `Cardless` | **Belum ada kontrak apa pun** — tidak ada endpoint, tabel, maupun aturan bisnis. Layar boleh dibuat dari Stitch, tapi integrasinya menunggu. |
| `m-Info` | Belum jelas, menunggu keputusan produk. Sementara diarahkan ke Rekening & Kartu. |
| Certificate pinning | Hash SPKI **masih ditunggu** dari infrastruktur. `NetworkModule.certificatePinner()` siap tapi daftarnya kosong. **Jangan mengisi hash karangan** — pinning dengan hash salah bukan "pinning yang berjalan", ia menolak setiap koneksi dan mematikan aplikasi di lapangan sampai ada rilis baru. |

---

## 7. Selesai bila

Per endpoint yang diintegrasikan:

- [ ] DTO dicocokkan ke handler backend, bukan ke contoh di spec (Aturan #0)
- [ ] Envelope `status`/`data`/`error`/`pagination`/`meta` ditangani, bukan objek telanjang
- [ ] Setiap `code` error di tabel terkait punya perilaku UI, bukan hanya toast generik
- [ ] Field nullable benar-benar nullable: `blocked_reason`, `tier`
- [ ] Aksi berbiaya mengirim `X-Idempotency-Key` yang **stabil** melintasi rotasi layar
- [ ] Aksi keamanan mengambil `verification_token` segar; token tidak pernah di-cache
- [ ] Tidak ada PIN, OTP, token, `masked_number`, atau PII yang masuk log

Sebelum bilang selesai, jalankan build dan lint Android, lalu uji ujung ke ujung
terhadap backend lokal (`make dev` di repo backend, `make tunnel` kalau dari
perangkat fisik). Layar yang "tersambung" tapi belum pernah menerima respons
sungguhan belum selesai.
