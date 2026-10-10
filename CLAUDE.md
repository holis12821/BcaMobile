# CLAUDE.md

Konteks project untuk AI coding agent. File ini **selalu** masuk konteks di setiap sesi.
Isinya: fakta yang berlaku universal + peta alur, supaya agent tahu harus ke mana
**tanpa membaca seluruh repo** lebih dulu. Detail per-domain ada di `.claude/skills/`.

Batas file ini **200 baris**. Kalau penjelasan butuh lebih panjang, tempatnya di skill —
bukan di sini. Panduan yang terlalu panjang cenderung terabaikan.

## Project

`bca_mobile` — aplikasi Android, Kotlin, 100% Jetpack Compose + Material3. Tanpa XML layout.

- Package & applicationId: `id.bca.bcamobile` — module tunggal (`:app`)
- minSdk 26, targetSdk 36, compileSdk 37, Kotlin 2.2.10, AGP 9.3.2, Compose BOM 2026.02.01
- DI **Hilt** (`BcaMobileApplication` + `MainActivity` `@AndroidEntryPoint`)
- **4 product flavor** dimensi `environment`: `local` (emulator, default), `ngrok`
  (tester), `staging`, `production`. Varian = flavor × buildType, jadi 8.
- Jaringan: Retrofit + OkHttp + kotlinx-serialization; push: Firebase Messaging (BoM)
- Theme composable: `BcaMobileTheme` — pertahankan namanya, jangan bikin theme kedua

### Jebakan tema yang harus diketahui

`BcaMobileTheme` menerima `darkTheme`/`dynamicColor` lalu **mengabaikan keduanya** —
`Theme.kt:52` mengunci ke `LightColorScheme`, jadi `@Preview(darkTheme = true)` tetap terang.
Bukan bug: skema gelap belum didefinisikan desain, **jangan dibuat dengan nilai tebakan**.

## Struktur paket

```text
id.bca.bcamobile
├── core/        camera, device, download, format, liveness, network (ApiEnvelope, ErrorText), ocr, push, qris, security
├── data/        {remote/dto, mapper, RepositoryImpl} per domain
├── di/          NetworkModule, RepositoryModule
├── domain/      common/DataResult + onboarding, auth, account, card, content, transaction,
│                transfer, ewallet, notification, qris, config — {model, Repository}
├── session/     SessionRepository, SessionState, SessionViewModel, AppGate, AppLifecycleObserver
└── ui/          components, navigation, screen (15 paket layar), theme
```

Arsitektur: Clean Architecture + MVI. Screen stateless → state dari ViewModel →
repository → `ApiCaller` → Retrofit. Detail pola ada di `android-architecture-patterns`.

## Navigasi

Tiga graph: `GraphAuth`, `GraphMain`, `GraphEWallet` (`ui/navigation/Graph.kt`).
Route terdaftar di `Route.kt`; host di `BcaNavHost.kt`.

**Struktur navigasi di `compose-architecture/references/navigation.md` bersifat mengikat.**
Menambah route atau graph berarti memperbarui file itu di commit yang sama.

### Flow buka rekening (14 layar, di `GraphAuth`)

```text
BukaRekening (pilih jenis) → PilihKartu → SyaratKetentuan → PanduanFoto → KameraFoto
→ HasilFoto → DataPribadi → VerifikasiOtp → VerifikasiBiometrik → AntreanVideoCall
→ VideoCall → BuatKredensial → Ringkasan → BerhasilDibuat
```

**Satu layar = satu folder** (`Screen`/`ViewModel`/`UiState`/`Contract`, mewarisi
`common/BukaRekeningStepViewModel`). State bersama di `common/BukaRekeningSessionStore`; umurnya
diikat ke graph oleh `BukaRekeningFlowScopeViewModel` — di situ PII dibersihkan.
`GraphMain` memuat `Riwayat`, `Notifikasi`, `BuktiTransaksi(id)`, `PusatBantuan`, `HubungiCs`, dialog `KartuAksi`/`KartuPin`.
**Navigasi maju digerakkan server.** Sumber kebenarannya `current_step` dari response,
diteruskan sebagai side effect `AdvanceTo(step)`. Jangan menavigasi berdasarkan tebakan lokal.

## Integrasi API

**Dua jaringan terpisah**, ditandai qualifier — jangan disatukan:
`@OnboardingNetwork` (`ONBOARDING_BASE_URL`, tanpa Authorization) dan
`@AppNetwork` (`APP_BASE_URL`, dengan Authorization + auto-refresh).

Alamat ada di **product flavor**, bukan buildType — yang menentukan alamat adalah
lingkungan server, bukan minifikasi. `CERTIFICATE_PINNING_ENABLED` ikut flavor karena
alasan sama. Jangan memindahkannya ke buildType: buildConfigField buildType menang atas
flavor, jadi nilainya akan membeku. `INTERNAL_BASE_URL` ada tapi **tidak dipanggil
aplikasi nasabah** — `/internal/v1` butuh `X-Internal-API-Key` yang tidak boleh ikut APK.
**URL WebSocket signaling bukan konstanta**: datang sebagai `signaling_url` dari response
`video-call/queue`, jangan dirakit dari base URL. Peta: `bca-mobile-api/docs/10-…`.

- Semua response dibungkus `ApiEnvelope<T>` (`status`/`data`/`error`/`meta`/`pagination`)
- `ApiCaller` (`core/network/`) memusatkan retry & klasifikasi → `DataResult<T>`: jaringan 3×
  (1s/3s/5s), 5xx 2×, 429 hormati `Retry-After`, 401 → `Unauthorized`; teks UI lewat `ErrorText`
- `HeaderInterceptor` kirim `X-Device-ID` + `X-Request-ID` ke **kedua** jaringan; `X-Device-ID`
  wajib sama dengan `device_id` body — satu sumber di `DeviceIdProvider`
- Token: access di memory, refresh di EncryptedSharedPreferences (`TokenManager`);
  `TokenAuthenticator` refresh sekali saat 401, antre lewat mutex
- Transaksi: `inquiry → verifyPin → execute`, `IdempotencyKeyProvider` terikat `inquiry_id`
- `OnboardingSessionStore` hanya menyimpan `session_id` + idempotency key — **tidak ada PII ke disk**
- Certificate pinning aktif di release tapi **daftar pin kosong** (`NetworkModule`) — TODO infra
- **Selisih spec QRIS**: §8 menaruh `idempotency_key` di body `qris/pay`; §Headers dan
  transfer/e-wallet memakai header `X-Idempotency-Key`. Client memakai **header**
- Kartu: `block`/`replacement` butuh `verification_token` (`BLOCK_CARD`/`REPLACE_CARD`, 120d
  sekali pakai, jangan di-cache); `replacement` berbiaya → `X-Idempotency-Key` di `SavedStateHandle`
- `content/*` dan `auth/pin/public-key` **tanpa Authorization** — `AuthInterceptor.PUBLIC_PATHS`
- **`assets/pin_public.pem` belum ada** — tanpa itu `PinEncryptor.encrypt` balas null dan
  operasi berbasis PIN gagal lebih awal. Jangan mengirim PIN apa adanya sebagai jalan pintas.

### Status integrasi per fitur

| Fitur | Status |
|---|---|
| Session, OCR, data pribadi, biometrik, kredensial, submit | Tersambung ke `OnboardingApi` |
| Verifikasi OTP | Tersambung `verify-otp` + `resend-otp`; hitung mundur `otp_expires_at`, `OTP_BLOCKED` vs kuota kirim ulang dibedakan |
| Antrean video call | `POST video-call/queue` + `ice_servers`/`signaling_expires_in` dimodelkan; **WebRTC belum jadi dependency**, layar video call masih UI |
| Pilih jenis & kartu | **Tersambung** keduanya. Jenis rekening: `onboarding/products` — copy halaman ikut dari server, daftar `strings.xml` jadi fallback saat `ONBOARDING_CATALOG_UNAVAILABLE`/offline. Produk dikenali lewat `product_type`, **bukan indeks baris**. Kartu: `products/{type}/cards` + `card_type` di `sessions` dan `PUT sessions/{id}/card`; step `CARD_SELECTION` |
| Auth, Account, Mutasi, Transfer, e-Wallet | **Tersambung penuh** sampai layar. Alur transaksi memakai satu ViewModel per flow, di-scope ke entri graph |
| Riwayat | **Tersambung** `transactions/history` + filter `type` & `period` (kosakata sama dengan Mutasi); baris → struk `receipt`, tombol Simpan → `receipt/pdf` |
| Notifikasi | **Tersambung** `notifications?type=` + `read`/`read-all`; tab menyaring di server, cursor direset per tab |
| Splash | **Tersambung** `health`: `maintenance_mode`/`force_update` memblokir sebelum NavHost, `feature_flags` menyaring menu |
| QRIS | Client `qris/decode` + `qris/pay` ada; layar pemindai belum |
| Rentang Waktu | **Tersambung** — dipakai Mutasi **dan** Riwayat lewat `SavedStateHandle` entri masing-masing |
| Kartu & Profil | **Tersambung** `account/cards` + `settings`/`block`/`replacement`, dan field `tier` di `account/profile` |
| Pusat Bantuan & Kontak CS | **Tersambung** `content/help-center` + `content/contact-cs` (publik, tanpa Authorization) |

## Agent Rules

### File Modification
- Never modify, create, or delete files without asking for user confirmation first.
- Before applying any code change, show the proposed changes.
- Wait for explicit user approval before editing files.
- Do not use destructive commands without explicit approval.

### Aturan yang selalu berlaku

1. **Tidak ada nilai visual hardcoded.** Warna, spacing, radius, stroke, dan ukuran font
   selalu lewat token di `ui/theme/`. Dilarang `Color(0xFF…)`, `.dp`/`.sp` telanjang, dan
   `RoundedCornerShape(n.dp)` inline di luar `Color.kt`, `Dimens.kt`, `Shape.kt`, `Type.kt`.
2. **Token tidak ada → berhenti dan lapor.** Jangan membuat token baru sendiri, jangan
   memakai hex langsung, jangan membulatkan ke token terdekat diam-diam.
3. **String selalu ke `strings.xml`, additive only.** Tidak menghapus atau mengubah entri
   yang sudah ada tanpa diminta. Berlaku untuk semua resource XML di `res/`.
4. **Composable layar stateless.** File `*Screen.kt` menerima `state` + lambda, tidak boleh
   menerima ViewModel, repository, atau memanggil network. ViewModel hidup di graph
   (`AuthGraph.kt` dkk), bukan di dalam layar.
5. **PII hanya di memory.** Foto KTP, wajah, NIK, kode akses, dan PIN tidak ditulis ke disk,
   tidak di-log, dan tidak masuk state yang dipersistensi.
6. **Jangan menjalankan `git commit`, `git push`, atau membuat branch** kecuali diminta
   eksplisit.

## Skill mana untuk pekerjaan apa

| Pekerjaan | Skill |
|---|---|
| Token, layout, penampilan visual dari desain | `stitch-to-compose` |
| Navigasi, state, batas ViewModel, keamanan sesi | `compose-architecture` |
| Layer data/domain, Hilt, UseCase, Repository, MVI | `android-architecture-patterns` |
| Endpoint umum, envelope, token auth, idempotency | `bca-mobile-api` |
| API client flow buka rekening (`/onboarding/*`) | `buka-rekening-api` |
| Layar Pilih Jenis Rekening + katalog `onboarding/products` | `buka-rekening-pilih-jenis-rekening` |
| CameraX, ML Kit OCR/face, permission, RSA Keystore | `buka-rekening-native-android` |
| WebRTC, signaling, antrean video call | `buka-rekening-video-call` |
| Performa, ANR, recomposition, memory, R8 | `performance-quality` |

Aturan di dalam skill **jangan disalin ke sini** — cukup rujukan.

## Dokumen kontrak

| Isi | Berkas — kontrak API hidup di repo `bca-mobile-api`, **tidak** disalin ke sini |
|---|---|
| Kontrak API onboarding | `bca-mobile-api/docs/06-BUKA-REKENING-API-SPEC.md` |
| Base URL & endpoint per lingkungan | repo `bca-mobile-api` → `docs/10-BASE-URL-DAN-ENDPOINT.md` |
| Pilih kartu: kontrak, lalu prompt | `bca-mobile-api/docs/08-…-API-SPEC.md`, `docs/backend-prompts/09-…` |
| Skill backend OTP onboarding | `bca-mobile-api/.claude/skills/buka-rekening-otp/` |
| Playbook integrasi Android | `docs/buka-rekening-android-prompts.md`, `…-pilih-kartu-android-prompts.md` |
| Inventaris layar & komponen | `.claude/skills/stitch-to-compose/references/screen-inventory.md` |
| Struktur navigasi (mengikat) | `.claude/skills/compose-architecture/references/navigation.md` |

## Perintah

```bash
./gradlew assembleLocalDebug         # build (assembleDebug = keempat flavor, lambat)
./gradlew testLocalDebugUnitTest     # unit test
./gradlew lintLocalDebug             # android lint
./scripts/check-hardcoded-ui.sh      # cek literal visual di luar file token
```

`check-hardcoded-ui.sh` berbasis baseline (`scripts/ui-baseline.txt`, saat ini **0
pelanggaran**) dan hanya menolak penambahan baru. Jalankan sebelum melaporkan pekerjaan UI
selesai. **Jangan** memakai `--update-baseline` untuk melewati kegagalan — itu hanya untuk
mengunci perbaikan saat angkanya turun.

## Memperbarui file ini

Wajib diperbarui **di commit yang sama** ketika:

- menambah atau menyambungkan endpoint API → perbarui §Integrasi API dan tabel status
- menambah layar atau route → perbarui §Navigasi dan `navigation.md`
- menambah paket baru di `id.bca.bcamobile` → perbarui §Struktur paket
- menambah skill di `.claude/skills/` → perbarui tabel §Skill mana untuk pekerjaan apa
- menambah dependency, permission, atau `buildConfigField` → sebut di §Project atau §Integrasi API

Yang ditulis di sini hanya **peta dan status**. Penjelasan panjang tempatnya di skill atau `docs/`.

## Batasan

- Jangan menambah dependency baru tanpa persetujuan.
- Jangan mengubah `build.gradle.kts`, `settings.gradle.kts`, `libs.versions.toml`, atau
  konfigurasi Gradle tanpa diminta.
- Jangan menyimpan API key, token, atau kredensial di file yang ter-commit.
- Jangan menaruh aset referensi desain di `res/` — tempatnya `docs/design/`.
