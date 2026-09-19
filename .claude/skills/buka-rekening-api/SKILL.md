---
name: buka-rekening-api
description: Android client API integration untuk seluruh flow buka rekening BCA — session lifecycle, endpoint reference (/onboarding/*), request/response contract, OCR upload multipart, personal data + OTP verification, biometric upload, video call queue + WebSocket signaling protocol, credential encryption RSA-OAEP, final submit + idempotency, error handling strategy, dan step-by-step implementation prompts. Gunakan saat membuat atau memodifikasi API client onboarding, Retrofit interface, DTO request/response, repository implementation, atau error handling buka rekening. JANGAN gunakan untuk backend Go (itu skill buka-rekening-backend), endpoint auth/login/PIN (itu skill auth), atau endpoint transaksi (itu skill transaction).
---

# Skill: Buka Rekening — Android Native Integration

Android native client untuk seluruh flow pembukaan rekening baru BCA.
Integrasi dengan backend API `/v1/onboarding/*` mencakup: session lifecycle,
camera capture + OCR preview, form data pribadi + OTP input, face liveness
capture, video call queue + WebRTC, credential encryption, dan final review.

**Trigger**: saat menyentuh screen/fragment onboarding, API client buka rekening,
camera capture, liveness SDK, WebRTC/WebSocket signaling, RSA encryption untuk
credential, atau state management flow buka rekening di Android.

**Jangan trigger** untuk: backend Go service (itu skill `buka-rekening-backend`),
endpoint auth/login/PIN (itu skill `auth`), endpoint transaksi/transfer (itu
skill `transaction`).

> **Referensi backend:**
> ```text
> docs/06-BUKA-REKENING-API-SPEC.md   -- API contract lengkap (request/response/error codes)
> .claude/skills/buka-rekening-backend/SKILL.md  -- Backend skill & arsitektur
> ```

---

## Arsitektur Client

### Tech Stack

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt (Dagger)
- **Networking**: Retrofit + OkHttp + Moshi
- **Image**: CameraX (capture KTP + liveness frames)
- **WebRTC**: Google WebRTC SDK (`org.webrtc`)
- **WebSocket**: OkHttp WebSocket
- **Crypto**: Android Keystore + Bouncy Castle (RSA-OAEP-SHA256)
- **Navigation**: Jetpack Navigation Component (single-activity)
- **State**: ViewModel + StateFlow / SavedStateHandle

### Module Structure

```text
app/
├── src/main/java/id/co/bca/mobile/
│   ├── onboarding/
│   │   ├── data/
│   │   │   ├── api/
│   │   │   │   └── OnboardingApi.kt          -- Retrofit interface
│   │   │   ├── dto/
│   │   │   │   ├── CreateSessionRequest.kt
│   │   │   │   ├── CreateSessionResponse.kt
│   │   │   │   ├── OCRResponse.kt
│   │   │   │   ├── PersonalDataRequest.kt
│   │   │   │   ├── VerifyOTPRequest.kt
│   │   │   │   ├── BiometricResponse.kt
│   │   │   │   ├── JoinQueueResponse.kt
│   │   │   │   ├── SetCredentialsRequest.kt
│   │   │   │   ├── SubmitRequest.kt
│   │   │   │   └── SubmitResponse.kt
│   │   │   └── repository/
│   │   │       └── OnboardingRepositoryImpl.kt
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── OnboardingSession.kt
│   │   │   │   ├── OnboardingStep.kt         -- enum matching backend Steps
│   │   │   │   └── ProductInfo.kt
│   │   │   ├── repository/
│   │   │   │   └── OnboardingRepository.kt   -- interface
│   │   │   └── usecase/
│   │   │       ├── CreateSessionUseCase.kt
│   │   │       ├── ProcessOCRUseCase.kt
│   │   │       ├── SavePersonalDataUseCase.kt
│   │   │       ├── VerifyOTPUseCase.kt
│   │   │       ├── ProcessBiometricUseCase.kt
│   │   │       ├── JoinVideoCallUseCase.kt
│   │   │       ├── SetCredentialsUseCase.kt
│   │   │       └── SubmitOnboardingUseCase.kt
│   │   └── presentation/
│   │       ├── OnboardingActivity.kt         -- single Activity host
│   │       ├── OnboardingViewModel.kt        -- shared VM for session state
│   │       ├── product/
│   │       │   └── ProductSelectionFragment.kt
│   │       ├── tnc/
│   │       │   └── TNCFragment.kt
│   │       ├── ocr/
│   │       │   ├── KTPCaptureFragment.kt     -- CameraX capture
│   │       │   └── OCRResultFragment.kt      -- review extracted data
│   │       ├── personaldata/
│   │       │   ├── PersonalDataFormFragment.kt
│   │       │   └── OTPVerifyFragment.kt
│   │       ├── biometric/
│   │       │   └── LivenessFragment.kt       -- face capture + liveness
│   │       ├── videocall/
│   │       │   ├── QueueFragment.kt          -- waiting room
│   │       │   └── VideoCallFragment.kt      -- WebRTC peer connection
│   │       ├── credentials/
│   │       │   └── SetCredentialsFragment.kt -- access code + PIN input
│   │       ├── review/
│   │       │   └── ReviewFragment.kt         -- final review before submit
│   │       └── result/
│   │           └── SuccessFragment.kt        -- account created
│   └── core/
│       ├── network/
│       │   ├── ApiClient.kt                  -- OkHttp + Retrofit setup
│       │   └── ApiEnvelope.kt                -- {"status","data","error","meta"}
│       └── crypto/
│           └── RSAEncryptor.kt               -- RSA-OAEP-SHA256 encrypt
```

### Session State Machine (Client-Side)

```text
TNC -> OCR -> PERSONAL_DATA -> OTP_VERIFY -> BIOMETRIC -> VIDEO_CALL -> CREDENTIALS -> REVIEW -> COMPLETED
```

- Client tracks `current_step` dari response API
- Navigasi fragment berdasarkan `current_step`
- Resume flow: GET `/sessions/{id}` saat app reopen, navigate ke step terakhir
- Session expire 24 jam (rolling) -- tampilkan countdown timer

---

## API Integration Contract

### Base URL

```text
Production : https://api.bcamobile.id/v1/onboarding
Staging    : https://api-staging.bcamobile.id/v1/onboarding
```

### Response Envelope

Semua response mengikuti format standar:

```json
{
  "status": "success",
  "data": { ... },
  "meta": {
    "request_id": "req_...",
    "timestamp": "2026-09-19T10:30:00Z"
  }
}
```

Error response:

```json
{
  "status": "error",
  "error": {
    "code": "ONBOARDING_SESSION_EXPIRED",
    "message": "Sesi pendaftaran sudah berakhir. Silakan mulai ulang.",
    "details": null
  },
  "meta": { ... }
}
```

### Retrofit Interface

```kotlin
interface OnboardingApi {

    // Session
    @POST("sessions")
    suspend fun createSession(@Body req: CreateSessionRequest): ApiEnvelope<CreateSessionResponse>

    @GET("sessions/{session_id}")
    suspend fun getSession(@Path("session_id") id: String): ApiEnvelope<GetSessionResponse>

    @DELETE("sessions/{session_id}")
    suspend fun cancelSession(@Path("session_id") id: String): ApiEnvelope<DeleteResponse>

    // OCR
    @Multipart
    @POST("ocr")
    suspend fun processOCR(
        @Part("session_id") sessionId: RequestBody,
        @Part ktpPhoto: MultipartBody.Part,
        @Part("flash_used") flashUsed: RequestBody,
        @Part("auto_captured") autoCaptured: RequestBody,
        @Part("resolution") resolution: RequestBody
    ): ApiEnvelope<OCRResponse>

    @GET("ocr/{session_id}")
    suspend fun getOCRResult(@Path("session_id") id: String): ApiEnvelope<OCRResponse>

    // Personal Data + OTP
    @POST("personal-data")
    suspend fun savePersonalData(@Body req: SavePersonalDataRequest): ApiEnvelope<SavePersonalDataResponse>

    @POST("verify-otp")
    suspend fun verifyOTP(@Body req: VerifyOTPRequest): ApiEnvelope<VerifyOTPResponse>

    @POST("resend-otp")
    suspend fun resendOTP(@Body req: ResendOTPRequest): ApiEnvelope<ResendOTPResponse>

    // Biometric
    @Multipart
    @POST("biometric")
    suspend fun processBiometric(
        @Part("session_id") sessionId: RequestBody,
        @Part facePhoto: MultipartBody.Part,
        @Part livenessFrames: List<MultipartBody.Part>,
        @Part("liveness_meta") livenessMeta: RequestBody
    ): ApiEnvelope<BiometricResponse>

    // Video Call
    @POST("video-call/queue")
    suspend fun joinQueue(@Body req: JoinQueueRequest): ApiEnvelope<JoinQueueResponse>

    // Credentials
    @GET("credentials/public-key")
    suspend fun getPublicKey(): ApiEnvelope<PublicKeyResponse>

    @POST("credentials")
    suspend fun setCredentials(@Body req: SetCredentialsRequest): ApiEnvelope<SetCredentialsResponse>

    // Submit
    @POST("submit")
    suspend fun submit(
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Body req: SubmitRequest
    ): ApiEnvelope<SubmitResponse>
}
```

---

## Endpoint Reference (Full Contract)

### 1. Create Session

```text
POST /sessions
```

**Request:**

```json
{
  "product_type": "TAHAPAN_BCA",
  "device_id": "d_abc123",
  "accepted_tnc_version": "2026-09-01"
}
```

- `product_type`: `TAHAPAN_BCA` | `TAHAPAN_XPRESI` | `TABUNGANKU`
- `device_id`: unique device identifier (Android ID atau fingerprint)
- `accepted_tnc_version`: versi S&K yang disetujui

**Response 201:**

```json
{
  "session_id": "onb_9f8e7d6c5b4a",
  "product": {
    "type": "TAHAPAN_BCA",
    "name": "Tahapan BCA",
    "currency": "IDR",
    "min_initial_deposit": 500000,
    "features": ["Paspor BCA Mastercard Debit", "m-BCA", "KlikBCA"]
  },
  "current_step": "OCR",
  "expires_at": "2026-09-20T10:30:00Z"
}
```

**Error codes:** `ONBOARDING_PRODUCT_UNAVAILABLE`, `ONBOARDING_SESSION_LIMIT`

**Rate limit:** 3 session per device per jam

---

### 2. OCR -- Upload Foto KTP

```text
POST /ocr
Content-Type: multipart/form-data
```

**Request (multipart):**

| Field | Type | Keterangan |
| ----- | ---- | ---------- |
| `session_id` | string | ID session aktif |
| `ktp_photo` | file (JPEG/PNG) | Foto e-KTP, maks 10 MB |
| `flash_used` | string | `"true"` / `"false"` |
| `auto_captured` | string | `"true"` / `"false"` |
| `resolution` | string | e.g. `"1920x1080"` |

**Response 200:**

```json
{
  "ocr_id": "ocr_a1b2c3d4",
  "accuracy_percent": 99.4,
  "extracted": {
    "nik": "3174082104950001",
    "nama_lengkap": "MUHAMMAD ARDAN PRAYOGI",
    "tempat_lahir": "Jakarta",
    "tanggal_lahir": "1995-04-21",
    "jenis_kelamin": "LAKI_LAKI",
    "alamat": "Jl. Sudirman Kav. 45 No. 12B",
    "rt_rw": "003/005",
    "kelurahan": "Senayan",
    "kecamatan": "Kebayoran Baru",
    "kota": "Jakarta Selatan",
    "provinsi": "DKI Jakarta",
    "agama": "Islam",
    "status_perkawinan": "Belum Kawin"
  },
  "dukcapil_match": true,
  "photo_quality": {
    "sharpness": "HIGH",
    "glare_detected": false,
    "all_corners_visible": true
  },
  "current_step": "PERSONAL_DATA"
}
```

**Error codes:** `OCR_PHOTO_BLURRY`, `OCR_GLARE_DETECTED`, `OCR_CORNERS_MISSING`, `OCR_NOT_KTP`, `OCR_DUKCAPIL_MISMATCH`, `OCR_DUKCAPIL_TIMEOUT`

**Rate limit:** 10 per jam per session

**Android notes:**
- Gunakan CameraX untuk capture foto KTP
- Auto-capture dengan edge detection (opsional, gunakan ML Kit)
- Compress JPEG quality 85% sebelum upload
- Tampilkan overlay guide (frame KTP) di camera preview
- Flash toggle button di UI

---

### 3. Get OCR Result

```text
GET /ocr/{session_id}
```

**Response 200:** sama seperti response POST `/ocr`

Gunakan untuk resume flow -- ambil data OCR jika user kembali ke step review.

---

### 4. Save Personal Data

```text
POST /personal-data
```

**Request:**

```json
{
  "session_id": "onb_9f8e7d6c5b4a",
  "ocr_id": "ocr_a1b2c3d4",
  "personal_data": {
    "nik": "3174082104950001",
    "nama_lengkap": "MUHAMMAD ARDAN PRAYOGI",
    "tempat_lahir": "Jakarta",
    "tanggal_lahir": "1995-04-21",
    "jenis_kelamin": "LAKI_LAKI",
    "alamat_ktp": {
      "alamat_lengkap": "Jl. Sudirman Kav. 45 No. 12B",
      "rt_rw": "003/005",
      "kode_pos": "12190",
      "kelurahan": "Senayan",
      "kecamatan": "Kebayoran Baru",
      "kota": "Jakarta Selatan",
      "provinsi": "DKI Jakarta"
    },
    "alamat_domisili_sama": true,
    "pekerjaan": "KARYAWAN_SWASTA",
    "penghasilan_per_bulan": "10_20_JUTA",
    "sumber_dana_utama": "GAJI",
    "nomor_hp": "081234568889",
    "email": "m.ardan@example.com"
  }
}
```

**Enum values:**

- `pekerjaan`: `KARYAWAN_SWASTA` | `PNS` | `TNI_POLRI` | `WIRASWASTA` | `PROFESIONAL` | `PELAJAR_MAHASISWA` | `IBU_RUMAH_TANGGA` | `LAINNYA`
- `penghasilan_per_bulan`: `DIBAWAH_5_JUTA` | `5_10_JUTA` | `10_20_JUTA` | `20_50_JUTA` | `DIATAS_50_JUTA`
- `sumber_dana_utama`: `GAJI` | `USAHA` | `INVESTASI` | `WARISAN` | `LAINNYA`

**Response 200:**

```json
{
  "personal_data_id": "pd_x1y2z3",
  "otp_sent_to": "0812****8889",
  "otp_expires_at": "2026-09-18T10:35:00Z",
  "current_step": "OTP_VERIFY"
}
```

**Error codes:** `PERSONAL_DATA_INVALID_PHONE`, `PERSONAL_DATA_INVALID_EMAIL`, `PERSONAL_DATA_NIK_MISMATCH`, `PERSONAL_DATA_NAMA_MISMATCH`

**Android notes:**
- Pre-fill form dari data OCR `extracted`
- NIK dan nama_lengkap read-only (harus match OCR)
- Validasi format nomor HP di client: regex `^(\+62|62|0)8[0-9]{8,12}$`
- Validasi format email di client
- Dropdown/spinner untuk pekerjaan, penghasilan, sumber_dana

---

### 5. Verify OTP

```text
POST /verify-otp
```

**Request:**

```json
{
  "session_id": "onb_9f8e7d6c5b4a",
  "otp_code": "847291"
}
```

**Response 200:**

```json
{
  "verified": true,
  "current_step": "BIOMETRIC"
}
```

**Error codes:**

| Code | Keterangan | Client action |
| ---- | ---------- | ------------- |
| `OTP_INVALID` | Kode OTP salah | Tampilkan pesan error, user bisa coba lagi |
| `OTP_EXPIRED` | OTP sudah expired atau di-regenerate setelah 3 gagal | Tampilkan pesan "OTP baru sudah dikirim" |
| `OTP_BLOCKED` | Terblokir 30 menit setelah 5 gagal | Tampilkan countdown dari `details.retry_after_seconds` |

**OTP_BLOCKED error response:**

```json
{
  "status": "error",
  "error": {
    "code": "OTP_BLOCKED",
    "message": "Terlalu banyak percobaan. Silakan coba lagi nanti.",
    "details": {
      "retry_after_seconds": 1740
    }
  }
}
```

**Android notes:**
- Input 6 digit OTP dengan auto-focus per digit
- SMS auto-read via SMS Retriever API (opsional)
- Countdown timer 5 menit untuk OTP expiry
- Tombol "Kirim Ulang OTP" (call POST `/resend-otp`)
- Jika `OTP_BLOCKED`, disable input dan tampilkan countdown

---

### 6. Resend OTP

```text
POST /resend-otp
```

**Request:**

```json
{
  "session_id": "onb_9f8e7d6c5b4a"
}
```

**Response 200:**

```json
{
  "otp_sent_to": "0812****8889",
  "otp_expires_at": "2026-09-18T10:40:00Z"
}
```

---

### 7. Biometric -- Face Liveness

```text
POST /biometric
Content-Type: multipart/form-data
```

**Request (multipart):**

| Field | Type | Keterangan |
| ----- | ---- | ---------- |
| `session_id` | string | ID session |
| `face_photo` | file (JPEG) | Foto wajah utama |
| `liveness_frames` | file[] (JPEG) | 3-5 frame challenge (kedip, gerak kepala) |
| `liveness_meta` | JSON string | Metadata liveness challenge |

**`liveness_meta` format:**

```json
{
  "challenge_type": "BLINK",
  "completed_actions": 3,
  "precision_score": 98.2
}
```

**Response 200:**

```json
{
  "biometric_id": "bio_m1n2o3",
  "liveness_verified": true,
  "liveness_score": 98.2,
  "face_match_with_ktp": true,
  "face_match_score": 96.7,
  "iso_30107_compliant": true,
  "current_step": "VIDEO_CALL"
}
```

**Error codes:** `BIO_LIVENESS_FAILED`, `BIO_FACE_NOT_MATCH`, `BIO_MULTIPLE_FACES`, `BIO_LOW_QUALITY`, `BIO_SPOOF_DETECTED`

**Rate limit:** 5 per jam per session

**Android notes:**
- Gunakan CameraX front camera untuk face capture
- Minimum 3, maksimum 5 liveness frames
- Challenge flow: tampilkan instruksi "Kedipkan mata", "Gerakkan kepala ke kiri", dll.
- Capture frames otomatis saat user menyelesaikan challenge
- Face detection via ML Kit untuk memastikan 1 wajah terdeteksi sebelum upload
- Pencahayaan check -- warn user jika terlalu gelap
- Compress frame JPEG quality 80% sebelum upload

---

### 8. Video Call -- Join Queue

```text
POST /video-call/queue
```

**Request:**

```json
{
  "session_id": "onb_9f8e7d6c5b4a"
}
```

**Response 200:**

```json
{
  "queue_id": "q_abc123",
  "queue_number": "A-042",
  "position": 2,
  "estimated_wait_seconds": 360,
  "operating_hours": {
    "start": "06:00",
    "end": "22:00",
    "timezone": "Asia/Jakarta"
  },
  "signaling_url": "wss://signal.bcamobile.id/v1/onboarding/video-call/signal?token=eyJ..."
}
```

**Operating hours:** 06:00-22:00 WIB. Jika di luar jam operasional, tampilkan pesan dan jam buka berikutnya.

---

### 9. WebSocket Signaling Protocol

```text
WS {signaling_url dari JoinQueueResponse}
```

**Connect** ke URL dari `signaling_url`. Token JWT ada di query param `?token=`.

#### Client -> Server Messages

```json
{"type": "join", "session_id": "onb_...", "queue_id": "q_abc123"}
```

```json
{"type": "offer", "sdp": "v=0\r\no=- ..."}
```

```json
{"type": "ice_candidate", "candidate": {"candidate": "...", "sdpMid": "0", "sdpMLineIndex": 0}}
```

```json
{"type": "media_control", "action": "mute_audio"}
```

Actions: `mute_audio`, `unmute_audio`, `switch_camera`

#### Server -> Client Messages

```json
{"type": "queue_update", "position": 1, "estimated_wait_seconds": 60}
```

```json
{"type": "agent_assigned", "agent": {"name": "Sarah Adisti", "employee_id": "CS-1042", "photo_url": "..."}}
```

```json
{"type": "answer", "sdp": "v=0\r\no=- ..."}
```

```json
{"type": "ice_candidate", "candidate": {...}}
```

```json
{"type": "instruction", "text": "Mohon tunjukkan e-KTP asli Anda ke kamera"}
```

```json
{"type": "call_ended", "result": "APPROVED", "agent_name": "Sarah Adisti", "duration_seconds": 195}
```

**Android notes:**
- Gunakan OkHttp WebSocket client
- Implement reconnect logic (max 3 retry, backoff 1s/3s/5s)
- Grace period: jika disconnect < 60 detik, server masih hold posisi
- Queue waiting screen: tampilkan queue_number, position, estimated wait
- Saat `agent_assigned`: init WebRTC PeerConnection, kirim `offer`
- Saat `answer`: set remote SDP description
- Exchange ICE candidates bidirectional
- Saat `instruction`: tampilkan instruksi agent di overlay text
- Saat `call_ended`: jika `result == "APPROVED"`, navigate ke credentials step
- Jika `result == "REJECTED"`, tampilkan pesan dan opsi retry

---

### 10. Get Encryption Public Key

```text
GET /credentials/public-key
```

**Response 200:**

```json
{
  "algorithm": "RSA-OAEP-SHA256",
  "key_id": "pin-key-v1",
  "public_key_pem": "-----BEGIN PUBLIC KEY-----\nMIIB..."
}
```

**Dev mode response** (jika server belum configure RSA key):

```json
{
  "algorithm": "RSA-OAEP-SHA256",
  "key_id": "dev-mode",
  "public_key_pem": "",
  "note": "Dev mode: send plaintext credentials (no encryption needed)."
}
```

---

### 11. Set Credentials

```text
POST /credentials
```

**Request:**

```json
{
  "session_id": "onb_9f8e7d6c5b4a",
  "access_code_encrypted": "BASE64_RSA_OAEP_ENCRYPTED...",
  "pin_encrypted": "BASE64_RSA_OAEP_ENCRYPTED...",
  "encryption_key_id": "pin-key-v1"
}
```

**Response 200:**

```json
{
  "credential_id": "cred_p1q2r3",
  "biometric_login_available": true,
  "current_step": "REVIEW"
}
```

**Error codes:** `CRED_WEAK_ACCESS_CODE`, `CRED_WEAK_PIN`, `CRED_SAME_AS_ACCESS_CODE`, `CRED_DECRYPTION_FAILED`

**Client-side validation (sebelum encrypt + kirim):**
- Access code: exactly 6 alphanumeric, not all same char, not sequential
- PIN: exactly 6 digits, not all same digit, not sequential
- PIN != access code

**RSA encryption flow:**
1. Fetch public key dari `GET /credentials/public-key`
2. Parse PEM ke `PublicKey` object
3. Encrypt access_code dengan RSA-OAEP-SHA256
4. Encrypt PIN dengan RSA-OAEP-SHA256
5. Base64-encode ciphertext
6. Kirim `encryption_key_id` dari response public key

---

### 12. Final Submit

```text
POST /submit
X-Idempotency-Key: <uuid>
```

**Request:**

```json
{
  "session_id": "onb_9f8e7d6c5b4a",
  "agreement_accepted": true,
  "agreement_version": "2026-09-01"
}
```

**Response 201:**

```json
{
  "account": {
    "account_number": "5420891234",
    "account_type": "TAHAPAN_BCA",
    "account_holder": "MUHAMMAD ARDAN PRAYOGI",
    "branch": "KCU Jakarta Thamrin",
    "branch_code": "0539",
    "currency": "IDR",
    "status": "ACTIVE",
    "min_initial_deposit": 500000,
    "initial_deposit_deadline": "2026-10-18T23:59:59Z"
  },
  "m_bca": {
    "user_id": "mbca_s1t2u3",
    "access_code_set": true,
    "pin_set": true
  },
  "created_at": "2026-09-18T10:45:00Z"
}
```

**Error codes:** `ONBOARDING_INCOMPLETE`, `ONBOARDING_SESSION_EXPIRED`, `ACCOUNT_CREATION_FAILED`

**Android notes:**
- Generate `X-Idempotency-Key` sebagai UUID v4 dan simpan di `SavedStateHandle`
- Jika network error, retry dengan key yang sama (server return response yang sama)
- Tampilkan loading full-screen saat submit
- Disable back button saat loading
- Setelah sukses, tampilkan detail rekening baru + info setoran awal

---

### 13. Get Session (Resume)

```text
GET /sessions/{session_id}
```

**Response 200:**

```json
{
  "session_id": "onb_9f8e7d6c5b4a",
  "product": {
    "type": "TAHAPAN_BCA",
    "name": "Tahapan BCA",
    "currency": "IDR",
    "min_initial_deposit": 500000,
    "features": ["Paspor BCA Mastercard Debit", "m-BCA", "KlikBCA"]
  },
  "current_step": "VIDEO_CALL",
  "steps_completed": {
    "tnc_accepted": true,
    "ocr_verified": true,
    "personal_data_saved": true,
    "otp_verified": true,
    "biometric_verified": true,
    "video_call_verified": false,
    "credentials_set": false,
    "submitted": false
  },
  "created_at": "2026-09-18T09:00:00Z",
  "expires_at": "2026-09-19T09:00:00Z"
}
```

**Android notes:**
- Simpan `session_id` di `SharedPreferences` (encrypted)
- Saat app dibuka, cek apakah ada active session
- Jika ada, GET session dan navigate ke `current_step`
- Jika expired (`ONBOARDING_SESSION_EXPIRED`), clear saved session, tampilkan dialog

---

### 14. Cancel Session

```text
DELETE /sessions/{session_id}
```

**Response 200:**

```json
{
  "deleted": true
}
```

**Android notes:**
- Konfirmasi dialog sebelum cancel
- Clear saved session_id dari preferences
- Navigate back ke product selection

---

## Error Handling Strategy

### Global Error Codes (semua endpoint)

| Code | HTTP | Client Action |
| ---- | ---- | ------------- |
| `VALIDATION_ERROR` | 400 | Tampilkan field-level error |
| `ONBOARDING_NOT_FOUND` | 404 | Session tidak ditemukan, clear local dan mulai ulang |
| `ONBOARDING_SESSION_EXPIRED` | 422 | Dialog "Sesi berakhir", tombol "Mulai Ulang" |
| `ONBOARDING_INVALID_STEP` | 422 | Step tidak sesuai, GET session ulang dan navigate |
| `RATE_LIMIT_EXCEEDED` | 429 | Tampilkan pesan "Terlalu banyak percobaan", disable button |
| `INTERNAL_ERROR` | 500 | Generic error dialog, tombol "Coba Lagi" |

### Retry Strategy

```text
Network error  -> retry 3x dengan exponential backoff (1s, 3s, 5s)
HTTP 429       -> retry setelah Retry-After header
HTTP 5xx       -> retry 2x, lalu tampilkan error dialog
HTTP 4xx       -> jangan retry, tampilkan error message ke user
```

---

## Security Requirements

### Credential Encryption (RSA-OAEP-SHA256)

```kotlin
// RSAEncryptor.kt
object RSAEncryptor {

    fun encrypt(plaintext: String, publicKeyPem: String): String {
        val keySpec = X509EncodedKeySpec(parsePem(publicKeyPem))
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)

        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun parsePem(pem: String): ByteArray {
        val base64 = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        return Base64.decode(base64, Base64.DEFAULT)
    }
}
```

### Session Storage

- Simpan `session_id` di `EncryptedSharedPreferences`
- JANGAN simpan data PII (NIK, nama, alamat) di local storage
- Data OCR/personal hanya in-memory di ViewModel, clear on destroy
- Hapus semua data lokal saat cancel atau completed

### Certificate Pinning

```kotlin
// OkHttp certificate pinning
val client = OkHttpClient.Builder()
    .certificatePinner(
        CertificatePinner.Builder()
            .add("api.bcamobile.id", "sha256/AAAA...")
            .add("api-staging.bcamobile.id", "sha256/BBBB...")
            .build()
    )
    .build()
```

---

## Aturan Wajib

1. **Jangan simpan PII di local storage** -- semua data sensitif hanya di memory (ViewModel)
2. **RSA-OAEP-SHA256** untuk encrypt credential sebelum kirim ke server
3. **Idempotency key** wajib untuk POST `/submit` -- simpan di `SavedStateHandle`
4. **Session resume** -- selalu GET session saat app reopen, navigate ke step terakhir
5. **Camera permission** -- request runtime permission sebelum OCR dan biometric step
6. **Network error retry** -- exponential backoff, jangan spam server
7. **TLS 1.3** -- enforce minimum TLS 1.3, certificate pinning
8. **Step validation** -- client navigate berdasarkan `current_step` dari server, bukan local state
9. **Countdown timer** -- tampilkan sisa waktu session (24 jam rolling)
10. **Back navigation** -- user bisa kembali ke step sebelumnya untuk review, tapi tidak bisa skip step

---

# Prompt Instructions -- AI Agent Android Integration

> Instruksi step-by-step untuk AI agent mengimplementasi Android client buka rekening.
> Jalankan satu prompt per sesi, secara berurutan.
> AI agent **wajib** membaca skill ini dan `06-BUKA-REKENING-API-SPEC.md` sebelum mulai.

---

## Prompt 1: Project Setup & API Client

```text
Setup the onboarding module in the Android project with Kotlin.

Requirements:
- Create Retrofit interface OnboardingApi.kt with ALL endpoints listed in the skill
- Create DTO classes for all request/response types matching the API contract exactly
- Create ApiEnvelope<T> wrapper matching backend response format:
  {"status":"success","data":T,"meta":{"request_id","timestamp"}}
  {"status":"error","error":{"code","message","details"}}
- Create OnboardingRepository interface and implementation
- Setup Hilt module for DI (provide OnboardingApi, Repository)
- Create OnboardingStep enum matching backend steps:
  TNC, OCR, PERSONAL_DATA, OTP_VERIFY, BIOMETRIC, VIDEO_CALL, CREDENTIALS, REVIEW, COMPLETED
- Create navigation graph for onboarding flow (nav_onboarding.xml)
- Create OnboardingActivity as single-activity host
- Create shared OnboardingViewModel with:
  - sessionId: String (persisted in SavedStateHandle)
  - currentStep: StateFlow<OnboardingStep>
  - navigateToStep(step) function
  - resumeSession() function (GET /sessions/{id})
  - cancelSession() function

Use encrypted SharedPreferences to persist session_id between app launches.
```

---

## Prompt 2: Product Selection & TNC Screen

```text
Implement product selection and terms & conditions screens.

Requirements:
- ProductSelectionFragment:
  - Display 3 product cards: Tahapan BCA, Tahapan Xpresi, TabunganKu
  - Each card shows: name, min_initial_deposit, features list
  - Check for existing draft session on load (resume flow)
  - On product select: navigate to TNC

- TNCFragment:
  - Display terms & conditions text (WebView or ScrollView)
  - "Saya setuju" checkbox + "Lanjutkan" button
  - On accept: call POST /sessions with product_type, device_id, accepted_tnc_version
  - Save session_id to EncryptedSharedPreferences
  - Navigate to KTPCaptureFragment (OCR step)

- Error handling:
  - ONBOARDING_SESSION_LIMIT: dialog "Anda sudah memiliki 3 sesi aktif"
  - ONBOARDING_PRODUCT_UNAVAILABLE: disable the product card
  - Network error: retry with snackbar
```

---

## Prompt 3: KTP Camera Capture & OCR

```text
Implement KTP photo capture and OCR result review.

Requirements:
- KTPCaptureFragment:
  - CameraX preview with back camera
  - KTP frame overlay guide (rounded rectangle matching KTP aspect ratio)
  - Flash toggle button
  - Capture button (or auto-capture with edge detection)
  - After capture: show preview with "Gunakan Foto" / "Ambil Ulang" buttons
  - On confirm: upload via POST /ocr (multipart)
  - Loading state during upload + OCR processing
  - Handle OCR errors with specific messages:
    - OCR_PHOTO_BLURRY: "Foto terlalu buram. Pastikan KTP dalam fokus."
    - OCR_GLARE_DETECTED: "Terdapat pantulan cahaya. Matikan flash dan coba lagi."
    - OCR_CORNERS_MISSING: "Sudut KTP terpotong. Pastikan seluruh KTP terlihat."
    - OCR_NOT_KTP: "Dokumen bukan e-KTP. Silakan foto e-KTP Anda."
    - All errors: show "Ambil Ulang" button

- OCRResultFragment:
  - Display extracted KTP data in read-only form
  - Show photo quality indicators (sharpness, glare, corners)
  - Show Dukcapil match status
  - "Lanjutkan" button to navigate to PersonalDataFormFragment
  - "Foto Ulang" button to go back to capture

Camera permissions: request at fragment start, explain why needed.
Compress image to JPEG 85% quality before upload.
Max file size: 10 MB.
```

---

## Prompt 4: Personal Data Form & OTP

```text
Implement personal data form and OTP verification.

Requirements:
- PersonalDataFormFragment:
  - Pre-fill NIK and nama_lengkap from OCR data (read-only, greyed out)
  - Editable fields: tempat_lahir, tanggal_lahir (date picker), jenis_kelamin (radio)
  - Address section: alamat_lengkap, rt_rw, kode_pos, kelurahan, kecamatan, kota, provinsi
  - Toggle "Alamat domisili sama dengan KTP" checkbox
  - Dropdown spinners for: pekerjaan, penghasilan_per_bulan, sumber_dana_utama
  - Phone input with format hint "08xxxxxxxxxx"
  - Email input with format validation
  - Client-side validation before submit
  - On submit: call POST /personal-data
  - Navigate to OTPVerifyFragment

- OTPVerifyFragment:
  - 6-digit OTP input (individual EditText per digit, auto-focus next)
  - Display masked phone: "Kode OTP dikirim ke 0812****8889"
  - Countdown timer (5 min from otp_expires_at)
  - "Kirim Ulang" button (disabled during countdown first 30s, then enabled)
  - On submit: call POST /verify-otp
  - Handle errors:
    - OTP_INVALID: shake animation + "Kode OTP salah"
    - OTP_EXPIRED: "OTP baru telah dikirim ke nomor Anda"
    - OTP_BLOCKED: disable input, show countdown from retry_after_seconds
  - On success: navigate to LivenessFragment

Optional: SMS Retriever API for auto-fill OTP.
```

---

## Prompt 5: Biometric Liveness Capture

```text
Implement face liveness verification capture.

Requirements:
- LivenessFragment:
  - CameraX front camera preview (face selfie mode)
  - Face detection overlay (oval guide for face positioning)
  - ML Kit Face Detection to:
    - Ensure exactly 1 face in frame
    - Check face is centered and correct size
    - Detect lighting quality
  - Liveness challenge flow:
    1. "Posisikan wajah dalam frame" -> wait for face detection
    2. "Kedipkan mata" -> capture frame when blink detected
    3. "Gerakkan kepala ke kanan" -> capture frame
    4. "Gerakkan kepala ke kiri" -> capture frame
    5. Auto-capture main face_photo when all challenges complete
  - Capture 3-5 liveness frames during challenges
  - Build liveness_meta JSON from challenge results
  - Upload via POST /biometric (multipart: face_photo + liveness_frames[] + liveness_meta)
  - Loading state during upload + processing
  - Handle errors:
    - BIO_MULTIPLE_FACES: "Pastikan hanya wajah Anda yang terlihat"
    - BIO_LOW_QUALITY: "Pencahayaan tidak cukup. Cari tempat lebih terang."
    - BIO_SPOOF_DETECTED: "Verifikasi gagal. Gunakan wajah asli, bukan foto."
    - BIO_LIVENESS_FAILED: "Deteksi keaktifan gagal. Silakan coba lagi."
    - BIO_FACE_NOT_MATCH: "Wajah tidak cocok dengan foto KTP."
  - On success: navigate to QueueFragment (video call)

Compress frames to JPEG 80% quality.
Request camera permission if not already granted.
```

---

## Prompt 6: Video Call Queue & WebRTC

```text
Implement video call waiting room and WebRTC call.

Requirements:
- QueueFragment (waiting room):
  - Call POST /video-call/queue on entry
  - Display: queue_number ("A-042"), position, estimated wait
  - Check operating hours (06:00-22:00 WIB); if outside, show message + next open time
  - Connect WebSocket to signaling_url
  - Send {"type":"join", "session_id", "queue_id"} after connect
  - Listen for "queue_update" messages, update position + estimated wait
  - On "agent_assigned": transition to VideoCallFragment with agent info
  - Reconnect logic: if disconnect < 60s, auto-reconnect
  - Cancel button: confirm dialog, then DELETE session or just leave queue

- VideoCallFragment (WebRTC call):
  - Initialize PeerConnectionFactory (Google WebRTC SDK)
  - Create PeerConnection with STUN/TURN servers
  - Get local media stream (front camera + audio)
  - Render local video in small PiP view
  - Render remote video in full-screen view
  - On init: create SDP offer, send via signaling {"type":"offer","sdp":"..."}
  - On "answer": set remote SDP description
  - Exchange ICE candidates via signaling
  - Display agent info (name, photo) in header
  - On "instruction": show agent instruction text overlay
  - Mute/unmute toggle button
  - Switch camera button (front/back for showing KTP)
  - On "call_ended":
    - If result == "APPROVED": navigate to SetCredentialsFragment
    - If result == "REJECTED": show dialog with reason, option to retry or cancel
  - Handle WebSocket disconnect during call: show reconnecting overlay

WebSocket: use OkHttp WebSocket client.
WebRTC: use org.webrtc:google-webrtc dependency.
```

---

## Prompt 7: Credential Input & Encryption

```text
Implement access code and PIN setup with RSA encryption.

Requirements:
- SetCredentialsFragment:
  - Step 1: Access Code input
    - 6-character alphanumeric input (masked)
    - Confirm access code (re-enter)
    - Client validation: not all same char, not sequential, exactly 6 alphanum
    - Real-time strength indicator
  - Step 2: PIN input
    - 6-digit numeric input (masked)
    - Confirm PIN (re-enter)
    - Client validation: not all same digit, not sequential, exactly 6 digits
    - PIN != access code check
  - On confirm:
    1. Fetch RSA public key: GET /credentials/public-key
    2. If key_id == "dev-mode": send plaintext (dev only)
    3. Else: encrypt access_code and pin with RSA-OAEP-SHA256
    4. Call POST /credentials with encrypted values + encryption_key_id
    5. On success: navigate to ReviewFragment

- RSAEncryptor utility:
  - Parse PEM public key
  - Encrypt with RSA/ECB/OAEPWithSHA-256AndMGF1Padding
  - Return Base64 encoded ciphertext

Error handling:
  - CRED_WEAK_ACCESS_CODE: highlight access code field with error
  - CRED_WEAK_PIN: highlight PIN field with error
  - CRED_SAME_AS_ACCESS_CODE: "PIN tidak boleh sama dengan kode akses"
  - CRED_DECRYPTION_FAILED: "Gagal enkripsi. Coba lagi." (refetch public key + retry)
```

---

## Prompt 8: Review & Submit

```text
Implement final review and submit screens.

Requirements:
- ReviewFragment:
  - Display summary of all onboarding data:
    - Product info (type, name, features)
    - Personal data (nama, NIK masked, phone masked, email masked)
    - Biometric status: verified checkmark
    - Video call status: verified checkmark, agent name
    - Credentials: "Kode akses dan PIN sudah diset" checkmark
  - Final agreement checkbox
  - Agreement version input (from config or hardcoded)
  - "Buka Rekening" button
  - On submit:
    1. Generate UUID v4 as idempotency key, save to SavedStateHandle
    2. Call POST /submit with X-Idempotency-Key header
    3. Full-screen loading (disable back button)
    4. On success: navigate to SuccessFragment
    5. On network error: retry with SAME idempotency key
    6. On ONBOARDING_INCOMPLETE: dialog listing missing steps

- SuccessFragment:
  - Celebration animation (confetti or checkmark)
  - Display account details:
    - Account number (formatted: "542-089-1234")
    - Account type
    - Branch name
    - Status: ACTIVE
    - Min initial deposit amount
    - Deposit deadline
  - m-BCA info: user_id, access_code_set, pin_set
  - "Setor Awal" button (navigate to transfer/deposit flow)
  - "Ke Beranda" button (navigate to home, clear onboarding state)
  - Clear saved session_id from preferences
  - Clear all in-memory onboarding data

On "Ke Beranda": the user is now a registered nasabah, they can login with
the access code and PIN they just set.
```