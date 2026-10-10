# Phase 1 Decisions & Phase 2 Go-Ahead — Face Liveness Hardening (BCA Mobile)

This file answers every open question from your Phase 1 report. **Phase 2 is approved.**
Implement **every finding in your Phase 1 findings table (F-01 … F-last), without exception**, in both repos (Android client + `bca-mobile-api`).
If you find a new bug while implementing, fix it directly and add it to the table as a new ID (see "Reporting" below). Do not stop to ask unless a rule in this file says STOP.

---

## Q1 — Certified ISO/IEC 30107-3 liveness/PAD vendor: available or not?

**Answer: check first. If none exists, build an internal engine. Never claim certification.**

### 1a. Check (do this first, report the result)
Search both repos and config for any existing vendor integration: Gradle dependencies, SDK `.aar` files, env vars / config keys, secrets placeholders, API clients, README/docs mentioning a liveness, PAD, face-match or e-KYC vendor.
- **Vendor found** → integrate it behind the provider interface in 1b as the default provider.
- **No vendor found** → build the internal engine in 1b. Record "no certified vendor present" in the final report.

### 1b. Build: pluggable server-side liveness module (in `bca-mobile-api`)
- Define a `LivenessProvider` interface (e.g. `Verify(ctx, challenge, frames, reference) (Verdict, error)`), selected by config, so a certified vendor can replace the internal engine later **without touching the client or the API contract**.
- Internal provider = layered checks, all server-side:
  1. **Deterministic checks (mandatory, no ML model needed):** nonce valid/unused/unexpired and bound to `device_id`; frame count matches the challenge; per-step timestamps monotonic and within the challenge window; frames are distinct (reject identical or near-identical frames — perceptual hash); exactly one face in every frame; **re-detect the pose in each step frame on the server** and confirm it matches the expected action (the server must not trust the client's step results); same person across all frames.
  2. **Passive PAD + face match (ML):** run through an inference service (Go via ONNX Runtime, or a small sidecar service — choose and justify). Face match compares the neutral frame against the enrolled reference (KYC selfie). Thresholds in config.
- **Model licensing rule:** only use models whose weights are under a license that allows commercial use (e.g. Apache-2.0 / MIT). List model name, source, license and version in the report. If no suitable model can be used, implement the interface + deterministic layer fully, mark the ML layer **BLOCKED (reason)** in the table, and keep the provider fail-closed (see below).
- **Fail-closed:** if the provider errors, times out, or is not configured in a production build/profile → verdict = FAIL (not PASS). Dev/SIT may use a stub provider only when explicitly enabled by config, and the stub must be impossible to enable in production config.

### 1c. Wording consequence
Building an internal engine does **not** make the product ISO/IEC 30107-3 certified — certification comes only from testing by an accredited lab. See Q4 for the strings change.

---

## Q2 — Do the backend too? (F-01 … F-04 live in `bca-mobile-api`)

**Answer: YES.** Implement all backend findings in `bca-mobile-api`. Client-only changes are not acceptable for this task.
Backend scope includes at minimum:
- `POST` challenge issuance: `{challenge_id, nonce, actions[], expires_at}`, TTL ~60 s, single use, bound to `device_id` (Redis, atomic consume — e.g. `GETDEL` or Lua).
- Verification endpoint: validates nonce, device binding, device-key signature, Play Integrity verdict, then calls `LivenessProvider`; the **server** decides pass/fail and only then issues the session/token or advances the onboarding step.
- Rate limit per device and per user, lockout/cooldown (see Q6), audit trail for every attempt (no raw images in logs).
- Remove/replace any endpoint or code path that accepts a client-side "liveness passed" flag.
- Migrations if new tables/columns are needed (golang-migrate, up + down).
Follow the `bca-mobile-backend` skill conventions if available.

---

## Q3 — May we add Play Integrity?

**Answer: YES.**
- Client: request an integrity token with a request hash/nonce derived from the challenge nonce.
- Server: decode and verify the token **server-side** (Google Play Integrity API), check package name, app certificate digest, `MEETS_DEVICE_INTEGRITY` (or stronger), and that the nonce/request hash matches.
- Credentials (Cloud project number, service account) go in config/secrets as placeholders — **do not hard-code or commit secrets**. List the required config keys in the report so I can fill them in.
- Policy when integrity fails: reject the attempt and record a risk flag (production). Configurable to "log-only" in dev/SIT.

---

## Q4 — May we change `strings.xml:595-597` (ISO / OJK claims)?

**Answer: YES**, this is an approved exception to the additive-only rule.
- Remove every claim of ISO/IEC 30107-3 certification and every claim of OJK compliance/approval that the product cannot prove.
- Replace with neutral, truthful wording describing what the check does, e.g. ID: "Verifikasi wajah untuk memastikan Anda adalah pemilik akun." / EN: "Face verification to confirm you are the account owner."
- Apply the same change to **every locale** (`values/`, `values-in/`, `values-en/`, any others) and anywhere else the claim appears (other strings, layouts, Compose text, docs, Play listing text in repo). Grep for `30107`, `ISO`, `OJK`, `tersertifikasi`, `certified`.
- Keep the same string keys if they are referenced elsewhere; if a key name itself contains the claim (e.g. `iso_certified_label`), rename it and update all references.

---

## Q5 — Action set & order

**Answer: AGREED.**
- Action set: `TURN_LEFT`, `TURN_RIGHT`, `LOOK_UP`, `LOOK_DOWN`, `BLINK`.
- The **server** picks 3 actions per challenge: `BLINK` always + 2 distinct head actions chosen at random; the order of all 3 (including where `BLINK` falls) is randomized. No repeated action in one challenge. Use a cryptographically secure RNG.
- The client only renders and detects the server-provided sequence; it never generates or reorders it.
- Every head action must return to neutral before the next step.

---

## Q6 — Fallback after 3 failures during Buka Rekening (no PIN yet)

**Answer (decision):** the PIN fallback does not apply in onboarding because the PIN is created later at the CREDENTIALS step. Use this rule:
1. After **3 failed attempts** → cooldown **5 minutes** (server-enforced, per device + per onboarding session), then the user may retry.
2. After **2 cooldown rounds (6 failures total) within 24 h** → stop self-service liveness for this onboarding session and:
   - **if** a video-call verification queue already exists in the codebase → route the applicant there with a risk flag (`liveness_failed`, attempt count, integrity verdict);
   - **if not** → block liveness for this device/onboarding session for 24 h and show a message to retry later or visit a branch. **Do not build a new video-call feature** in this task; report it as a recommendation.
3. Cooldown and counters live on the **server**; the client only displays the remaining time returned by the API.
4. Existing flows where the user already has a PIN (login, transaction step-up) keep the PIN fallback.
All values (3, 5 min, 6/24 h) go in config.

---

## Q7 — Direction calibration needs a physical device

**Answer: YES, proceed as you proposed.**
- Make the sign/axis mapping configurable (e.g. `yawSignForUserLeft`, `pitchSignForUp`, and whether front-camera mirroring is applied) in one config object, together with all thresholds.
- Add a debug overlay/log (debug builds only) showing per frame: yaw, pitch, roll, left/right eye-open probability, trackingId, current state, current expected action. **No images, no PII**; stripped from release builds (check with a release build + R8).
- Write a short calibration checklist in the report for me to run on a real phone: look left / right / up / down / blink and confirm the logged sign and the detected action match. I will verify and send back corrected values if needed.

---

## Implementation rules
- Implement **all** findings from your Phase 1 table, client and backend, no exceptions. A finding may be marked BLOCKED only for a reason outside the codebase (missing credentials, no usable licensed model), with the reason written down — and the rest of that finding must still be implemented.
- Fix bugs found along the way immediately; add them to the table as `F-NEW-xx`.
- Do not break PIN login (Kode Akses) or other auth flows. Keep other changes additive except where this file allows otherwise (Q4).
- No new third-party dependency beyond: Play Integrity, ML Kit/CameraX (if not already present), ONNX Runtime or an equivalent inference runtime for Q1b. Anything else → list it in the report with justification.
- No secrets committed. No face images on disk, in cache, or in logs on either side; server keeps only what the audit trail requires (no raw frames unless a retention policy exists — if it does not, do not persist frames).
- Unit tests for: client state machine and detection rules (synthetic face values), server challenge issuance/consumption, nonce replay, provider fail-closed, rate limit/cooldown, Play Integrity verification (mocked).

## Reporting (final output)
1. Findings table updated with a status per row:

| ID | Area | Severity | Status (DONE / BLOCKED) | Files changed | What was done | Test(s) | Notes / residual risk |
|----|------|----------|-------------------------|---------------|---------------|---------|------------------------|

2. Result of the Q1a vendor check.
3. Config keys/secrets I must fill in (Play Integrity, provider, thresholds).
4. Calibration checklist (Q7).
5. Test case table (TC-01 … TC-20 from the original prompt) with result and evidence.
6. Residual risks, stated plainly — including that the internal engine is not ISO/IEC 30107-3 certified and that a certified vendor is still recommended for production.
