# Prompt Instructions — Integrasi API Android BCA Mobile

> Playbook implementasi untuk AI agent. Satu prompt per sesi, tiap prompt
> menghasilkan deliverable yang bisa di-review sebelum lanjut.
>
> **Bagian A** — flow buka rekening (sebagian besar sudah berjalan).
> **Bagian B** — domain lain aplikasi: auth, beranda, mutasi, transfer, e-wallet,
> QRIS, notifikasi. Semuanya **belum tersambung**.

## Referensi wajib sebelum mulai

| Isi | Berkas |
|---|---|
| Kontrak API seluruh aplikasi | `bca-mobile-api/docs/01-API-SPECIFICATION.md` |
| Kontrak API onboarding | `bca-mobile-api/docs/06-BUKA-REKENING-API-SPEC.md` |
| Sisipan pilih kartu Paspor | `bca-mobile-api/docs/08-PILIH-KARTU-API-SPEC.md` |
| Pola client umum: envelope, token, PIN encryption, idempotency | `.claude/skills/bca-mobile-api/SKILL.md` |
| Pola client onboarding | `.claude/skills/buka-rekening-api/SKILL.md` |
| Clean Architecture + MVI + Hilt | `.claude/skills/android-architecture-patterns/SKILL.md` |
| CameraX, ML Kit, RSA Keystore | `.claude/skills/buka-rekening-native-android/SKILL.md` |
| WebRTC dan signaling | `.claude/skills/buka-rekening-video-call/SKILL.md` |
| Navigasi, batas ViewModel, sesi | `.claude/skills/compose-architecture/SKILL.md` |
| Token visual | `.claude/skills/stitch-to-compose/SKILL.md` |

---

# Peta workflow API seluruh aplikasi

Status di bawah hasil pembacaan kode, bukan rencana. Perbarui tiap kali satu baris berubah.

| Domain | Endpoint utama | Layar | Status |
|---|---|---|---|
| Onboarding | `/v1/onboarding/*` | 13 layar buka rekening | **Tersambung** kecuali video call |
| Pilih kartu | `GET /products/{type}/cards`, `PUT /sessions/{id}/card` | `BukaRekeningPilihKartuScreen` | Belum — data dari `strings.xml` |
| Video call | `POST /video-call/queue` + WS signaling | Antrean, Video Call | Antrean tersambung; WebRTC belum jadi dependency |
| Auth | `/v1/auth/login/pin`, `/login/biometric`, `/token/refresh`, `/logout` | Login, Kode Akses, Face ID, Touch ID | API + repository **selesai**; `KodeAksesViewModel` masih buffer lokal tanpa Hilt |
| Account | `/v1/account/dashboard`, `/balance`, `/profile`, `/settings`, `/transaction-limit` | Beranda, Akun | API + repository **selesai**; layar belum disambungkan |
| Mutasi & riwayat | `/v1/transactions/mutations`, `/history`, `/{id}/receipt` | Mutasi, Riwayat, Rentang Waktu, Bukti Transaksi | API + repository **selesai**; Riwayat & Rentang Waktu masih placeholder teks di `MainGraph.kt` |
| Transfer | `/v1/transfer/recent`, `/inquiry`, `/execute` | Transfer, Transfer Antar Rekening | API + repository **selesai**; layar belum disambungkan |
| E-Wallet | `/v1/ewallet/providers`, `/inquiry`, `/topup` | Top Up, Konfirmasi, Bukti | API + repository **selesai**; layar belum disambungkan |
| QRIS | `/v1/qris/decode`, `/pay` | FAB scan | Layar belum ada |
| Notifikasi | `/v1/notifications` | — | Layar belum ada |

---

# Fondasi bersama — kerjakan sebelum domain mana pun

Lapisan jaringan yang ada sekarang **dibangun khusus untuk onboarding**. Menyambung domain
lain tanpa membereskan lima hal ini akan menghasilkan tambalan yang saling bertabrakan.

### 1. Base URL tunggal yang terkunci ke onboarding

`NetworkModule.provideRetrofit` memakai `BuildConfig.ONBOARDING_BASE_URL`, yaitu
`…/v1/onboarding/`. `Retrofit` disediakan sebagai `@Singleton` **tanpa qualifier**, jadi
`AuthApi` yang dibuat dari instance itu akan menembak `…/v1/onboarding/auth/login/pin`.

Perbaikannya: pisahkan dengan `@Qualifier` (`@OnboardingRetrofit`, `@AppRetrofit`) dan
tambahkan `buildConfigField` kedua untuk base URL `…/v1/`. Jangan menempel path absolut di
anotasi Retrofit untuk mengakalinya — itu menyembunyikan masalah, bukan menyelesaikannya.

### 2. Header otorisasi kurang — di Bagian A maupun Bagian B

Ini bukan sekadar "Bagian B butuh token". Onboarding sendiri sudah kekurangan header.

`01-API-SPECIFICATION.md:57-63` menetapkan header standar `Authorization`, `X-Device-ID`,
`X-Request-ID`, dan `X-Idempotency-Key`. Yang benar-benar dikirim client hanya:

```text
Accept, X-Client-Platform, X-Client-Version     ← NetworkModule.kt:42-44
X-Idempotency-Key                                ← OnboardingApi.kt:129, submit saja
```

Tiga akibatnya:

- **`X-Device-ID` tidak pernah dikirim.** `device_id` hanya masuk *body* `createSession`
  (`OnboardingRepositoryImpl.kt:63`), jadi 14 endpoint lain tidak membawa identitas
  perangkat. `06-BUKA-REKENING-API-SPEC.md` §0 sekarang mewajibkannya di semua request,
  dan `08-PILIH-KARTU-API-SPEC.md` §4 mewajibkannya untuk katalog kartu yang tanpa sesi.
- **`X-Request-ID` tidak pernah dikirim**, jadi log client dan server tidak bisa
  dikorelasikan saat menelusuri kegagalan.
- **`session_id` adalah satu-satunya otorisasi onboarding**, tanpa pengikatan perangkat.
  Siapa pun yang memperolehnya bisa melanjutkan pendaftaran orang lain dari perangkat
  berbeda. `06` §0 menutup celah ini lewat `ONBOARDING_DEVICE_MISMATCH`.

Ketiadaan `Authorization: Bearer` di onboarding **memang benar** — nasabah belum punya
akun. Tapi onboarding bukan tanpa kredensial: `signaling_url` di `06` §5a membawa JWT
untuk WebSocket, dan `QueueTicket` sudah menampungnya meski belum dipakai.

Bagian B berbeda lagi: semua endpointnya butuh access token, jadi perlu interceptor auth
terpisah + `Authenticator` untuk refresh — keduanya belum ada.

### 3. `ApiCaller` dan `OnboardingError` masih milik onboarding

Keduanya ada di `data/onboarding/`. Dipakai lintas domain berarti memindahkannya ke
`core/network/` dan menggeneralisasi nama error. Putuskan sekali di Prompt 9, jangan
menyalin berkasnya per domain.

### 4. Dua sumber kunci RSA

Onboarding mengambil kunci publik dari `GET /credentials/public-key`. Skill `bca-mobile-api`
menyebut kunci PIN ditaruh di `assets`. `RsaEncryptor` sendiri sudah generik — ia menerima
PEM, jadi bisa dipakai keduanya. Yang perlu diputuskan: dari mana PEM untuk PIN transaksi
diambil. Tanyakan sebelum menulis kode.

### 5. Dua keluarga endpoint untuk buka rekening

`01-API-SPECIFICATION.md` §9 mendefinisikan `/v1/registration/*` dengan **Registration
Token**, sementara `06-BUKA-REKENING-API-SPEC.md` mendefinisikan `/v1/onboarding/*` dengan
`session_id`. Kode client mengikuti yang kedua. Belum ada yang merekonsiliasi keduanya —
lihat daftar keputusan terbuka di `06` §0. Jangan menambah endpoint registrasi sebelum ini
dijawab.

### Pola yang berbeda antar bagian

| | Bagian A (onboarding) | Bagian B (domain lain) |
|---|---|---|
| Sumber navigasi maju | `current_step` dari server | Hasil aksi pengguna |
| Bentuk state | Satu `FlowState` untuk 13 layar | Satu ViewModel per layar |
| Transaksi finansial | Tidak ada | `inquiry → PIN verify → execute` |
| Auth | `session_id` + `X-Device-ID`, tanpa access token | Access token + refresh |

Jangan menyeret pola `AdvanceTo(step)` ke Bagian B. Di luar onboarding tidak ada step
machine di server — memaksakannya membuat navigasi bergantung pada sesuatu yang tidak ada.

---

# Bagian A — Buka Rekening

## Status implementasi

Prompt 1-4 dan 6-8 **sudah dikerjakan**. Pola di bawah ini sudah ada di kode —
ikuti, jangan bikin pola tandingan.

| Prompt | Status | Catatan |
|---|---|---|
| 1 — FlowViewModel | Selesai | `BukaRekeningFlowViewModel` + kontrak MVI |
| 2 — CameraX | Selesai | `CameraCapture`, gerbang izin, auto-capture berbasis NIK |
| 3 — ML Kit OCR | Selesai | `KtpTextRecognizer` + `KtpParser`, 12 tes |
| 4 — Face liveness | Selesai | `LivenessDetector` + `LivenessAnalyzer`, 3 tantangan |
| 5 — WebRTC | Belum | butuh persetujuan dependency WebRTC |
| 6 — Enkripsi kredensial | Selesai | `RsaEncryptor`, RSA-OAEP-SHA256 |
| 7 — Integrasi API | Selesai | Retrofit + Hilt + OkHttp, seluruh endpoint |
| 8 — Permission & error | Selesai | gerbang izin kamera + error handling menyeluruh |

---

## Pola ViewModel yang dipakai (wajib diikuti)

Satu ViewModel untuk seluruh flow, bukan satu per layar. Sebelas layar berbagi
satu sesi onboarding, jadi state-nya juga harus satu.

### Berkas

```
ui/screen/buka_rekening/
├── BukaRekeningFlowContract.kt   // State + Event + SideEffect
├── BukaRekeningFlowViewModel.kt  // @HiltViewModel, onEvent(), state, sideEffect
├── BukaRekeningFlowMappers.kt    // error -> teks, OCR -> PersonalData, validasi
└── BukaRekeningUiStates.kt       // FlowState -> UiState tiap layar
```

### Tiga aturan yang tidak boleh dilanggar

**1. Satu pintu masuk.** Layar tidak memanggil method ViewModel satu per satu;
semuanya lewat `onEvent(BukaRekeningEvent)`.

```kotlin
onLanjutClick = { viewModel.onEvent(BukaRekeningEvent.PersonalDataSubmitted) }
```

**2. Navigasi maju datang dari server, bukan dari tombol.** Layar tidak
memanggil `navigate()` setelah aksi yang menyentuh API. ViewModel mengirim
`BukaRekeningSideEffect.AdvanceTo(step)` memakai `current_step` dari response,
dan `AuthGraph` yang menavigasi. Ini yang mencegah user lompat ke step yang
server belum setujui.

```kotlin
is DataResult.Success -> {
    _state.update { it.copy(otpSentTo = result.value.otpSentTo) }
    _sideEffect.send(BukaRekeningSideEffect.AdvanceTo(OnboardingStep.OTP_VERIFY))
}
```

Perpindahan yang murni UI — Pilih Jenis ke S&K, Panduan ke Kamera — tetap boleh
`navigate()` langsung karena tidak menyentuh server.

**3. Layar tetap stateless.** Composable tidak pernah menerima ViewModel.
`AuthGraph` menurunkan `UiState` tiap layar dari `BukaRekeningFlowState`:

```kotlin
val viewModel = bukaRekeningViewModel(navController)
val state by viewModel.state.collectAsState()
BukaRekeningSideEffects(viewModel, navController)

BukaRekeningDataPribadiScreen(
    state = state.toDataPribadiUiState(),
    onLanjutClick = { viewModel.onEvent(BukaRekeningEvent.PersonalDataSubmitted) },
    ...
)
```

### Scope ViewModel

Di-scope ke back stack entry layar pertama flow, bukan ke `Graph.Auth` dan bukan
per layar:

```kotlin
@Composable
private fun bukaRekeningViewModel(navController: NavHostController): BukaRekeningFlowViewModel {
    val parentEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(BukaRekening)
    }
    return hiltViewModel(parentEntry)
}
```

Akibatnya instance ikut dibuang saat `BukaRekening` lepas dari back stack —
PII di memory ikut hilang tanpa perlu pembersihan manual.

### Error

`OnboardingError` (domain) diklasifikasi di `ApiCaller`, lalu diterjemahkan ke
`ErrorText` oleh `OnboardingError.toErrorText()`. Pesan yang kita tulis sendiri
lewat `strings.xml`; pesan bisnis dari server dipakai apa adanya.

Dua error ditangani ViewModel, bukan layar:
- `SessionExpired` / `SessionNotFound` → state direset, `RestartFlow`
- `InvalidStep` → GET session, lalu `AdvanceTo(current_step)`

### Yang masih kurang

- **Slot error per layar.** Hanya `BukaRekeningPilihJenisUiState` yang punya
  field `isLoading` dan `error`. Dua belas layar lain belum, jadi `isLoading`
  dan pesan error tidak terlihat di sana. Menambahkannya mengubah signature
  composable — kerjakan bersama pemilik desain.
- **Layar input OTP.** Backend punya step `OTP_VERIFY` antara `PERSONAL_DATA`
  dan `BIOMETRIC`, tapi layarnya belum ada di desain. `verifyOtp` dan
  `resendOtp` sudah siap di ViewModel dan repository.
- **Picker pekerjaan, penghasilan, sumber dana.** Layar Data Pribadi
  menampilkannya tapi belum punya callback perubahan, jadi masih memakai nilai
  default di `BukaRekeningFlowMappers.kt`.
- **Nomor HP dan email** belum punya input di layar mana pun, sehingga dikirim
  kosong ke `POST /personal-data`. Server mengirim OTP ke nomor itu, jadi layar
  Data Pribadi perlu dua field ini sebelum flow OTP bisa dipakai sungguhan.
- **Video call (Prompt 5).** `VideoCallUiState` masih statis; `signaling_url`
  dari `QueueTicket` belum dipakai.

---

## Kamera, OCR, dan liveness

### Pembagian peran OCR

OCR berjalan **dua kali**, dan itu disengaja:

| Di mana | Kapan | Untuk apa |
|---|---|---|
| Perangkat (ML Kit) | Segera setelah menjepret | Menampilkan hasil seketika, dan menolak foto buruk sebelum diunggah |
| Server (`POST /ocr`) | Saat "Gunakan Foto" ditekan | Hasil resmi + pencocokan Dukcapil |

Hasil lokal **bukan** sumber kebenaran. Begitu server menjawab, `state.ocr`
menimpa `state.localScan`, dan `BukaRekeningFlowState.ktpData` otomatis
menunjuk ke data server. Foto mentah tidak pernah dikirim ke layanan OCR pihak
ketiga — hanya ke backend BCA.

Penyaringan lokal ini juga menjaga kuota: `POST /ocr` dibatasi 10 kali per jam
per sesi, jadi foto dengan akurasi di bawah 80% ditandai `needsRetake` dan tidak
dihabiskan percuma.

### Slot preview, bukan layar yang tahu kamera

Layar tetap stateless. `BukaRekeningKameraFotoScreen` dan
`BukaRekeningVerifikasiBiometrikScreen` masing-masing menerima satu parameter
opsional:

```kotlin
cameraPreview: (@Composable () -> Unit)? = null
```

Kalau null — di `@Preview`, atau saat izin kamera belum ada — wireframe bawaan
yang tampil, jadi tata letak tidak bergeser. Bingkai, sudut, oval, dan badge
tetap digambar layar; preview hanya mengisi latarnya.

### Berkas sementara

Foto e-KTP, foto wajah, dan frame liveness ditulis ke `cacheDir` lalu dihapus
lewat `CameraCapture.discard` begitu unggahan selesai — berhasil maupun gagal.
Tidak ada satu pun yang menyentuh penyimpanan eksternal.

### Auto-capture

Toggle "Auto-Capture" memasang `KtpAutoCaptureAnalyzer` pada aliran frame.
Pemicunya bukan sekadar "ada tulisan", melainkan **NIK 16 digit terbaca** —
penanda paling andal bahwa kartu sudah tegak, fokus, dan tidak terpotong.

### Liveness

Tiga tantangan berurutan di `LivenessDetector`: wajah di tengah, kedip
(`eyeOpenProbability` turun di bawah 0.3 lalu naik di atas 0.7), lalu kepala
menoleh lebih dari 15 derajat. Satu frame bukti disimpan tiap tantangan lewat,
sehingga terkumpul 3 frame sesuai kontrak `POST /biometric`.

Anti-spoofing di perangkat ini hanya penyaring awal — keputusan akhir tetap
milik backend. Lebih dari satu wajah di bingkai membuat frame ditolak.

---

## Prompt 1: BukaRekeningFlowViewModel — Shared State

```
Buat BukaRekeningFlowViewModel yang di-scope ke navigation graph buka rekening.

Baca skill compose-architecture dan buka-rekening-native-android dulu.

Requirements:
1. Buat data class BukaRekeningFlowState yang menyimpan SEMUA data lintas screen:
   - selectedProduct (dari screen Pilih Jenis)
   - ocrResult (dari screen Hasil Foto)
   - personalData (dari screen Data Pribadi)
   - otpVerified (boolean)
   - biometricResult (dari screen Biometrik)
   - videoCallResult (dari screen Video Call)
   - credentialsSet (boolean)
   - sessionId (dari backend)

2. ViewModel methods:
   - initSession(productType) → call POST /onboarding/sessions
   - setOcrResult(result)
   - savePersonalData(data) → call POST /onboarding/personal-data
   - verifyOtp(code) → call POST /onboarding/verify-otp
   - setBiometricResult(result) → call POST /onboarding/biometric
   - setVideoCallResult(result)
   - saveCredentials(accessCode, pin) → call POST /onboarding/credentials
   - submitApplication() → call POST /onboarding/submit
   - resumeSession(sessionId) → call GET /onboarding/sessions/{id}

3. Setiap screen composable mendapat:
   - Read-only slice dari flowState via derived StateFlow
   - Lambda callbacks yang memanggil ViewModel methods

4. Error handling: per-screen error state, bukan global

5. Scope ViewModel ke buka rekening navigation graph:
   val flowViewModel: BukaRekeningFlowViewModel = hiltViewModel(
       viewModelStoreOwner = navController.getBackStackEntry(BukaRekeningGraphRoute)
   )

File yang dibuat:
- BukaRekeningFlowViewModel.kt
- BukaRekeningFlowState.kt
- OnboardingRepository.kt (interface + impl)

Jangan sentuh file screen composable yang sudah ada. Hanya buat ViewModel dan repository.
```

---

## Prompt 2: CameraX Integration — Foto e-KTP

```
Integrasikan CameraX ke BukaRekeningKameraFotoScreen untuk ambil foto e-KTP.

Baca skill buka-rekening-native-android bagian CameraX dulu.

Requirements:
1. Tambahkan permission CAMERA di AndroidManifest.xml

2. Buat CameraController wrapper:
   - startCamera(lifecycleOwner, previewView)
   - capturePhoto() → return File (temp file di cacheDir)
   - setFlashMode(mode: FlashMode)
   - toggleAutoCapture(enabled: Boolean)

3. Buat permission gate composable:
   - Check CAMERA permission
   - Jika belum: minta runtime permission
   - Jika ditolak: tampilkan explainer + tombol ke Settings
   - Jika granted: tampilkan kamera

4. Update KameraFotoScreen:
   - Ganti mockup Box dengan AndroidView { PreviewView }
   - Wire shutter button ke capturePhoto()
   - Wire flash toggle ke setFlashMode()
   - Setelah capture: navigate ke HasilFoto dengan file path

5. Auto-capture (optional): gunakan ImageAnalysis use case
   untuk detect apakah dokumen sudah fully visible di frame.
   Jika ya, auto-trigger capturePhoto().

Pastikan:
- Kamera di-release saat screen hilang dari backstack
- Preview mirror = false (bukan selfie)
- Aspect ratio sesuai KTP (landscape crop guide)
- Photo resolution minimal 1920x1080
```

---

## Prompt 3: ML Kit OCR — Parse e-KTP Indonesia

```
Integrasikan ML Kit Text Recognition untuk OCR e-KTP Indonesia.

Baca skill buka-rekening-native-android bagian ML Kit OCR dulu.

Requirements:
1. Buat KtpOcrProcessor:
   - Input: foto e-KTP (Bitmap atau URI)
   - Process via TextRecognizer
   - Parse hasil OCR menjadi KtpOcrResult data class

2. Buat KtpParser:
   - Parsing logic khusus format KTP Indonesia
   - Extract: NIK (16 digit), Nama, Tempat Lahir, Tanggal Lahir,
     Jenis Kelamin, Alamat, RT/RW, Kel, Kec, Kota, Provinsi,
     Agama, Status Perkawinan
   - Hitung accuracy score berdasarkan berapa field yang berhasil di-extract
   - Validasi format NIK (16 digit, kode wilayah valid)

3. Update HasilFotoScreen:
   - Terima photo file path dari KameraFotoScreen
   - Jalankan OCR processing (show loading indicator)
   - Tampilkan hasil OCR di field-field yang sudah ada
   - "Gunakan Foto Ini" → kirim foto + OCR result ke backend via ViewModel

4. Update DataPribadiScreen:
   - Auto-fill field dari OCR result (via shared ViewModel)
   - Field yang dari OCR: readonly + badge "OCR e-KTP"
   - Field tambahan (pekerjaan, penghasilan): user input

Pastikan:
- OCR processing on-device (ML Kit bundled model)
- Show loading state saat processing
- Jika accuracy < 80%: tampilkan warning + opsi ambil ulang
- Mask NIK di semua log output
```

---

## Prompt 4: Face Liveness Detection

```
Implementasi face liveness detection untuk verifikasi biometrik.

Baca skill buka-rekening-native-android bagian Face Liveness dulu.

Requirements:
1. Buat LivenessDetector:
   - Gunakan ML Kit FaceDetector (PERFORMANCE_MODE_ACCURATE)
   - Challenge sequence: detect face → blink → (optional head turn)
   - Capture frame di setiap challenge yang berhasil
   - Anti-spoofing basic: cek variasi eye open probability antar frame

2. Buat BiometrikViewModel:
   - State: faceDetected, currentChallenge, challengeProgress, precisionScore
   - Method: startDetection(), onFrameAnalyzed(image), submitBiometric()

3. Update VerifikasiBiometrikScreen:
   - Ganti mockup dengan AndroidView { PreviewView } (front camera)
   - Overlay: animated oval frame guide
   - Real-time feedback: "Wajah Terdeteksi", "Kedipkan Mata"
   - Progress indicator per challenge

4. Permission: reuse CAMERA permission (sudah diminta di screen Kamera)
   Tapi tetap check ulang jika user revoke di Settings.

5. Setelah liveness pass:
   - Capture main face photo
   - Kirim face_photo + liveness_frames ke backend via ViewModel
   - Backend compare face dengan foto KTP (face matching)
   - Navigate ke Antrean Video Call

Pastikan:
- Gunakan front camera (LENS_FACING_FRONT)
- FaceDetector options: classification ALL, landmark ALL
- Blink detection: leftEyeOpenProb < 0.3 && rightEyeOpenProb < 0.3
  diikuti > 0.7 dalam 500ms
- Min face size 30% dari frame
- JANGAN simpan face data di device setelah upload
```

---

## Prompt 5: WebRTC Video Call — Full Implementation

```
Implementasi video call e-KYC dengan WebRTC native.

Baca skill buka-rekening-video-call/SKILL.md LENGKAP sebelum mulai.
Ini adalah prompt paling kompleks. Ikuti struktur di skill secara ketat.

Requirements:
1. Buat SignalingClient (OkHttp WebSocket):
   - connect(url), disconnect()
   - send: offer, answer, ice_candidate, media_control
   - receive: queue_update, agent_assigned, answer, ice_candidate,
     instruction, call_ended, error
   - Auto-reconnect dengan exponential backoff

2. Buat WebRtcClient:
   - PeerConnectionFactory setup
   - createPeerConnection() dengan ICE/TURN servers
   - startLocalVideo(surfaceViewRenderer) — front camera
   - createOfferAndSend()
   - onAnswerReceived(sdp)
   - ICE candidate exchange
   - toggleMute(), switchCamera(), endCall()

3. Buat AntreanVideoCallViewModel:
   - joinQueue() → POST /onboarding/video-call/queue → connect WebSocket
   - Handle queue_update events → update position + ETA
   - Handle agent_assigned → trigger WebRTC setup
   - Handle operating hours check

4. Buat VideoCallViewModel:
   - Manage WebRtcClient lifecycle
   - State: connectionState, agentName, callDuration, isMuted, instruction
   - Call duration timer (formatted MM:SS)
   - Handle call_ended → navigate ke Buat Kredensial

5. Update AntreanVideoCallScreen:
   - Permission check (CAMERA + RECORD_AUDIO)
   - Real-time queue position dari WebSocket
   - "Tunggu Panggilan" → joinQueue()

6. Update VideoCallScreen:
   - Replace mockup dengan real VideoRenderer (AndroidView + SurfaceViewRenderer)
   - PiP layout: agent besar (full screen), nasabah kecil (bottom-right corner)
   - Real mute/unmute/switch-camera controls
   - Agent instruction text overlay
   - Connection status indicator (Terhubung / Menghubungkan...)

7. Lifecycle:
   - ViewModel scope: PeerConnection survive rotation
   - ON_PAUSE: pause local video
   - ON_RESUME: resume local video
   - App killed: connection lost, user rejoin queue

8. Security:
   - FLAG_SECURE pada window saat video call aktif
   - DTLS-SRTP (default WebRTC, jangan disable)
   - WSS only (bukan WS)

File yang dibuat/dimodifikasi:
- SignalingClient.kt (interface + OkHttpSignalingClient)
- WebRtcClient.kt
- AntreanVideoCallViewModel.kt
- VideoCallViewModel.kt
- Update AntreanVideoCallScreen.kt (wire to ViewModel)
- Update VideoCallScreen.kt (wire to ViewModel + real video)
```

---

## Prompt 6: Credential Encryption & Validation

```
Implementasi validasi dan enkripsi kredensial (kode akses + PIN).

Baca skill buka-rekening-native-android bagian Credential Encryption.

Requirements:
1. Buat CredentialValidator:
   - validateAccessCode(code: String): List<ValidationResult>
     Rules: 6 chars, alphanumeric, not sequential, not all same
   - validatePin(pin: String, accessCode: String): List<ValidationResult>
     Rules: 6 digits, not sequential, not all same, different from access code
   - isSequential(input: String): Boolean — detect abc123, 654321, etc.
   - isAllSame(input: String): Boolean — detect aaaaaa, 111111

2. Buat CredentialEncryptor:
   - fetchServerPublicKey() → RSA PublicKey (from /auth/public-key)
   - encrypt(plaintext: String, publicKey: PublicKey): String (Base64)
   - Algoritma: RSA/ECB/OAEPWithSHA-256AndMGF1Padding

3. Buat BuatKredensialViewModel:
   - State: validation results (real-time), field values, visibility toggles
   - Method: onAccessCodeChange(), onPinChange(), saveCredentials()
   - saveCredentials() → encrypt → POST /onboarding/credentials

4. Update BuatKredensialScreen:
   - Wire ke ViewModel (bukan hardcoded state)
   - Real-time validation indicators (green check / red cross per rule)
   - "Simpan Kredensial" disabled sampai semua validasi pass
   - PIN input: masked by default, toggle visibility

Pastikan:
- Credential JANGAN masuk log (bahkan encrypted form)
- Clear credential dari memory setelah submit
- RSA public key cache di memory saja (BUKAN disk)
- Strength indicator visual sesuai jumlah rules yang pass
```

---

## Prompt 7: API Integration — Wire All Screens

```
Hubungkan semua screen ke backend API via BukaRekeningFlowViewModel.

Baca skill bca-mobile-api dan bca-mobile-api/docs/06-BUKA-REKENING-API-SPEC.md.

Requirements:
1. Buat OnboardingApiService (Retrofit interface):
   - createSession(request): Response<SessionResponse>
   - uploadOcr(sessionId, photo, meta): Response<OcrResponse>
   - savePersonalData(request): Response<PersonalDataResponse>
   - verifyOtp(request): Response<OtpResponse>
   - uploadBiometric(sessionId, face, frames, meta): Response<BiometricResponse>
   - joinVideoCallQueue(request): Response<QueueResponse>
   - saveCredentials(request): Response<CredentialResponse>
   - submitApplication(request): Response<SubmitResponse>
   - getSession(sessionId): Response<SessionResponse>

2. Buat OnboardingRepositoryImpl:
   - Implement OnboardingRepository interface
   - Map API responses ke domain models
   - Handle errors: network, timeout, business logic
   - Idempotency key management

3. Wire setiap screen ke ViewModel:
   - Screen 1 (Pilih Jenis): onJenisSelected → viewModel.initSession(product)
   - Screen 5 (Hasil Foto): onGunakanFoto → viewModel.uploadOcr(photo)
   - Screen 6 (Data Pribadi): onLanjutClick → viewModel.savePersonalData(data)
   - Screen 7 (Biometrik): onMulaiClick → viewModel.submitBiometric(face, frames)
   - Screen 8A (Antrean): onTungguClick → viewModel.joinQueue()
   - Screen 9 (Kredensial): onSimpanClick → viewModel.saveCredentials(code, pin)
   - Screen 10 (Ringkasan): onProsesClick → viewModel.submitApplication()
   - Screen 11 (Berhasil): data dari submitApplication response

4. Loading & error states:
   - Setiap API call: tampilkan loading indicator
   - Error: tampilkan di Snackbar atau inline error
   - Retry button untuk transient errors

5. Resume draft:
   - Saat app dibuka: check apakah ada session aktif
   - Jika ada: tampilkan dialog "Lanjutkan pendaftaran?"
   - Navigate ke screen sesuai current_step dari API

Jangan ubah layout/visual screen. Hanya wire data flow.
```

---

## Prompt 8: Runtime Permissions & Error Handling

```
Tambahkan runtime permission handling dan comprehensive error handling
ke seluruh flow buka rekening.

Requirements:
1. AndroidManifest.xml:
   - Tambah: CAMERA, RECORD_AUDIO, INTERNET, ACCESS_NETWORK_STATE
   - Tambah uses-feature: camera, camera.front, microphone

2. Permission gate pattern:
   - Screen 4 (Kamera): gate CAMERA permission
   - Screen 7 (Biometrik): gate CAMERA permission (re-check)
   - Screen 8A→8B (Video Call): gate CAMERA + RECORD_AUDIO

3. Permission denied handling:
   - shouldShowRationale → show dialog explaining why needed
   - Permanently denied → show "Buka Pengaturan" button
     Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)

4. Network error handling:
   - No internet → show offline banner + retry button
   - Timeout → show retry button
   - Server error (500) → show generic error + retry
   - Business error (422) → show specific message from API

5. Session expiry handling:
   - 401 from any endpoint → session expired
   - Show dialog "Sesi Anda telah berakhir"
   - Navigate back to Pilih Jenis, start fresh

6. Back press handling:
   - Show confirmation dialog saat user back dari flow:
     "Keluar dari pendaftaran? Data Anda akan tersimpan sebagai draf."
   - "Simpan Draf" → save current state via API
   - "Keluar" → navigate back

7. Process death recovery:
   - Save sessionId di EncryptedSharedPreferences
   - On app restart: check session, resume if valid
   - Do NOT save sensitive data (credentials, photos) to disk

Pastikan semua error messages menggunakan strings.xml (bukan hardcoded).
```

---

# Bagian B — Domain lain

Kerjakan berurutan. Prompt 9 adalah prasyarat semua prompt sesudahnya.

---

## Prompt 9: Fondasi jaringan lintas domain

```
Siapkan lapisan jaringan supaya bisa melayani domain di luar onboarding.
Baca .claude/skills/bca-mobile-api/SKILL.md bagian 2, 3, 4, dan 9 dulu.

1. Pisahkan base URL:
   - Tambah buildConfigField APP_BASE_URL (debug staging, release produksi),
     berakhir di /v1/
   - Buat @Qualifier @OnboardingRetrofit dan @AppRetrofit di NetworkModule
   - OnboardingApi tetap memakai retrofit onboarding; jangan ubah endpointnya

1b. Lengkapi header standar di KEDUA interceptor — onboarding maupun aplikasi.
   Sekarang yang terkirim hanya Accept, X-Client-Platform, X-Client-Version.
   Kontrak 01-API-SPECIFICATION.md:57-63 dan 06-BUKA-REKENING-API-SPEC.md §0
   mewajibkan juga:
   - X-Device-ID  → ambil dari OnboardingSessionStore.deviceId(), sumber yang
     sama dengan device_id di body createSession. Nilainya HARUS sama, karena
     server menolak dengan ONBOARDING_DEVICE_MISMATCH kalau berbeda.
     Pindahkan deviceId() ke core/ kalau dipakai lintas domain — jangan
     membuat generator device id kedua.
   - X-Request-ID → UUID v4 baru per request, bukan per sesi.
   Interceptor onboarding tetap TIDAK mengirim Authorization; interceptor
   aplikasi yang mengirimnya (langkah 4).
   Tambahkan test yang memastikan X-Device-ID yang terkirim identik dengan
   device_id yang dipakai createSession.

2. Pindahkan ApiCaller dari data/onboarding/remote/ ke core/network/ dan
   generalisasi OnboardingError menjadi ApiError domain yang dipakai bersama.
   Pertahankan kebijakan retry yang sudah ada: jaringan 3x (1s/3s/5s),
   5xx 2x, 429 hormati Retry-After, 4xx lain tidak diulang.
   Semua pemanggil onboarding harus tetap kompilasi dan tesnya tetap hijau.

3. Buat TokenManager (Hilt @Singleton):
   - access token di memory saja
   - refresh token di EncryptedSharedPreferences (pakai pola OnboardingSessionStore)
   - clear() saat logout

4. Buat AuthInterceptor (menyisipkan Authorization: Bearer) dan
   TokenAuthenticator (refresh sekali saat 401, antre request lain selama
   refresh berjalan, logout bila refresh gagal).

5. Buat IdempotencyKeyProvider: satu kunci per percobaan transaksi, dipakai
   ulang untuk setiap retry percobaan yang sama.

JANGAN menyentuh layar mana pun di prompt ini. Deliverable: lapisan jaringan +
unit test untuk refresh 401 (sukses, gagal, dan dua request bersamaan) + test
kesamaan X-Device-ID dengan device_id.

Catatan: pengikatan perangkat membuat draf onboarding tidak bisa dilanjutkan
setelah aplikasi dipasang ulang, karena ANDROID_ID berubah. Itu konsekuensi yang
sudah diketahui dan tercatat sebagai keputusan terbuka nomor 3 di 06 §0 —
laporkan, jangan diakali sendiri.
```

---

## Prompt 10: Auth — Login, Kode Akses, Biometrik

```
Sambungkan flow masuk ke /v1/auth/*.
Baca bca-mobile-api/docs/01-API-SPECIFICATION.md bagian 2 dan skill compose-architecture.

1. AuthApi: login/pin, login/biometric, biometric/challenge, biometric/register,
   token/refresh, logout, pin/change, pin/verify.

2. PIN dan kode akses dikirim terenkripsi RSA-OAEP-SHA256. Pakai RsaEncryptor
   yang sudah ada di core/security — jangan bikin encryptor kedua.

3. KodeAksesViewModel sekarang ViewModel biasa tanpa Hilt dan tanpa repository.
   Ubah jadi @HiltViewModel dengan AuthRepository, pertahankan perilaku
   buffer digit dan shake error yang sudah ada.

4. Login biometrik dua langkah: GET challenge → tanda tangan dengan kunci
   AndroidKeyStore → POST login/biometric. Sambungkan ke FaceIdScreen dan
   FingerPrintScreen.

5. Sukses login: simpan token lewat TokenManager, panggil
   SessionRepository.authenticate(), lalu navigasi ke Graph.Main dengan
   popUpTo yang membersihkan Graph.Auth — ikuti navigation.md, jangan improvisasi.

6. Logout: POST /auth/logout, TokenManager.clear(), SessionRepository.logout().

Layar tetap stateless. Pesan error lewat strings.xml.
```

---

## Prompt 11: Beranda, Saldo, dan Akun

```
Sambungkan Beranda dan Akun ke /v1/account/*.

1. AccountApi: dashboard, balance, profile (GET/PUT), settings,
   transaction-limit.

2. HomeViewModel memuat GET /account/dashboard sekali saat layar masuk
   komposisi, dan menyediakan refresh manual. Saldo tersembunyi secara default;
   menampilkannya memanggil GET /account/balance, bukan menyimpan saldo di state
   yang dipersistensi.

3. AkunScreen: profil, pengaturan, dan ubah limit transaksi.

4. Dashboard gagal dimuat tidak boleh mengosongkan layar — nomor rekening dan
   menu tetap tampil, hanya bagian saldo yang menampilkan "Gagal memuat" +
   "Coba Lagi".

Jangan ubah layout. Hanya aliran data.
```

---

## Prompt 12: Mutasi, Riwayat, dan Bukti Transaksi

```
Sambungkan mutasi ke /v1/transactions/*.
Baca skill bca-mobile-api bagian 10 (pagination cursor-based).

1. TransactionApi: mutations, history, {id}/receipt, {id}/receipt/pdf.

2. Pagination memakai cursor, bukan nomor halaman. Muat halaman berikutnya
   saat pengguna mendekati ujung daftar. Jangan memuat semua sekaligus.

3. Riwayat dan Rentang Waktu di MainGraph.kt masih kotak teks placeholder —
   layarnya belum ada. Bangun keduanya bersama pemilik desain sebelum
   menyambungkan API; jangan mengarang tata letaknya sendiri.

4. Rentang Waktu mengirim filter tanggal ke GET /transactions/mutations.

5. BuktiTransaksiScreen memakai GET /transactions/{id}/receipt. Tombol simpan
   memakai endpoint /receipt/pdf.

6. Daftar panjang: pakai key stabil di LazyColumn dan hindari recomposition
   seluruh daftar — lihat skill performance-quality.
```

---

## Prompt 13: Transfer — inquiry, verifikasi PIN, eksekusi

```
Implementasi transfer mengikuti pola tiga langkah di skill bca-mobile-api bagian 8.1.

1. TransferApi: recent, inquiry, execute. PIN verify memakai AuthApi.pin/verify.

2. Urutan wajib: POST /transfer/inquiry (hasil berlaku 5 menit) →
   POST /auth/pin/verify (verification_token berlaku 120 detik) →
   POST /transfer/execute dengan X-Idempotency-Key.

3. Kedua masa berlaku itu harus terlihat di UI. Inquiry kedaluwarsa berarti
   mengulang dari langkah satu, bukan mengirim ulang execute.

4. Idempotency key dibuat sekali per percobaan transfer dan dipakai ulang untuk
   setiap retry. Retry setelah timeout TIDAK BOLEH menghasilkan dua transfer.

5. Sukses → BuktiTransaksiScreen memakai data dari response execute.

6. PIN tidak boleh masuk log, bahkan dalam bentuk terenkripsi, dan dibersihkan
   dari memory setelah dipakai.
```

---

## Prompt 14: E-Wallet Top Up

```
Implementasi top up e-wallet, pola sama dengan transfer.
Baca skill bca-mobile-api bagian 8.2.

1. EWalletApi: providers, inquiry, topup.

2. GET /ewallet/providers mengisi daftar penyedia beserta min/max amount,
   admin fee, dan preset nominal. Jangan menanam daftar penyedia di client.

3. Nominal preset dan batas min/max datang dari server — validasi input
   memakai nilai itu, bukan angka tetap.

4. Urutan: providers → inquiry → PIN verify → topup (X-Idempotency-Key).

5. Alur layar mengikuti EWalletGraph yang sudah ada: Pilih → Nominal → PIN →
   Bukti. Perbarui navigation.md bila urutannya berubah.
```

---

## Prompt 15: QRIS dan Notifikasi

```
Dua fitur yang layarnya belum ada. Kerjakan setelah domain lain stabil.

1. QRIS: POST /qris/decode → tampilkan merchant → PIN verify → POST /qris/pay
   dengan X-Idempotency-Key. Pemindaian memakai CameraX yang sudah terpasang;
   izin kamera memakai gerbang yang sama dengan foto e-KTP.

2. Notifikasi: GET /notifications dengan pagination cursor,
   PUT /{id}/read dan PUT /read-all.

Layar untuk keduanya belum ada di desain maupun di screen-inventory.md.
Minta desainnya dulu — jangan membuat tata letak sendiri.
```

---

## Prompt 16: Pilih Kartu Paspor (sisipan onboarding)

```
Sambungkan BukaRekeningPilihKartuScreen ke katalog kartu.

Instruksi lengkapnya sudah ada di docs/buka-rekening-pilih-kartu-android-prompts.md
(enam prompt: DTO, domain+repository, ViewModel, UI state, navigasi, uji).
Kontrak backend: bca-mobile-api/docs/08-PILIH-KARTU-API-SPEC.md.

Dua hal yang paling mudah salah:
- API mengirim style BLUE/GOLD/PLATINUM, bukan warna. Pemetaan ke token CardArt
  sudah ada di layar.
- Nominal datang sebagai Long, diformat di client. Daftar di strings.xml turun
  pangkat jadi fallback offline, tidak dihapus.
```

---

# Urutan pengerjaan

```text
Prompt 9 (fondasi jaringan)
   ├─→ Prompt 10 (auth) ──→ Prompt 11 (beranda/akun)
   │                          └─→ Prompt 12 (mutasi/riwayat/bukti)
   │                                └─→ Prompt 13 (transfer)
   │                                      └─→ Prompt 14 (e-wallet)
   │                                            └─→ Prompt 15 (QRIS/notifikasi)
   └─→ Prompt 16 (pilih kartu) — tidak bergantung auth, boleh paralel
Prompt 5 (WebRTC) — tertahan persetujuan dependency
```

Prompt 10 sampai 15 semuanya menyentuh token akses, jadi menjalankannya sebelum Prompt 9
selesai berarti menulis ulang lapisan jaringannya dua kali.

## Setelah tiap prompt

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./scripts/check-hardcoded-ui.sh
./gradlew assembleDebug
```

Perbarui juga, di commit yang sama:

- `CLAUDE.md` — tabel status integrasi per fitur
- `.claude/skills/compose-architecture/references/navigation.md` — bila route berubah
- `.claude/skills/stitch-to-compose/references/screen-inventory.md` — bila ada layar baru
- tabel **Peta workflow API** di dokumen ini
