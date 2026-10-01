---
name: bca-api-integrasi-semua-layar
description: Menyambungkan backend BCA Mobile API ke SELURUH layar aplikasi bca_mobile tanpa kecuali — audit cakupan endpoint per layar, urutan pengerjaan, dan aturan berhenti. Gunakan saat diminta "integrasikan API ke semua halaman", "cek layar mana yang belum tersambung", "endpoint apa yang belum dipakai client", audit menyeluruh integrasi, atau saat merencanakan sprint integrasi lintas layar. Trigger juga pada "semua halaman diintegrasikan", "layar belum ada API-nya", "coverage endpoint", "layar mana yang masih dummy", dan "sweep integrasi". JANGAN dipakai untuk mengerjakan SATU endpoint atau satu layar saja — itu `bca-mobile-api` (umum) atau `buka-rekening-api` (onboarding); jangan dipakai untuk visual/token (itu `stitch-to-compose`) atau navigasi/ViewModel (itu `compose-architecture`).
---

# Integrasi API ke Seluruh Layar — bca_mobile

Skill ini adalah **pengendali pekerjaan**, bukan referensi kontrak API. Isinya: daftar
kerja lengkap, urutan, dan aturan berhenti. Detail request/response, envelope, enkripsi
PIN, dan idempotency **tidak diulang di sini** — itu milik skill lain (§8).

Dipakai saat perintahnya berbentuk "integrasikan semua", bukan "kerjakan endpoint X".

---

## 0. Tiga aturan yang mengatur segalanya

**Aturan A — Satu layar per sesi.** "Semua halaman" adalah daftar kerja, bukan izin
mengerjakan semuanya sekaligus. Kerjakan satu baris dari §3, laporkan, baru lanjut.
Satu PR yang menyentuh 20 layar tidak bisa ditinjau manusia.

**Aturan B — Tiga cabang saat layar belum ada.** Jangan pernah mengarang layar.

| Kondisi | Tindakan |
|---|---|
| Layar ada di project | Integrasikan langsung. |
| Layar belum ada, **tapi ada di Stitch** | Jalankan kalimat persis: `Implement screen (nama halaman di stich) dari desain.` — ganti bagian dalam kurung dengan judul screen Stitch apa adanya. Layar dibuat dulu lewat skill `stitch-to-compose`, **baru** diintegrasikan di sesi terpisah. |
| Layar belum ada **dan tidak ada di Stitch** | **STOP dan lapor.** Jangan merancang layar sendiri. Endpoint tanpa desain bukan izin berimprovisasi. |

**Aturan C — Tidak ada data karangan.** Kalau endpoint tidak membalas sebuah field yang
digambar desain, tampilkan penanda kosong dan catat alasannya di komentar — jangan
memakai nilai contoh dari artefak desain. Ini sudah jadi preseden di `AkunScreen.kt`
(nomor kartu, masa berlaku) dan `BukaRekeningPilihKartuScreen.kt`.

---

## 1. Sebelum mulai: tentukan status sebenarnya

Tabel status di `CLAUDE.md` **bisa tertinggal** dari kode. Pernah terjadi: tabel menyebut
layar QRIS pemindai dan Rentang Waktu "belum ada" padahal file-nya sudah ada dan terpasang
di `MainGraph.kt`. Verifikasi sendiri, jangan percaya tabel:

```bash
# Layar yang ada
find app/src/main/java/id/bca/bcamobile/ui/screen -name "*Screen.kt" | sort

# Layar yang punya ViewModel (indikator kuat sudah tersambung)
find app/src/main/java/id/bca/bcamobile/ui/screen -name "*ViewModel.kt" | sort

# Apakah sebuah endpoint sudah dipakai client sama sekali
grep -rl "account/dashboard" app/src/main/java/

# Apakah layar benar-benar terpasang di graph
grep -n "RentangWaktu" app/src/main/java/id/bca/bcamobile/ui/navigation/MainGraph.kt
```

Layar tanpa `*ViewModel.kt` **dan** tanpa ViewModel flow bersama adalah kandidat kuat
"belum tersambung" — tapi konfirmasi dulu: buka rekening memakai satu
`BukaRekeningFlowViewModel` untuk 14 layar, dan flow transaksi memakai satu ViewModel
per flow. Ketiadaan file ViewModel di paket layar bukan bukti.

---

## 2. Urutan pengerjaan

Kerjakan menurun. Baris atas memblokir baris bawah.

1. **Endpoint yang sudah punya layar tapi belum dipanggil** — hasil tertinggi, risiko terendah.
2. **Layar yang ada tapi masih memakai state statis** (tanpa ViewModel / `UiState` hardcoded).
3. **Layar yang belum ada tapi ada di Stitch** — buat layar dulu (Aturan B), integrasi menyusul.
4. **Endpoint tanpa layar dan tanpa desain** — laporkan, jangan kerjakan.

---

## 3. Daftar kerja — seluruh layar

Kolom **Status** diisi ulang setiap sesi lewat perintah di §1. Nilai di bawah adalah
kondisi saat skill ini ditulis, bukan kebenaran abadi.

### 3.1 GraphAuth

| Layar | Endpoint | Status awal |
|---|---|---|
| Splash | `GET /health` | Tersambung — `maintenance_mode` / `force_update` memblokir sebelum NavHost |
| Login m-BCA | pintu masuk, tanpa panggilan | Tersambung |
| Kode Akses | `POST /auth/login/pin` | Tersambung |
| Face ID | `GET /auth/biometric/challenge` → `POST /auth/login/biometric` | Tersambung |
| Touch ID | idem Face ID | Tersambung |
| Buka Rekening — 14 layar | `/onboarding/*` | Tersambung, **kecuali** video call |
| Buka Rekening — Video Call | `POST /onboarding/video-call/queue`, `GET /video-call/signal`, `POST /video-call/agent-token` | **Sebagian** — hanya antrean yang dipanggil. Backend sudah menyediakan WebSocket signaling; yang belum ada di sisi client adalah dependency WebRTC. Ini **gap client**, bukan gap backend. |
| Ganti Kode Akses | `POST /auth/access-code/change` | Tersambung, **tapi 1 layar** melawan 5 layar di Stitch (§3.4) |

**Kode akses dan PIN adalah dua rahasia berbeda.** `POST /auth/access-code/change`
memindahkan kredensial login; `POST /auth/pin/change` memindahkan PIN transaksi.
Jangan tertukar — router backend memberi komentar eksplisit soal ini.

### 3.2 GraphMain

| Layar | Endpoint | Status awal |
|---|---|---|
| Beranda | `GET /account/dashboard`, `GET /account/balance` | Tersambung |
| Mutasi | `GET /transactions/mutations` | Tersambung |
| Rentang Waktu | `GET /transactions/mutations?from=&to=` | Lapisan data sudah benar (`period` dikosongkan untuk rentang kustom). Layar masih tanpa ViewModel — `MainGraph.kt` mengoper `RentangWaktuUiState` statis. |
| Riwayat | `GET /transactions/history` | Tersambung — handler hanya membaca `limit`, `cursor`, `type`. **Tanpa parameter periode**, berbeda dari `mutations`. Gap backend, lihat §9. |
| Bukti Transaksi | `GET /transactions/{id}/receipt` | Tersambung |
| Bukti Transaksi — unduh PDF | `GET /transactions/{id}/receipt/pdf` | **Gap client.** Endpoint sudah ada di backend, nol pemakaian di client. |
| Notifikasi | `GET /notifications`, `PUT .../read`, `PUT .../read-all` | Tersambung — tab menyaring yang sudah termuat; endpoint tanpa filter jenis |
| Profil Saya / Akun | `GET·PUT /account/profile`, `PUT /account/settings` | Tersambung. Tiga sakelar (biometrik, notifikasi, e-Statement) berfungsi sejak perbaikan §9.0. Kartu Paspor dan tier tetap penanda kosong — tak ada endpoint. |
| Rekening & Kartu | `GET /account/balance` | Tersambung |
| Atur Limit | `GET·PUT /account/transaction-limit` + `POST /auth/pin/verify` | Tersambung penuh; batas dimuat saat layar dibuka. |
| Ubah Kode Akses | `POST /auth/access-code/change` | Tersambung |
| Ubah PIN transaksi | `POST /auth/pin/change` | Tersambung |
| Ubah profil (email/HP) | `POST /account/profile/otp` → `PUT /account/profile` | Repository siap. **Layarnya belum ada** — tidak ada di Stitch juga. |
| Push token | `POST /account/device/push-token` | Repository siap. Terhenti: project **tidak memakai Firebase**, jadi tidak ada sumber token. |

### 3.3 Flow transaksi

Semuanya memakai satu ViewModel per flow, di-scope ke entri graph.

| Layar | Endpoint | Status awal |
|---|---|---|
| Menu Transfer | `GET /transfer/recent` | Tersambung |
| Transfer Antar Rekening | `POST /transfer/inquiry` → `/auth/pin/verify` → `/transfer/execute` | Tersambung |
| Scan QRIS + Konfirmasi | `POST /qris/decode`, `POST /qris/pay` | Tersambung |
| Top Up E-Wallet + Konfirmasi | `GET /ewallet/providers`, `POST /ewallet/inquiry`, `POST /ewallet/topup` | Tersambung |

### 3.4 Belum ada layarnya — pakai Aturan B

| Judul screen di Stitch | Endpoint | Cabang |
|---|---|---|
| `m-Info` | belum ada | Ada di Stitch. `QuickAction.M_INFO` sementara diarahkan ke Rekening & Kartu. |
| `m-Commerce` | belum ada | Ada di Stitch, endpoint belum ada → buat layar dulu, integrasi menunggu backend. |
| `m-Admin` | belum ada | idem |
| `Cardless` | belum ada | idem |
| `Pulsa & Paket Data` | belum ada | idem |
| `Hubungi CS` | belum ada | idem |
| `Pusat Bantuan` | belum ada | idem |
| `Ganti Kode Akses - Kode Akses Lama` | `POST /auth/pin/change` | Ada di Stitch, endpoint ada → layak dikerjakan |
| `Ganti Kode Akses - Verifikasi Kartu ATM` | belum ada | Ada di Stitch |
| `Ganti Kode Akses - Verifikasi OTP` | belum ada | Ada di Stitch |
| `Ganti Kode Akses - Buat Kode Baru` | `POST /auth/pin/change` | Ada di Stitch |
| `Ganti Kode Akses - Berhasil Diubah` | — | Ada di Stitch |

Untuk setiap baris di atas, kalimat yang dijalankan persis seperti ini:

```
Implement screen (nama halaman di stich) dari desain.
```

Contoh: `Implement screen Pulsa & Paket Data dari desain.`

Judul di dalam kurung **disalin apa adanya** dari daftar screen Stitch — termasuk tanda
hubung dan ampersand. Salah judul berarti agent mengambil desain yang salah.

### 3.5 Endpoint tanpa layar dan tanpa desain — STOP

| Endpoint | Masalah |
|---|---|
| `POST /registration/initiate` | Empat endpoint registrasi nasabah lama ada di backend, **nol** pemakaian di client, dan **tidak ada** screen Stitch-nya. |
| `POST /registration/verify-otp` | idem |
| `POST /registration/upload-document` | idem |
| `POST /registration/complete` | idem |

Ini kondisi Aturan B cabang ketiga. Laporkan ke manusia dan minta keputusan: apakah
alur registrasi memang di luar lingkup aplikasi nasabah, atau desainnya belum dibuat.
**Jangan** merancang empat layar registrasi sendiri.

---

## 4. Prosedur per layar

Enam langkah. Jangan lompat.

**1. Tentukan kontrak.** Buka `docs/backend/01-API-SPECIFICATION.md` (atau `06-…` untuk
onboarding). Catat: path, method, jaringan mana (§5), field request, field response,
error code yang mungkin.

**2. Bandingkan dengan yang ditampilkan layar.** Buat dua daftar: field yang dipakai UI,
dan field yang dibalas endpoint. Selisih ke arah mana pun adalah temuan:

- UI butuh, endpoint tidak balas → penanda kosong + komentar (Aturan C). Jangan mengarang.
- Endpoint balas, UI tidak pakai → catat; mungkin desain tertinggal, mungkin field mati.

**3. Layer data.** DTO + mapper + `RepositoryImpl`, mengikuti `android-architecture-patterns`.
Semua panggilan lewat `ApiCaller` supaya retry dan klasifikasi error seragam.

**4. Layer domain.** Interface repository di `domain/<domain>/`, model domain tanpa
anotasi JSON.

**5. ViewModel + UiState.** ViewModel hidup di graph, bukan di dalam layar. Untuk flow
multi-langkah pakai satu ViewModel yang di-scope ke entri graph, seperti
`BukaRekeningFlowViewModel` dan flow transaksi. Batas persisnya diatur `compose-architecture`.

**6. Empat state wajib.** `loading`, `empty`, `error` + retry, `success`. Desain biasanya
hanya menggambar `success`. Teks empty **dikonfirmasi**, tidak dikarang.

---

## 5. Yang paling sering salah

**Salah jaringan.** Ada dua qualifier dan tidak boleh disatukan:
`@OnboardingNetwork` (tanpa `Authorization`) dan `@AppNetwork` (dengan `Authorization`
+ auto-refresh). Endpoint `/onboarding/*` memakai yang pertama; sisanya yang kedua.

**Merakit URL WebSocket.** `signaling_url` datang dari response `video-call/queue`.
Bukan konstanta, bukan turunan base URL.

**Menaruh base URL di buildType.** Alamat dan `CERTIFICATE_PINNING_ENABLED` ikut
**product flavor**. `buildConfigField` buildType menang atas flavor, jadi nilainya akan
membeku kalau dipindah.

**Mengirim PIN apa adanya.** `assets/pin_public.pem` belum ada, jadi `PinEncryptor.encrypt`
membalas `null` dan operasi berbasis PIN gagal lebih awal. Itu perilaku yang benar —
jangan dilewati dengan mengirim PIN mentah.

**Idempotency QRIS.** Spec §8 menaruh `idempotency_key` di body `qris/pay`, sedangkan
§Headers dan transfer/e-wallet memakai header `X-Idempotency-Key`. Client memakai
**header**. Jangan "diperbaiki" mengikuti §8.

**Menulis PII ke disk.** Foto KTP, wajah, NIK, kode akses, PIN: hanya di memory.
`OnboardingSessionStore` menyimpan `session_id` dan idempotency key saja.

---

## 6. Definition of Done per layar

- [ ] Endpoint dipanggil lewat `ApiCaller`, bukan Retrofit langsung dari ViewModel.
- [ ] Jaringan benar (`@OnboardingNetwork` vs `@AppNetwork`).
- [ ] `loading` / `empty` / `error` + retry / `success` semuanya tertangani.
- [ ] Error code dipetakan ke teks lewat `ErrorText`, bukan pesan mentah server.
- [ ] Tidak ada PII yang ditulis ke disk atau di-log.
- [ ] Layar tetap stateless — `*Screen.kt` menerima `state` + lambda, tidak menerima ViewModel.
- [ ] Field yang tidak dibalas server tampil sebagai penanda kosong, bukan nilai contoh.
- [ ] `./gradlew compileLocalDebugKotlin` lulus.
- [ ] `./gradlew lintLocalDebug` lulus.
- [ ] `./scripts/check-hardcoded-ui.sh` lulus, tanpa `--update-baseline`.
- [ ] Route atau graph berubah → `compose-architecture/references/navigation.md` diperbarui di commit yang sama.
- [ ] Endpoint baru tersambung → tabel status di `CLAUDE.md` diperbarui di commit yang sama.

---

## 7. Laporan akhir sesi

Setiap sesi ditutup dengan tiga hal, bukan satu:

1. **Selesai** — layar mana, endpoint mana.
2. **Tidak terverifikasi** — apa yang tidak bisa dibuktikan dan kenapa. Contoh nyata:
   `MainActivity.kt` memasang `FLAG_SECURE`, jadi `adb screencap` dan `uiautomator dump`
   dua-duanya mengembalikan kosong. Perbandingan visual harus dilakukan manusia.
   Jangan mematikan `FLAG_SECURE` demi kenyamanan verifikasi.
3. **Diblokir** — baris §3 yang tidak bisa dikerjakan, beserta alasan dan keputusan
   yang dibutuhkan dari manusia.

---

## 8. Batas dengan skill lain

Skill ini **tidak** memuat kontrak API, pola arsitektur, atau aturan visual. Rujuk:

| Kebutuhan | Skill |
|---|---|
| Envelope, token, idempotency, error code, enkripsi PIN | `bca-mobile-api` |
| Kontrak `/onboarding/*` | `buka-rekening-api` |
| Layar OTP onboarding | `frontend-otp-verification` |
| Repository, UseCase, Hilt, MVI | `android-architecture-patterns` |
| Route, graph, batas ViewModel, keamanan sesi | `compose-architecture` |
| Membuat layar dari desain Stitch | `stitch-to-compose` |
| CameraX, ML Kit, RSA Keystore | `buka-rekening-native-android` |
| WebRTC dan signaling | `buka-rekening-video-call` |
| Login biometrik, AndroidKeyStore | `android-biometric-keystore` |
| Performa, ANR, recomposition, R8 | `performance-quality` |

Dokumen kontrak: `docs/backend/01-API-SPECIFICATION.md`,
`docs/backend/06-BUKA-REKENING-API-SPEC.md`, `docs/backend/08-PILIH-KARTU-API-SPEC.md`.

---

## 9. Backlog backend — apa yang belum punya endpoint

Bagian ini adalah **daftar kerja untuk repo `bca-mobile-api`**, bukan untuk client.
Selama sebuah baris di sini belum selesai, layar terkait tidak bisa diintegrasikan
dan harus memakai penanda kosong (Aturan C).

Kontrak rinci untuk §9.1 ada di skill `profil-saya-kartu-api` di repo backend.

### 9.0 Cacat kontrak yang sudah diperbaiki — jangan diulang

Enam kekeliruan di bawah **sudah dibetulkan di client**. Dicatat supaya tidak
kembali saat DTO disalin dari spec alih-alih dari handler.

| # | Gejala | Sebab | Perbaikan |
|---|---|---|---|
| 1 | Sakelar biometrik & notifikasi selalu membalik sendiri | `SettingsRequest` memakai `is_biometric_enabled`/`notification_enabled` | jadi `biometric_enabled`/`push_notification_enabled`/`email_statement_enabled` |
| 2 | ID dan nomor HP kosong di Profil Saya | `ProfileResponse` memakai `user_id`/`phone_number`/`last_login` | jadi `id`/`phone`/`last_login_at` |
| 3 | Rentang kustom Mutasi mengembalikan semua data | `period=CUSTOM` + `start_date`/`end_date` | `period` dikosongkan, kirim `from`/`to` |
| 4 | Ubah Kode Akses memindahkan PIN transaksi | memanggil `/auth/pin/change` | jadi `/auth/access-code/change` |
| 5 | Simpan limit berhasil di server tapi terbaca gagal | `limits` dimodelkan objek `Long` | jadi `Map<String, String>` desimal |
| 6 | Atur Limit kosong sampai disimpan sekali | `GET /account/transaction-limit` tak pernah dipanggil | dipanggil di `init` |

**Pelajarannya: bentuk DTO diturunkan dari handler backend, bukan dari contoh di
spec.** Kekeliruan 1, 2, dan 5 semuanya lahir dari menyalin spec yang berbeda
dari kode yang berjalan.

<details>
<summary>Rincian kekeliruan #1</summary>

**`PUT /account/settings` menolak setiap panggilan dari aplikasi.**

| Sisi | Field yang dipakai |
|---|---|
| Spec `01-API-SPECIFICATION.md` §body | `biometric_enabled`, `push_notification_enabled`, `email_statement_enabled` |
| Backend `UpdateSettingsRequest` | sama dengan spec — **benar** |
| Client `SettingsRequest` (`AccountDto.kt`) | `is_biometric_enabled`, `notification_enabled` — **menyimpang** |

Ketiga field backend bertipe `*bool`. Nama yang tidak cocok terurai jadi `nil`,
dan `Service.UpdateSettings` membalas `ValidationError` kalau ketiganya `nil`.
Akibatnya sakelar Login Biometrik dan Notifikasi Transaksi di Profil Saya
**selalu gagal** dan membalik sendiri.

Yang menyimpang adalah client. Perbaikannya di `AccountDto.kt`, bukan di backend.
Dugaan asal kekeliruan: `GET /account/profile` memang membalas `is_biometric_enabled`,
dan nama itu ikut tersalin ke request body.

Sekalian: `email_statement_enabled` sudah dilayani backend, dan barisnya kini
sudah jadi sakelar yang berfungsi.

</details>

### 9.1 Profil Saya — tujuh kebutuhan tanpa endpoint

Diturunkan dari `AkunScreen.kt`. Kolom kiri menyebut apa yang layar butuhkan.

| Butuh | Usulan endpoint | Catatan |
|---|---|---|
| Kartu nasabah: nomor tersamar, nama pemegang, masa berlaku, tier, status | `GET /v1/account/cards` | Belum ada tabel kartu milik nasabah. `card_products` dan `onboarding_card_issuance` hanya melayani flow buka rekening. |
| Sakelar Transaksi Debit Online dan Transaksi Luar Negeri | `PUT /v1/account/cards/{card_id}/settings` | Dua sakelar ini sekarang terpasang di UI tapi tidak bergerak. |
| Aksi Blokir Kartu | `POST /v1/account/cards/{card_id}/block` | Wajib `verification_token` dari `POST /auth/pin/verify`. |
| Aksi Kontrol Akses | — | Kemungkinan besar nama lain untuk baris kedua. Konfirmasi ke produk sebelum membuat endpoint ketiga. |
| Aksi Ganti Kartu dan Permintaan Penggantian Kartu | `POST /v1/account/cards/{card_id}/replacement` | Wajib `X-Idempotency-Key` dan `verification_token`. |
| Badge tier nasabah ("Prioritas") | tambah field `tier` di `GET /account/profile` | **Bukan** endpoint baru — cukup satu kolom dan satu field. |
| Pusat Bantuan & FAQ, Halo BCA CS | `GET /v1/content/help-center`, `GET /v1/content/contact-cs` | Tanpa ini, dua baris itu hanya bisa menjawab "belum tersedia". |

Dua aturan repo backend yang mengikat §9.1:

- **PAN lengkap tidak pernah disimpan atau dikirim.** Hanya `masked_number`.
  Aturan ini sudah ditulis di migrasi `000020`.
- **Nilai visual tidak dikirim server.** Kartu memakai `style` (`BLUE`/`GOLD`/`PLATINUM`)
  yang dipetakan client ke token. Hex warna dan URL gambar dilarang.

### 9.2 Layar lain yang menunggu backend

| Layar | Butuh | Ukuran |
|---|---|---|
| Riwayat | parameter periode di `GET /transactions/history` | Kecil — `mutations` sudah punya `period`/`from`/`to`, tinggal disamakan |
| Notifikasi | filter jenis di `GET /notifications` | Kecil — tab sekarang hanya menyaring yang sudah termuat |
| `m-Commerce` | seluruh domain | Besar, belum ada apa pun |
| `m-Admin` | seluruh domain | Besar, belum ada apa pun |
| `Cardless` | seluruh domain | Besar, belum ada apa pun |
| `Pulsa & Paket Data` | katalog produk + inquiry + pembelian | Besar, belum ada apa pun |
| `m-Info` | belum jelas, sementara diarahkan ke Rekening & Kartu | Perlu keputusan produk |
| `Ganti Kode Akses - Verifikasi Kartu ATM` | verifikasi nomor kartu + PIN ATM | Belum ada endpoint |
| `Ganti Kode Akses - Verifikasi OTP` | OTP untuk jalur ganti kode akses | Belum ada endpoint |

### 9.3 Yang TIDAK perlu backend

Jangan membuat tiket backend untuk lima hal ini — endpoint-nya sudah ada dan yang
kurang adalah pemakaiannya di client:

- `GET /transactions/{id}/receipt/pdf` — unduh struk
- `GET /video-call/signal` dan `POST /video-call/agent-token` — signaling sudah siap,
  yang belum ada dependency WebRTC di client
- `POST /account/profile/otp` → `PUT /account/profile` — ubah email dan nomor HP.
  Repository sudah menyediakannya; **layar pengubah profil belum ada**.
- `POST /account/device/push-token` — repository sudah menyediakannya, tapi
  **project tidak memakai Firebase sama sekali**, jadi belum ada sumber token.
  Menambah `firebase-messaging` adalah keputusan dependency, bukan integrasi.
- `GET /transactions/mutations?period=` — pemasok layar Rentang Waktu
