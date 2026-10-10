# Task: Audit & Harden Face Biometric (Liveness) — BCA Mobile

## Role
You are a senior Android + security engineer. Your job is to (1) investigate the current face biometric implementation, (2) report every issue/bug with evidence, (3) STOP and wait for approval, then (4) fix it according to the target spec below.

**Do not modify any code in Phase 0–1.** Read, trace, run, and log only.

---

## Background (from product owner)
- Face detection is **not precise**.
- The user must **tap a button** to start. Expected: the check starts **automatically** once a valid face is detected — no button.
- The user must perform **head movements (left, up, down) and an eye blink** before passing (active liveness).
- Current implementation is considered **insecure / easy to bypass**, which is unacceptable for a mobile banking app.

### Important clarification (read before investigating)
- Android's system face unlock via `BiometricPrompt` does **not** support head-movement or blink challenges, and the app cannot customize it. Head turn + blink is an **app-owned active liveness flow** (CameraX + ML Kit Face Detection or a vendor SDK). Identify which one(s) this project actually uses — it may use both (e.g. `BiometricPrompt` for login, custom camera liveness for enrollment / KYC / high-value transactions).
- A liveness check that runs **only on the client** and reports `passed = true` is **not secure**, no matter how good the detection is: it can be bypassed with hooking (Frida/Xposed), a repackaged APK, an emulator, or a virtual camera / video injection. A fixed challenge order (always left → up → down → blink) can be defeated with a pre-recorded video. The fix must therefore include **server-side verification**, not only better on-device detection.

---

## Phase 0 — Discovery
1. Locate all code related to face/biometric: search for `BiometricPrompt`, `BiometricManager`, `FaceDetector`, `FaceDetection`, `ImageAnalysis`, `CameraX`, `ProcessCameraProvider`, `liveness`, `biometri`, `selfie`, `kyc`, `blink`, `eulerAngle`, `EyeOpenProbability`.
2. Determine the client stack (Kotlin/XML, Compose, Flutter, etc.) and the module/feature paths.
3. Locate the backend endpoints involved (Go service, e.g. biometrik login/enrollment, registrasi/KYC). If the `bca-mobile-backend` skill is available, use its conventions for any backend work.
4. Draw the current end-to-end flow: screen → camera → detection → challenge → result → API call → server decision → session/token. Note exactly **where the "pass" decision is made**.

---

## Phase 1 — Audit checklist
For each item, confirm or rule out with file:line evidence.

### A. Detection precision (client)
- [ ] `InputImage` built without `imageProxy.imageInfo.rotationDegrees` (wrong rotation → face missed / wrong angles).
- [ ] `FaceDetectorOptions`: `PERFORMANCE_MODE_FAST` where accuracy matters; `CLASSIFICATION_MODE_NONE` (eye-open probability is then null → blink can never be detected properly); `enableTracking()` missing; `setMinFaceSize` not set.
- [ ] `ImageAnalysis` backpressure not `STRATEGY_KEEP_ONLY_LATEST`, or `ImageProxy.close()` not called in all paths (lag, frozen analysis, memory leak).
- [ ] Analysis resolution too low (blurry landmarks) or too high (dropped frames).
- [ ] Front-camera mirroring not accounted for → "left" detected as "right". **Verify the sign of `headEulerAngleY` empirically on a real device with logs** — do not assume.
- [ ] Face bounding box not mapped from image coordinates to `PreviewView` coordinates (scale type / crop) → "face inside oval" check is wrong.
- [ ] Decisions made from a **single frame** with no smoothing / consecutive-frame confirmation / hysteresis → jitter and false triggers.
- [ ] Multiple faces not rejected; no reset when the face is lost or `trackingId` changes mid-challenge.
- [ ] Hard-coded magic thresholds with no central config.

### B. Trigger / UX
- [ ] Manual start button; no automatic "face ready" gate.
- [ ] No per-step timeout, total timeout, or max-attempt limit; no fallback to PIN (Kode Akses).
- [ ] Missing accessibility: TalkBack announcements per instruction, haptics, clear instruction text.

### C. Liveness logic
- [ ] Challenge order is **fixed** / predictable / generated on the client.
- [ ] Steps can be satisfied out of order, or several steps by one movement.
- [ ] No "return to neutral" required between head poses.
- [ ] Blink detected from one "eyes closed" frame (a photo with closed eyes passes) instead of an open → closed → open sequence within a time window.
- [ ] Same-person continuity not enforced (face swap mid-challenge).

### D. Security
- [ ] Client decides pass/fail and sends a boolean / flag to the server, or unlocks locally.
- [ ] No server-issued challenge/nonce; no TTL; nonce reusable → replay.
- [ ] No frames/evidence sent for server-side **passive liveness (PAD) and face match** against the enrolled reference (KYC selfie / e-KTP data).
- [ ] No device integrity check (Play Integrity verified **server-side**), no root/hook/emulator signals, no virtual-camera / injection defenses.
- [ ] Screen not protected with `FLAG_SECURE`.
- [ ] Face frames written to disk, cache, or logs; PII in Logcat / crash reports.
- [ ] Request not bound to `device_id` / device key (Android Keystore signature); no rate limit / lockout / audit trail on the backend.
- [ ] If `BiometricPrompt` is used for login/transactions: `BIOMETRIC_WEAK` allowed, no `CryptoObject`, key not created with `setUserAuthenticationRequired(true)` and `setInvalidatedByBiometricEnrollment(true)`, success callback trusted without a cryptographic operation.

### Phase 1 output (required format)
1. Current flow diagram (text is fine) and where the pass decision lives.
2. Findings table:

| ID | Area (A–D) | Severity (Critical/High/Medium/Low) | File:line | Evidence | Impact | Proposed fix |
|----|-----------|-------------------------------------|-----------|----------|--------|--------------|

3. Proposed scope of change (client files, backend files, new endpoints/migrations, new dependencies).
4. Open questions (e.g. is a certified liveness vendor available? which flows use face: login, enrollment, KYC, transaction step-up?).

## ⛔ STOP — wait for explicit approval before Phase 2.

---

## Phase 2 — Target behavior (implement after approval)

### State machine
`IDLE → CAMERA_READY → POSITIONING → CHALLENGE(step i of n) → CAPTURE → VERIFYING → SUCCESS | FAILED`
- Any face loss > 1 s, second face detected, or `trackingId` change during `CHALLENGE` → abort the current challenge, request a **new** nonce, restart from `POSITIONING`.

### Auto-start (no button)
Enter `CHALLENGE` automatically when the quality gate holds for ≥ 500 ms (or ≥ N consecutive frames):
- exactly 1 face; face box inside the oval guide and 35–70% of its width;
- neutral pose: |yaw| < 10°, |pitch| < 10°, |roll| < 15°;
- both eyes open (prob > 0.8); acceptable brightness/blur.
Show live guidance while the gate is not met ("Dekatkan wajah", "Wajah di tengah", "Cahaya kurang").

### Challenge
- Action set: `TURN_LEFT`, `LOOK_UP`, `LOOK_DOWN`, `BLINK` (optionally `TURN_RIGHT`).
- **Server issues** `{challenge_id, nonce, actions[] in random order, expires_at}` (TTL ~60 s, single use). Minimum 3 actions, `BLINK` always included. The client never generates the order.
- Detection rules (starting values — keep in one config object and calibrate on real devices):
  - Turn left: yaw past 25° in the verified "left" direction, held ≥ 3 consecutive frames.
  - Look up: pitch > +20°; look down: pitch < −15°, held ≥ 3 frames.
  - Must return to neutral (|yaw|, |pitch| < 10°) before the next step.
  - Blink: both eyes > 0.8 → both < 0.2 → both > 0.8, whole sequence within 600 ms.
  - Only the expected step counts; a different movement does not advance (repeated wrong moves → fail).
- Timeouts: 8 s per step, 30 s total. Max 3 attempts, then cooldown and fallback to PIN.

### Capture & verification
- Capture a best neutral frame plus one frame at each completed step, kept **in memory only** (no disk, no logs).
- Send to backend with `challenge_id`, `nonce`, per-step timestamps, Play Integrity token, `device_id`, signed with a device Keystore key; TLS with the project's existing pinning.
- Backend (authoritative decision):
  - validate nonce (exists, unexpired, unused → mark used), device binding, integrity verdict, signature;
  - run passive liveness/PAD and face match against the enrolled reference (vendor or internal service — confirm in Phase 1);
  - rate-limit per device/user, lockout after repeated failures, write audit trail;
  - only then issue the session/token or authorize the transaction.
- The client UI shows success only after the server response.

### Hardening
- `FLAG_SECURE` on the liveness screen; use only the physical front camera via CameraX.
- Root / hook / emulator signals sent to the server as risk inputs (do not rely on client blocking alone).
- If `BiometricPrompt` is used: `BIOMETRIC_STRONG` + `CryptoObject`, keys invalidated on new biometric enrollment, server verifies the signature.

### Constraints
- Keep changes additive; do not break PIN login or other auth flows.
- No new third-party SDK without listing it in the Phase 1 scope for approval.
- Do not claim the result is "secure" or "certified". ML Kit–based active liveness is a UX + friction layer; production-grade anti-spoofing needs server-side PAD (ideally an ISO/IEC 30107-3–tested vendor). State remaining risks explicitly in the final report.

---

## Phase 3 — Test plan / acceptance criteria
Provide a test case table (ID / steps / expected / evidence) and unit tests for the state machine and detection rules (feed synthetic face values — no camera needed).

**Functional**
- TC-01 Valid face in oval → challenge auto-starts, no button.
- TC-02 Each action detected correctly on front camera (left really = user's left).
- TC-03 Random order from server is followed; wrong movement does not advance.
- TC-04 Blink requires open→closed→open; eyes kept closed does not pass.
- TC-05 Step timeout / total timeout / 3 failures → PIN fallback + cooldown.
- TC-06 Low light, glasses, hijab/hat, different skin tones — detection still works or shows guidance.

**Negative / security**
- TC-10 Printed photo / photo on another screen → fails.
- TC-11 Replay of a pre-recorded video doing left→up→down→blink → fails (order is random + server PAD).
- TC-12 Two faces in frame, or face swapped mid-challenge → challenge reset.
- TC-13 Reused or expired nonce → server rejects.
- TC-14 Tampered client sends "passed" without valid frames/signature → server rejects.
- TC-15 Rooted device / emulator / failed Play Integrity → server applies risk policy (reject or step-up).
- TC-16 Screenshot / screen recording blocked on the liveness screen; no frames in storage or Logcat.

**Performance / devices**
- TC-20 Low-end, mid, flagship device; analysis keeps ≥ 15 fps without ANR or memory growth.

## Final deliverables
1. Phase 1 report (findings table, scope, open questions).
2. After approval: implementation (client + backend), unit tests, test case table with results, and a list of residual risks.
