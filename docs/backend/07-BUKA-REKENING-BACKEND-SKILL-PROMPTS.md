---
name: buka-rekening-onboarding
description: >-
  Backend onboarding service untuk flow buka rekening BCA — session management,
  OCR e-KTP, verifikasi Dukcapil, biometrik, video call signaling, credential
  hashing, dan account creation. Gunakan saat membuat atau memodifikasi endpoint
  /v1/onboarding/*, service OCR/biometric/video-call, WebSocket signaling server,
  queue management, atau audit trail onboarding. JANGAN gunakan untuk endpoint
  auth (login/PIN), endpoint transaksi (transfer/e-wallet), atau UI/frontend
  Android (itu project terpisah).
---

# Skill: Buka Rekening — Backend Onboarding Service

Backend service untuk seluruh flow pembukaan rekening baru BCA.
Mencakup: session lifecycle, OCR e-KTP + Dukcapil, data pribadi + OTP,
biometrik, video call queue + WebSocket signaling, credential encryption,
account creation, dan audit trail.

**Trigger**: saat menyentuh endpoint `/v1/onboarding/*`, service OCR,
biometric, video call queue, WebSocket signaling server, credential hashing,
session state machine, atau audit logging onboarding.

**Jangan trigger** untuk: endpoint auth/login/PIN (itu skill `auth`),
endpoint transaksi/transfer/e-wallet (itu skill `transaction`),
UI/frontend Android (itu project terpisah dengan skill `buka-rekening-native-android`).

> **Setup di project backend:**
> ```
> .claude/skills/
> ├── buka-rekening-onboarding/
> │   ├── SKILL.md                          ← bagian "Skill Definition" file ini
> │   └── references/
> │       └── prompts.md                    ← bagian "Prompt Instructions" file ini
> └── video-call-ekyc/
>     └── SKILL.md                          ← extract dari Prompt 5 jika perlu skill terpisah
> ```
> Referensi API spec: `docs/06-BUKA-REKENING-API-SPEC.md`

---

## Arsitektur

### Tech Stack
- **Language**: Go (Golang)
- **Database**: PostgreSQL (strong consistency)
- **Cache/Queue**: Redis (session, antrean video call)
- **Object Storage**: S3-compatible (foto KTP, wajah, rekaman video)
- **OCR Engine**: Google Cloud Vision API atau Tesseract + custom Indonesian KTP parser
- **Face Matching**: On-premise model atau cloud API (AWS Rekognition / Google Vision)

### Service Boundaries

```
onboarding-service/
├── handler/          ← HTTP handlers (Gin/Chi)
│   ├── session.go
│   ├── ocr.go
│   ├── personal_data.go
│   ├── biometric.go
│   ├── video_call.go
│   ├── credential.go
│   └── submit.go
├── service/          ← Business logic
│   ├── session_service.go
│   ├── ocr_service.go
│   ├── dukcapil_client.go
│   ├── biometric_service.go
│   ├── video_call_service.go
│   ├── queue_service.go
│   ├── credential_service.go
│   └── account_service.go
├── repository/       ← Database access
│   ├── session_repo.go
│   ├── onboarding_repo.go
│   └── queue_repo.go
├── model/            ← Domain models
│   ├── session.go
│   ├── ocr_result.go
│   ├── biometric_result.go
│   └── account.go
├── websocket/        ← Video call signaling
│   ├── hub.go
│   ├── client.go
│   └── message.go
└── crypto/           ← Encryption utilities
    ├── rsa.go
    └── aes.go
```

### Database Tables

```sql
-- Tabel utama
onboarding_sessions     -- session lifecycle
onboarding_ocr_results  -- hasil OCR per session
onboarding_personal_data -- data pribadi nasabah
onboarding_biometrics   -- hasil biometrik
onboarding_video_calls  -- rekaman & hasil video call
onboarding_credentials  -- hash kode akses & PIN
onboarding_audit_logs   -- immutable audit trail

-- Redis keys
onboarding:session:{id}         -- session cache (TTL 24h)
onboarding:queue:active         -- sorted set (score = timestamp join)
onboarding:queue:position:{id}  -- posisi antrean
onboarding:rate:{device_id}     -- rate limiter
```

### Session State Machine

```
CREATED → TNC_ACCEPTED → OCR_PROCESSING → OCR_VERIFIED
→ PERSONAL_DATA_SAVED → OTP_SENT → OTP_VERIFIED
→ BIOMETRIC_PROCESSING → BIOMETRIC_VERIFIED
→ VIDEO_CALL_QUEUED → VIDEO_CALL_ACTIVE → VIDEO_CALL_VERIFIED
→ CREDENTIALS_SET → REVIEW → SUBMITTED → COMPLETED

Side states: EXPIRED | CANCELLED | REJECTED
```

## Aturan Wajib

1. **Idempotency** — Setiap mutating endpoint harus menerima `X-Idempotency-Key`
2. **Audit trail** — Setiap perubahan state session masuk `onboarding_audit_logs`
3. **PII encryption** — NIK, nama, alamat di-encrypt AES-256-GCM di database
4. **Credential hashing** — Kode akses dan PIN di-hash Argon2id, JANGAN simpan plaintext
5. **Auto-expiry** — Session expire 24 jam, foto auto-delete sesuai jadwal
6. **Rate limiting** — Per-device dan per-session, pakai Redis sliding window
7. **Dukcapil validation** — NIK wajib divalidasi ke API Dukcapil sebelum lanjut

---

# Prompt Instructions — AI Agent Backend Onboarding

> Instruksi step-by-step untuk AI agent mengimplementasi backend buka rekening.
> Jalankan satu prompt per sesi, secara berurutan.
> AI agent **wajib** membaca skill di atas dan `06-BUKA-REKENING-API-SPEC.md` sebelum mulai.

---

## Prompt 1: Session Management Service

```
Implement the onboarding session management service in Go.

Requirements:
- POST /v1/onboarding/sessions — create new session
  - Validate product_type enum (TAHAPAN_BCA, TAHAPAN_XPRESI, TABUNGANKU)
  - Generate unique session_id with prefix "onb_"
  - Store in PostgreSQL `onboarding_sessions` table
  - Cache in Redis with TTL 24 hours
  - Rate limit: max 3 sessions per device_id per hour
  - Return product details from config (not hardcoded)

- GET /v1/onboarding/sessions/{id} — resume/check status
  - Return current_step and steps_completed map
  - Check Redis cache first, fallback to PostgreSQL

- DELETE /v1/onboarding/sessions/{id} — cancel session
  - Soft-delete session
  - Queue async job to delete associated files (photos, biometric)
  - Write audit log entry

State machine:
- Session has `current_step` field — enforce ordering, reject out-of-order requests
- Each step transition writes to `onboarding_audit_logs`
- Session expires after 24 hours (background job or lazy check)

Database schema for `onboarding_sessions`:
- id (UUID primary key)
- session_id (unique, indexed)
- device_id (indexed)
- product_type (enum)
- current_step (enum)
- tnc_version (string)
- created_at, updated_at, expires_at (timestamps)
- deleted_at (nullable, soft delete)

Use the repository pattern. Service layer handles business logic,
repository handles SQL queries. Follow project conventions from
00-ARCHITECTURE-OVERVIEW.md and 04-SECURITY.md.
```

---

## Prompt 2: OCR Service & Dukcapil Integration

```
Implement the OCR processing service for Indonesian e-KTP.

Requirements:
- POST /v1/onboarding/ocr — accept multipart upload
  - Validate session exists and current_step == "OCR"
  - Save photo to S3-compatible storage (encrypted at rest)
  - Extract text via OCR engine (Google Cloud Vision API)
  - Parse Indonesian KTP fields from raw OCR text:
    NIK (16 digits), nama, tempat_lahir, tanggal_lahir,
    jenis_kelamin, alamat, RT/RW, kelurahan, kecamatan,
    kota, provinsi, agama, status_perkawinan
  - Validate NIK format (16 digits, valid province/city code)
  - Call Dukcapil API to verify NIK + nama match
  - Assess photo quality (sharpness, glare, corners)
  - Store results in `onboarding_ocr_results` table
  - Update session current_step to "PERSONAL_DATA"
  - Rate limit: 10 attempts per hour per session

KTP Parser logic:
- NIK structure: PPKKCC-DDMMYY-NNNN
  PP = provinsi, KK = kota, CC = kecamatan
  DD = tanggal (female +40), MM = bulan, YY = tahun
  NNNN = sequence
- Extract gender from NIK: DD > 40 means female
- Validate checksum if available

Dukcapil client (mock for development):
- Interface: DukcapilClient with method VerifyNIK(nik, nama) (bool, error)
- Production: HTTP call to Dukcapil API (separate config)
- Staging: Mock that returns true for known test NIKs

Error handling:
- Photo too blurry → return OCR_PHOTO_BLURRY with instruction
- Glare detected → return OCR_GLARE_DETECTED
- Not a KTP → return OCR_NOT_KTP
- Dukcapil timeout → return OCR_DUKCAPIL_TIMEOUT (allow retry)

Encryption:
- Photo stored with AES-256-GCM encryption key per-session
- OCR results (NIK, nama) stored encrypted in PostgreSQL
- Schedule auto-deletion: photo after 30 days, OCR after account creation
```

---

## Prompt 3: Personal Data & OTP Service

```
Implement personal data submission and OTP verification.

Requirements:
- POST /v1/onboarding/personal-data
  - Validate session current_step == "PERSONAL_DATA"
  - Accept personal data JSON (identity + address + employment)
  - Cross-validate with OCR results (NIK, nama must match)
  - Validate enums: pekerjaan, penghasilan, sumber_dana
  - Validate nomor_hp format (Indonesian: 08xx or +628xx)
  - Validate email format
  - Store encrypted in `onboarding_personal_data` table
  - Generate 6-digit OTP, send via SMS gateway
  - Store OTP hash in Redis (TTL 5 minutes)
  - Update current_step to "OTP_VERIFY"

- POST /v1/onboarding/verify-otp
  - Validate session current_step == "OTP_VERIFY"
  - Compare OTP against Redis hash
  - Rate limit: 5 attempts per 5 minutes
  - After 3 failed attempts: regenerate new OTP
  - After 5 failed attempts: block for 30 minutes
  - On success: update current_step to "BIOMETRIC"
  - Write audit log for both success and failure

SMS Gateway interface:
- Interface: SMSGateway with method SendOTP(phone, otp) error
- Production: integration with telco SMS provider
- Staging: log OTP to console, always succeed

PII encryption:
- All personal data fields encrypted with AES-256-GCM
- Separate encryption key from session key
- Key stored in HSM/Vault (config-driven)
```

---

## Prompt 4: Biometric Verification Service

```
Implement biometric face liveness verification service.

Requirements:
- POST /v1/onboarding/biometric — multipart upload
  - Validate session current_step == "BIOMETRIC"
  - Accept: face_photo (main), liveness_frames (3-5 challenge frames)
  - Store all photos encrypted in S3

Liveness detection pipeline:
1. Validate face_photo has exactly 1 face
2. Check liveness_frames sequence:
   - Frame quality check (not blurry, good lighting)
   - Blink detection across frames
   - Head movement detection (optional challenge)
   - Anti-spoofing: detect screens, printed photos, masks
3. Face matching: compare face_photo with KTP photo from OCR step
   - Use face embedding comparison (cosine similarity > 0.85)
4. Calculate scores:
   - liveness_score (0-100)
   - face_match_score (0-100)
   - Thresholds: liveness >= 90, face_match >= 85

Integration options (implement as interface):
- Option A: Google Cloud Vision Face Detection
- Option B: AWS Rekognition
- Option C: On-premise model (ONNX runtime)
- Mock: return success for development

Store results in `onboarding_biometrics` table:
- session_id, liveness_verified, liveness_score,
  face_match_verified, face_match_score,
  iso_compliant, created_at

Auto-delete biometric photos after 7 days (GDPR/PDP compliance).
Rate limit: 5 attempts per hour per session.

On success: update current_step to "VIDEO_CALL"
```

---

## Prompt 5: Video Call Queue & Signaling Server

```
Implement video call queue management and WebRTC signaling server.

This is the most complex service. Split into two parts:

PART A: Queue Management
- POST /v1/onboarding/video-call/queue — join queue
  - Validate session current_step == "VIDEO_CALL"
  - Add to Redis sorted set (score = join timestamp)
  - Generate queue number (format: A-NNN, sequential per day)
  - Calculate position and estimated wait
  - Return signaling WebSocket URL with short-lived JWT token
  - Update current_step to "VIDEO_CALL_QUEUED"

- Queue processor (background goroutine):
  - Monitor available CS agents (from agent-service or Redis)
  - When agent available: pop oldest from queue
  - Assign agent to session
  - Notify client via WebSocket: "agent_assigned"
  - Operating hours check: 06:00-22:00 WIB

PART B: WebSocket Signaling Server
- WS /v1/onboarding/video-call/signal?token=<jwt>
  - Authenticate JWT from query param
  - Two roles per room: "nasabah" (mobile client) and "agent" (CS dashboard)
  - Room ID = session_id

Message relay (pass-through signaling):
- Client sends "offer" (SDP) → relay to agent
- Agent sends "answer" (SDP) → relay to client
- Both sides exchange "ice_candidate" → relay to peer

Server-initiated messages:
- "queue_update" — periodic position update (every 10s while waiting)
- "agent_assigned" — agent name, employee_id
- "instruction" — text instruction from agent to client
- "call_ended" — result (APPROVED/REJECTED), duration

Connection management:
- Ping/pong heartbeat every 30 seconds
- Reconnect grace period: 60 seconds
- If client disconnects > 60s: release agent, remove from queue
- If agent disconnects: reassign to another agent

Hub pattern:
- Hub manages all active WebSocket connections
- Map[sessionID] → Room{nasabahConn, agentConn}
- Thread-safe with mutex or channels

STUN/TURN server configuration:
- Return STUN/TURN credentials in "agent_assigned" message
- TURN server required for NAT traversal in mobile networks
- Credentials: time-limited (RFC 5766 shared secret)

Do NOT implement WebRTC media — that happens peer-to-peer in the browser/app.
The backend only handles signaling (SDP, ICE candidates).
```

---

## Prompt 6: Credential Storage Service

```
Implement credential (access code + PIN) storage service.

Requirements:
- POST /v1/onboarding/credentials
  - Validate session current_step == "CREDENTIALS"
  - Decrypt access_code and pin using RSA private key
  - Validate access code rules:
    - Exactly 6 alphanumeric characters
    - Not sequential (abc123, 123456)
    - Not all same character (aaaaaa, 111111)
  - Validate PIN rules:
    - Exactly 6 digits
    - Not sequential (123456, 654321)
    - Not all same digit
    - Different from access code
  - Hash with Argon2id:
    - memory: 64MB, iterations: 3, parallelism: 4
    - unique salt per credential
  - Store hashes in `onboarding_credentials` table
  - NEVER log or store plaintext credentials
  - Update current_step to "REVIEW"

RSA key management:
- Public key: served to mobile app (rotated quarterly)
- Private key: stored in HSM/Vault, loaded at startup
- Key ID tracking: mobile sends encryption_key_id
  so server knows which private key to use

Sequential detection algorithm:
- Check if characters form arithmetic sequence
- "abcdef", "123456", "fedcba" are all sequential
- Sliding window of 3+ consecutive chars counts as sequential

Argon2id parameters (banking-grade):
- time: 3 iterations
- memory: 65536 KB (64MB)
- threads: 4
- salt: 16 bytes random per credential
- hash output: 32 bytes

Store in `onboarding_credentials`:
- session_id, access_code_hash, access_code_salt,
  pin_hash, pin_salt, encryption_key_id, created_at
```

---

## Prompt 7: Final Submit & Account Creation

```
Implement the final submission endpoint that creates the bank account.

Requirements:
- POST /v1/onboarding/submit
  - Validate X-Idempotency-Key header (prevent double submit)
  - Validate session current_step == "REVIEW"
  - Validate ALL steps completed:
    tnc, ocr, personal_data, otp, biometric, video_call, credentials
  - If any step incomplete: return ONBOARDING_INCOMPLETE error

Account creation flow:
1. Begin database transaction
2. Create account in `accounts` table:
   - Generate 10-digit account number (BCA format)
   - Assign nearest branch (based on KTP address)
   - Set status = ACTIVE
   - Set initial_deposit_deadline = now + 30 days
3. Create m-BCA user:
   - Link account to credentials (access code + PIN hashes)
   - Set user_id with prefix "mbca_"
4. Update onboarding session: current_step = "COMPLETED"
5. Write audit log: ACCOUNT_CREATED
6. Commit transaction

Post-creation async jobs (queue via Redis/NATS):
- Send welcome SMS to registered phone
- Send welcome email with account details
- Generate virtual debit card (if applicable)
- Notify branch for physical card delivery
- Schedule initial deposit reminder (T+25 days)

Account number generation:
- Format: 10 digits, starts with branch_code prefix
- Must be unique (check against existing accounts)
- Use database sequence + branch prefix

Idempotency handling:
- Store idempotency_key → response mapping in Redis (TTL 24h)
- If same key received: return cached response
- Prevents double account creation on network retry

Core banking integration (interface):
- Interface: CoreBankingClient
  - CreateAccount(data) (accountNumber, error)
- Production: HTTP/gRPC call to core banking system
- Staging: generate mock account number, return success

Error handling:
- If core banking fails: rollback transaction, return ACCOUNT_CREATION_FAILED
- If partial failure: compensation/saga pattern
- Log everything for audit trail
```

---

## Prompt 8: Audit Trail & Monitoring

```
Implement comprehensive audit logging for the onboarding flow.

Requirements:
- Every state transition in onboarding session must be logged
- Every API call must be logged (success + failure)
- Logs must be immutable (append-only table, no UPDATE/DELETE)

Table `onboarding_audit_logs`:
- id (UUID)
- session_id (indexed)
- event_type (enum: SESSION_CREATED, OCR_UPLOADED, OCR_VERIFIED,
  PERSONAL_DATA_SAVED, OTP_SENT, OTP_VERIFIED, OTP_FAILED,
  BIOMETRIC_UPLOADED, BIOMETRIC_VERIFIED, BIOMETRIC_FAILED,
  VIDEO_CALL_QUEUED, VIDEO_CALL_STARTED, VIDEO_CALL_ENDED,
  CREDENTIALS_SET, SUBMITTED, COMPLETED,
  SESSION_EXPIRED, SESSION_CANCELLED)
- actor (string: "system", "nasabah:{device_id}", "agent:{employee_id}")
- details (JSONB: event-specific payload)
- ip_address
- user_agent
- created_at (indexed)

Middleware approach:
- Create audit middleware that wraps every onboarding handler
- Automatically captures: request_id, device_id, ip, user_agent
- Handler calls auditService.Log(event_type, details) explicitly

Monitoring alerts (integrate with alerting system):
- Session stuck in same step > 1 hour → alert
- OCR failure rate > 20% in 5 minutes → alert
- Biometric spoof detected → immediate alert + block device
- Video call queue > 10 people → alert CS supervisor
- Submission failure rate > 5% → alert on-call engineer

Data retention:
- Audit logs: retain 7 years (POJK requirement)
- Session data: 30 days after completion, then archive
- Photos: per retention schedule (KTP 30d, biometric 7d, video 5y)
- Credentials: until account closed
```