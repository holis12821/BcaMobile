# Prompt Instructions — Android Buka Rekening Implementation

> Instruksi step-by-step untuk AI agent (Claude/Cursor) mengimplementasi
> seluruh flow buka rekening di Android. Jalankan satu prompt per sesi.
> Setiap prompt menghasilkan deliverable yang bisa di-review sebelum lanjut.

Referensi skill yang wajib dibaca agent sebelum mulai:
- `.claude/skills/buka-rekening-native-android/SKILL.md`
- `.claude/skills/buka-rekening-video-call/SKILL.md`
- `.claude/skills/compose-architecture/SKILL.md`
- `.claude/skills/bca-mobile-api/SKILL.md`
- `docs/backend/06-BUKA-REKENING-API-SPEC.md`

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

Baca skill bca-mobile-api dan docs/backend/06-BUKA-REKENING-API-SPEC.md.

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