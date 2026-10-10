# Phase 2 Report — Face Liveness Hardening (BCA Mobile)

Implementasi keputusan di `bca-face-liveness-decisions.md`, dua repo:
`BcaMobile` (Android) dan `bca-mobile-api` (Go).

**Satu hal yang harus dibaca lebih dulu:** dengan konfigurasi produksi, verifikasi
liveness sekarang **menolak setiap percobaan**. Itu bukan bug — itu aturan
fail-closed di §1b keputusan Phase 2 yang bekerja sebagaimana mestinya, karena
lapisan ML tidak punya model yang boleh dipakai komersial. Rinciannya di
§"Yang BLOCKED" dan §"Risiko yang tersisa".

---

## 1. Hasil pemeriksaan vendor (Q1a)

**Tidak ada vendor liveness/PAD tersertifikasi di kedua repo.**

Yang diperiksa dan hasilnya:

| Tempat | Hasil |
|---|---|
| `gradle/libs.versions.toml` | Hanya ML Kit face-detection 16.1.7 + CameraX 1.5.3. Tidak ada SDK vendor. |
| `app/libs/`, `*.aar` | Tidak ada. |
| `go.mod` / `go.sum` | Tidak ada inference runtime (ONNX/TensorFlow/OpenCV), tidak ada SDK vendor e-KYC. |
| `internal/config/config.go` | Tidak ada env key vendor. |
| `keys/` | Hanya pasangan RSA PIN dan JWT. |
| `docs/**` | Satu-satunya jejak: `iso_30107_compliant: true` di spec — field dari mesin tiruan. |
| Mesin yang ada | `MockBiometricEngine` (`biometric_engine.go:15-28`): `LivenessScore 98.2`, `FaceMatchScore 96.7`, `ISOCompliant true`, **tanpa melihat input sama sekali**. |

Konsekuensinya mesin internal yang dibangun. Dan sesuai §1c: **membangun mesin
internal tidak membuat produk ini tersertifikasi ISO/IEC 30107-3.**

---

## 2. Tabel temuan

| ID | Area | Sev | Status | Berkas | Yang dikerjakan | Test | Catatan / risiko sisa |
|---|---|---|---|---|---|---|---|
| F-01 | D | Crit | **DONE** | `LivenessStateMachine.kt`, `biometric_service.go` | Keputusan lulus dipindah ke server. Mesin status berhenti di `CAPTURE`; `onServerVerdict()` satu-satunya yang menetapkan SUCCESS/FAILED | `hasil akhir hanya datang dari server`, `TestProcessBiometric_PassAdvancesStep` | — |
| F-02 | D | Crit | **DONE** | `OnboardingModels.kt`, `entity.go` | `LivenessMeta` (`completed_actions` konstanta) dihapus dari kontrak. `LivenessSubmission` tidak punya field hasil | `server menolak berarti gagal…` | — |
| F-03 | C/D | Crit | **DONE** | `liveness_challenge_service.go`, `LivenessAction.kt` | Server memilih BLINK + 2 pose kepala berbeda, urutan diacak `crypto/rand`; client memakai apa adanya | `TestPickActions_*` (3 test), `urutan server diikuti…` | — |
| F-04 | D | Crit | **DONE** | `liveness_challenge_service.go`, `biometric_cache.go` | Nonce 32 byte `crypto/rand`, TTL 60s, sekali pakai via Redis `GETDEL`, terikat `device_id` + `session_id` | `TestProcessBiometric_NonceIsSingleUse`, `…UnknownChallengeRefused`, `…ExpiredChallengeRefused` | — |
| F-05 | D | Crit | **DONE** | `biometric_engine.go`, `providers.go` | `MockBiometricEngine` dihapus seluruhnya. Tidak ada mock liveness: provider memverifikasi atau menolak | `TestUnconfiguredProvider_Refuses`, `TestValidate_ProductionRejectsStubLivenessProvider` | Stub dev-only, ditolak dua gerbang terpisah |
| F-06 | D | Crit | **DONE** (deterministik) / **BLOCKED** (pose ulang) | `biometric_engine.go` | Lapisan deterministik lengkap: pasangan nonce, binding device/key, jumlah & urutan frame, timestamp monoton + di dalam jendela, kekhasan frame (aHash 64-bit) | `TestDeterministicLayer_Refusals` (10 kasus), `TestFrameDistinctness_*` | Deteksi ulang pose butuh model → BLOCKED, lihat §4 |
| F-07 | C | High | **DONE** | `LivenessStateMachine.kt` | Kedipan = buka→pejam→buka dalam 600 ms, diukur sejak mata **mulai** menutup | `kedipan butuh urutan buka pejam buka`, `mata dipejamkan terus tidak lolos…`, `mata terbuka lagi di luar jendela…` | — |
| F-08 | A | High | **DONE** | `LivenessStateMachine.kt`, `LivenessConfig.kt` | Gerbang kualitas nyata: 1 wajah, lebar 35–70%, offset dari pusat oval, pose netral, kedua mata terbuka, tahan ≥500 ms | `panduan menyebut alasan gerbang tidak terpenuhi` (8 kasus), `gerbang terputus mengulang hitungan tahan` | Oval diuji lewat titik pusat + ukuran, bukan geometri oval — didokumentasikan di `LivenessConfig` |
| F-09 | A/C | High | **DONE** | `LivenessStateMachine.kt` | 5 aksi berarah; `TURN_LEFT/RIGHT/LOOK_UP/LOOK_DOWN` dibedakan; wajib kembali netral antar pose kepala | `menengadah dan menunduk dibedakan`, `kepala wajib kembali netral…`, `kedipan tidak menuntut kembali ke netral` | Tanda sumbu **belum dikalibrasi di perangkat** — §5 |
| F-10 | A | High | **DONE** | `LivenessFaceAnalyzer.kt` | `enableTracking()` dipasang; `trackingId` dikunci saat tantangan mulai | `trackingId berubah membatalkan tantangan` | — |
| F-11 | A | High | **DONE** | `LivenessConfig.kt`, `LivenessStateMachine.kt` | `holdFrames = 3` frame berurutan untuk setiap pose kepala | `gerakan kepala butuh beberapa frame berurutan`, `frame tidak berurutan mengulang hitungan tahan` | — |
| F-12 | C | High | **DONE** | `LivenessStateMachine.kt` | Wajah hilang >1s, wajah kedua, atau `trackingId` berubah → tantangan dibatalkan, nonce baru diminta, frame dibuang | `wajah hilang melewati tenggang…`, `wajah kedua membatalkan…`, `waktu berjalan tanpa frame…` | — |
| F-13 | D | High | **DONE** | `LivenessFrameBuffer.kt`, `LivenessFaceAnalyzer.kt` | Tidak ada frame yang menyentuh disk. Buffer di memori, ditimpa nol saat dibersihkan; tidak ada varian `File` untuk frame wajah | `isi buffer ditimpa nol saat dibersihkan`, `frame ditimpa nol setelah pengiriman` | — |
| F-14 | D | High | **DONE** | `DeviceIntegrityProvider.kt`, `DeviceKeyManager.kt`, `biometric_engine.go` | Play Integrity diminta dengan nonce tantangan, diverifikasi **server-side**; payload ditandatangani kunci Keystore EC P-256; sinyal root/emulator dikirim sebagai masukan | `token integritas diminta dengan nonce tantangan`, `TestProcessBiometric_FailedIntegrityRefusedInProduction`, `…IntegrityCheckedAgainstChallengeNonce` | Butuh kredensial — §3 |
| F-15 | B | High | **DONE** | `…BiometrikScreen.kt`, `AuthGraph.kt` | Tombol "Mulai Perekaman Biometrik" dihapus. Analyzer dipasang saat kamera siap; tantangan mulai sendiri | `tantangan mulai sendiri setelah gerbang kualitas bertahan` | — |
| F-16 | B | High | **DONE** | `LivenessConfig.kt`, `biometric_cache.go` | 8s/langkah, 30s total (client); 3 gagal → 5 menit, 6/24 jam → stop (server) | `langkah kehabisan waktu…`, `total waktu habis…`, `TestProcessBiometric_CooldownIsReportedWithRetryAfter` | — |
| F-17 | D | High | **DONE** | `strings.xml`, `BiometricDto.kt`, `biometric_service.go` | Klaim ISO 30107-3 + OJK dihapus dari layar; `iso_30107_compliant` dihapus dari kontrak dan response | `TestProcessBiometric_ResponseCarriesNoCertificationClaim` | Rincian §6 |
| F-18 | A | Med | **DONE** | `…BiometrikUiState.kt` | `precisionPercent` dihapus; diganti `isFaceReady` yang memang bisa dibuktikan frame | pratinjau + mapper | — |
| F-19 | A | Med | **DONE** | `FaceSignals.kt` | `faceCount` dibawa eksplisit; 0 dan >1 dibedakan; probabilitas mata memakai nilai **terkecil** dari dua mata, bukan rata-rata | `panduan menyebut alasan…` | — |
| F-20 | A | Med | **DONE** | `LivenessConfig.kt` | Semua ambang di satu objek, termasuk `yawSignForUserLeft`, `pitchSignForUp`, `isAnalysisMirrored` | `arah menoleh mengikuti kalibrasi tanda` | — |
| F-21 | B | Med | **DONE** | `…BiometrikUiState.kt` | Nilai bawaan palsu (`98`, `2`) dihapus; state awal kosong | pratinjau "Belum siap" | — |
| F-22 | B | Med | **DONE** | `…BiometrikScreen.kt` | Kartu instruksi mengikuti langkah; `liveRegion = Polite` untuk TalkBack; bilah progres punya `contentDescription` | — (perilaku Compose, diverifikasi lewat pratinjau) | Haptics **tidak** ditambahkan — lihat §7 |
| F-23 | D | Med | **DONE** | `biometric_cache.go`, `liveness_challenge_service.go` | Hitungan per **device + session**; masa tunggu dikembalikan sebagai `details.retry_after_seconds` | `TestProcessBiometric_CooldownIsReportedWithRetryAfter`, `masa tunggu diambil dari server dan dihitung turun` | — |
| F-24 | A | Low | **TIDAK DIKERJAKAN** | `CameraCapture.kt` | — | — | Resolusi analisa tetap default. Mengubahnya menyentuh jalur kamera OCR yang berbagi `createController`, dan tanpa pengukuran di perangkat angka barunya hanya tebakan. Direkomendasikan diukur saat kalibrasi §5 |
| F-25 | A | Low | **DONE** | `LivenessFaceAnalyzer.kt` | `@Singleton` yang menyesatkan hilang bersama `LivenessDetector`; mesin status dipegang ViewModel, analyzer dibuat per-layar dan ditutup di `onDispose` | — | — |
| F-26 | D | Low | **DONE** | `repository.go`, `biometric_service.go` | `ObjectStorage.Download` ditambahkan; foto KTP benar-benar dimuat sebagai pembanding face match | `TestProcessBiometric_*` lewat `mockStorage.Download` | Akar masalahnya F-NEW-01 |

### Temuan baru yang ditemukan saat mengerjakan

| ID | Sev | Status | Yang terjadi |
|---|---|---|---|
| F-NEW-01 | High | **DONE** | `ObjectStorage` **tidak punya method `Download`**. Jadi `ktpPhotoData = nil` di `biometric_service.go:141-145` bukan kelalaian — face match secara struktural mustahil, bahkan dengan mesin sungguhan. Ditambahkan `Download` ke interface dan ketiga implementasinya. |
| F-NEW-02 | High | **DONE** | Rancangan awal saya mengambil frame bukti kedipan saat mata **terbuka lagi**. Frame itu nyaris identik dengan frame netral, sehingga pemeriksaan kekhasan frame di server akan menolak nasabah yang jujur. Diubah: frame diambil saat mata **terpejam** — sekaligus jadi satu-satunya frame yang membuktikan kedipan terjadi, dan bisa diverifikasi server. |
| F-NEW-03 | Med | **DONE** | Gerbang kualitas meminta nonce setiap frame selama gerbang terpenuhi, sehingga satu percobaan membakar beberapa nonce dan beberapa kuota kegagalan. Dijaga `challengeRequested`. |
| F-NEW-04 | Med | **DONE** | Pembatalan tantangan (`abortChallenge`) tidak membuang frame bukti yang sudah terkumpul, sehingga frame milik nonce lama bisa ikut terkirim pada tantangan berikutnya. `discardFrames` ditambahkan ke jalur itu. |
| F-NEW-05 | Low | **DONE** | Verifikasi tanda tangan harus memakai kunci yang **terdaftar saat tantangan diterbitkan**, bukan kunci yang ikut di request. Kalau tidak, penyerang cukup menandatangani dengan kunci buatannya sendiri dan melampirkannya. Dipastikan oleh `TestProcessBiometric_SubmissionKeyIsIgnored`. |
| F-NEW-06 | **High** | **DONE** | **Ditemukan smoke test end-to-end.** `iso_30107_compliant` masih ikut terkirim di response Go sebagai `false` — saya menghapusnya dari DTO Kotlin tapi test server saya hanya memeriksa **nilainya**, bukan **keberadaan key-nya**. Jadi klaimnya masih ada di kontrak dan masih bisa dibaca client. Field dihapus dari `BiometricResponse`; test diperkuat agar memeriksa JSON hasil marshal tidak memuat key-nya. Kolom database `onboarding_biometrics.iso_compliant` masih ada (NOT NULL) tapi tidak pernah diisi selain `false` dan tidak pernah diserialisasi — penghapusan kolomnya perlu migrasi lanjutan. |
| F-NEW-08 | **High** | **DONE** | **Percobaan ulang tidak pernah terkirim.** `LivenessFaceAnalyzer.submitted` hanya diset sekali, dan analyzer hidup selama controller kamera tidak berubah — jadi melewati `retry()`. Percobaan kedua yang BERHASIL berhenti di fase CAPTURE tanpa pernah dikirim, dan layar menggantung tanpa pesan. `retry()` juga tidak melewati analyzer sama sekali (ViewModel memanggilnya langsung), jadi `discardFrames` tidak menolong. Diperbaiki: frame netral menandai tantangan baru, dan di titik itu buffer dibersihkan serta `submitted` di-reset. |
| F-NEW-09 | **High** | **DONE** | **Frame percobaan lama ikut terkirim.** `CaptureSlot.Step` memuat aksinya, jadi `Step(0, BLINK)` dan `Step(0, TURN_LEFT)` adalah slot **berbeda**. Percobaan ulang dengan urutan aksi baru menumpuk di atas frame lama alih-alih menimpanya, menembus batas enam frame, dan mengirim campuran dua percobaan. Diperbaiki bersama F-NEW-08. Test regresi: `aksi berbeda di indeks sama adalah slot berbeda`. |
| F-NEW-10 | Med | **DONE** | Frame langkah dikirim dalam urutan buffer, bukan urutan indeks. Server memasangkan `liveness_frames[i]` dengan `step_meta[i]` lalu menuntut `step.Index == i` — payload yang ditandatangani sendiri sudah diurutkan indeks, jadi urutan yang salah **lolos** verifikasi tanda tangan lalu ditolak sebagai `step_index_mismatch`. Ditambah `.sortedBy { it.index }`. |
| F-NEW-11 | Med | **DONE** | Gerakan yang **berhasil** bisa hangus: `wrongMove` dievaluasi sebelum `completeStep`, jadi nasabah yang berkedip tepat saat kepalanya masih sedikit menoleh bisa menggagalkan tantangan di frame yang sama ketika langkahnya justru selesai. Urutannya dibalik — langkah selesai menang. |
| F-NEW-12 | Med | **DONE** | Jawaban server yang datang terlambat bisa menimpa percobaan yang sudah dimulai ulang, memaksa SUCCESS atau FAILED di tengah jalan. `onServerVerdict` sekarang hanya berlaku saat fase CAPTURE atau VERIFYING. |
| F-NEW-13 | Low | **DONE** | Tantangan yang hilang antara CAPTURE dan pengiriman membuat `submit()` keluar diam-diam — layar menggantung di CAPTURE tanpa pesan dan tanpa tombol coba lagi. Sekarang melaporkan kegagalan lokal. |
| F-NEW-14 | Low | **DONE** | Bukti yang tidak lengkap dilaporkan sebagai `SERVER_REJECTED`, padahal server belum pernah melihat payloadnya — jejak masalahnya menunjuk ke tempat yang salah. Ditambahkan `LivenessFailure.EVIDENCE_INCOMPLETE`. |
| F-NEW-15 | Med | **DONE** | **Kebohongan yang sama seperti F-NEW-07, di tempat kedua.** `checkIntegrity` mengembalikan satu boolean yang mencampur "verdict-nya lulus" dengan "percobaan ini boleh lanjut". Di mode log-only, verdict yang GAGAL kembali sebagai `true`, lalu tercatat `integrity_ok=true` — tepat di satu mode yang ada justru untuk **mencatat** kegagalan tanpa menolaknya. Jalur `provider_error` juga mencatat `false` padahal integritas sudah lulus. Dipecah jadi `integrityOutcome{Verified, Allowed}`. |
| F-NEW-16 | Low | **DONE** | Konstanta bucket ganda: `ktpPhotoBucket` dan `ocrPhotoBucket` bernilai sama. Dua nama untuk satu bucket akan menyimpang, dan gejalanya adalah face match yang diam-diam tidak pernah menemukan pembandingnya. Yang duplikat dihapus. |
| F-NEW-17 | Low | **DONE** | `LivenessConfig.pitchThreshold` kode mati, tidak pernah dipakai. Dan KDoc di `LivenessPayload.kt` menunjuk ke `liveness_payload.go` yang **tidak ada** — persis di komentar yang tugasnya mencegah kedua sisi payload menyimpang. Keduanya diperbaiki. |
| F-NEW-07 | Med | **DONE** | **Ditemukan saat memeriksa jejak audit sungguhan.** Kolom `integrity_ok` diisi dari "apakah ada token", bukan "apakah verdict-nya sah". Hasilnya baris audit berbunyi `reason=integrity_failed` **dan** `integrity_ok=true` sekaligus — menyesatkan orang yang menyelidiki percobaan itu. Hasil verifikasi sekarang dibawa di `LivenessVerdict.IntegrityOK`. Catatan: upaya perbaikan pertama saya memakai field mutable di `BiometricService`, yang merupakan data race karena service-nya dipakai bersama antar request; diganti jadi nilai yang dialirkan lewat verdict. |

---

## 3. Kunci konfigurasi & secret yang harus Anda isi

### Android (`app/build.gradle.kts`, per product flavor)

| Key | Sekarang | Yang harus diisi |
|---|---|---|
| `PLAY_INTEGRITY_CLOUD_PROJECT` | `0L` di keempat flavor | Nomor project Google Cloud yang **terhubung ke Play Console**. `0` berarti token integritas tidak pernah diminta. |
| `PLAY_INTEGRITY_LOG_ONLY` | `true` di local/ngrok/staging, `false` di production | Biarkan. |

Dependency baru: `com.google.android.play:integrity:1.5.0` (disetujui Q3).

### Backend (`.env`, sudah didokumentasikan di `.env.example`)

**Secret:**

| Key | Keterangan |
|---|---|
| `PLAY_INTEGRITY_CREDENTIALS_FILE` | Path ke service account JSON dengan scope `https://www.googleapis.com/auth/playintegrity`. Taruh di `keys/`, **jangan commit**. |
| `PLAY_INTEGRITY_CERT_SHA256` | SHA-256 base64url sertifikat penanda tangan APK, seperti dilaporkan Play. Tanpa ini, APK yang dibungkus ulang dan mengaku memakai package name yang benar diterima. |

**Policy (punya default yang masuk akal):**
`LIVENESS_PROVIDER`, `LIVENESS_CHALLENGE_TTL`, `LIVENESS_ACTION_COUNT`,
`LIVENESS_MAX_FAILURES`, `LIVENESS_COOLDOWN`, `LIVENESS_MAX_COOLDOWN_ROUNDS`,
`LIVENESS_BLOCK_WINDOW`, `LIVENESS_SCORE_THRESHOLD`,
`LIVENESS_FACE_MATCH_THRESHOLD`, `LIVENESS_MIN_FRAME_DISTANCE`,
`LIVENESS_MAX_CLOCK_SKEW`, `LIVENESS_INTEGRITY_LOG_ONLY`,
`PLAY_INTEGRITY_PACKAGE_NAME`, `PLAY_INTEGRITY_TIMEOUT`.

**Boot ditolak di produksi** kalau `LIVENESS_PROVIDER=stub`,
`LIVENESS_INTEGRITY_LOG_ONLY=true`, atau salah satu dari dua secret di atas kosong.

### Migrasi

`migrations/000040_liveness_attempts.{up,down}.sql`. **Belum pernah dijalankan** —
Docker tidak tersedia di lingkungan ini. Jalankan `make migrate-up`, lalu
`make migrate-down 1` dan `up` lagi untuk memastikan pasangannya benar.

---

## 4. Yang BLOCKED, dan apa artinya

**Lapisan ML dari `internal` provider: BLOCKED.**

Alasannya di luar basis kode, sesuai §97 keputusan Phase 2:

1. Go tidak punya inference runtime di repo ini. `onnxruntime-go` menuntut library
   native ONNX Runtime terpasang di host — itu dependency sistem, bukan Go module.
2. Model yang dibutuhkan ada tiga: deteksi wajah + pose, embedding untuk
   kesinambungan orang dan face match, dan PAD. Bobot yang tersedia untuk
   ketiganya (keluarga InsightFace/ArcFace, dan model PAD riset) **tidak** berada
   di bawah lisensi yang mengizinkan pemakaian komersial. Aturan lisensi §1b
   (Apache-2.0 / MIT) tidak terpenuhi.

Jadi yang dikerjakan — persis seperti yang §1b perintahkan untuk kasus ini:

- `LivenessProvider` sebagai interface, dipilih konfigurasi, siap diganti vendor
  tersertifikasi **tanpa menyentuh client atau kontrak API**.
- `FaceAnalyzer` sebagai port untuk lapisan ML. Tidak ada implementasi di repo ini.
- Lapisan deterministik **lengkap dan berjalan**: nonce, binding, jumlah dan
  urutan frame, timestamp, kekhasan frame.
- **Fail-closed**: `analyzer == nil` → verdict `Passed: false, Unavailable: true`.

Pemeriksaan yang ikut BLOCKED karena butuh model: satu wajah per frame, **deteksi
ulang pose di sisi server**, kesinambungan orang antar frame, PAD pasif, face match.

**Akibat yang harus dibaca apa adanya:** di profil produksi, `POST /onboarding/biometric`
membalas `503 LIVENESS_PROVIDER_UNAVAILABLE` untuk setiap percobaan. Fitur ini
terpasang penuh tapi **tidak bisa dipakai di produksi** sampai vendor atau model
berlisensi dikonfigurasi. Itu keputusan yang benar (menolak lebih baik daripada
meloloskan buta), tapi berarti ini **bukan** fitur yang siap rilis.

Untuk pengujian end-to-end lokal: `APP_ENV=development` + `LIVENESS_PROVIDER=stub`.
Stub tetap menegakkan seluruh lapisan deterministik — foto diam yang diulang untuk
setiap langkah gagal bahkan di bawah stub (`TestStubProvider_StillEnforcesDeterministicLayer`).

---

## 5. Daftar periksa kalibrasi di perangkat fisik (Q7)

Tanda sumbu ML Kit **belum diverifikasi**. Nilai bawaan di
`LivenessConfig.calibration` adalah dugaan, bukan hasil pengukuran.

**Persiapan**

1. Build debug: `./gradlew assembleLocalDebug`, pasang di perangkat fisik
   (bukan emulator — kamera depan emulator tidak merepresentasikan apa pun).
2. `adb logcat -s LivenessCalibration`
3. Buka Buka Rekening sampai layar **Verifikasi Biometrik Wajah**.

**Langkah**

| # | Lakukan | Baca di log | Benar bila |
|---|---|---|---|
| 1 | Wajah lurus di tengah oval | `raw[yaw pitch roll]`, `user[yaw pitch]`, `box[w cx cy]` | `user[yaw]` dan `user[pitch]` ≈ 0; `box[w]` 0.35–0.70; `cx`,`cy` ≈ 0.5; `guide=NONE` |
| 2 | Tolehkan kepala ke **kiri Anda sendiri** | `user[yaw]` | **Positif** dan ≥ 25. Kalau negatif → set `yawSignForUserLeft = -1f` |
| 3 | Tolehkan ke **kanan Anda sendiri** | `user[yaw]` | Negatif dan ≤ −25 |
| 4 | **Angkat** kepala (menengadah) | `user[pitch]` | **Positif** dan ≥ 20. Kalau negatif → set `pitchSignForUp = -1f` |
| 5 | **Tundukkan** kepala | `user[pitch]` | Negatif dan ≤ −15 |
| 6 | Berkedip perlahan | `eye=` | Turun < 0.2 lalu naik > 0.8; `pose=[]` tetap kosong |
| 7 | Geser wajah ke pojok | `cx` / `cy` | `guide=CENTER_FACE`. Kalau arahnya terbalik (geser kiri tapi `cx` naik) → set `isAnalysisMirrored = true` |
| 8 | Mundur, lalu terlalu dekat | `box[w]` | `guide=MOVE_CLOSER`, lalu `MOVE_AWAY` |
| 9 | Minta orang kedua masuk frame | `faces=` | `faces=2`, tantangan dibatalkan, nonce baru diminta |

**Yang juga perlu diukur sekalian (F-24):** apakah analisa bertahan ≥15 fps di
perangkat kelas bawah. Kalau tidak, resolusi `ImageAnalysis` perlu diturunkan di
`CameraCapture.createController` — tapi dengan angka hasil pengukuran, bukan tebakan.

**Verifikasi build release:** `./gradlew assembleLocalRelease`, pasang, lalu
`adb logcat -s LivenessCalibration` **harus kosong** (seluruh badan
`LivenessDebugLog` dijaga `BuildConfig.DEBUG` dan dibuang R8).

Kalau ada angka yang terbalik, kirimkan nilai `yawSignForUserLeft` /
`pitchSignForUp` / `isAnalysisMirrored` yang benar — perubahannya satu baris di
`LivenessConfig.kt`.

---

## 6. Perubahan klaim ISO / OJK (Q4)

Hanya ada **satu** locale di repo ini (`values/`); tidak ada `values-in/` atau
`values-en/`. Grep `30107|ISO|OJK|tersertifikasi|certified` dijalankan di
`res/`, `java/`, dan `docs/design/`.

**Dihapus / diganti:**

| Lokasi | Sebelum | Sesudah |
|---|---|---|
| `strings.xml:595` `…biometrik_security` | "Deteksi Biometrik Liveness Berstandar **ISO 30107-3** & **Diawasi OJK** untuk mencegah pemalsuan identitas." | "Verifikasi wajah untuk memastikan Anda adalah pemilik rekening. Gerakan yang diminta diverifikasi di server BCA." |
| `strings.xml:596-597` `…security_iso`, `…security_ojk` | "ISO 30107-3", "OJK" | **Key dihapus** (nama key sendiri memuat klaimnya; keduanya tidak dipakai di Compose) |
| `…ekyc_security_desc` | "Sesuai regulasi Otoritas Jasa Keuangan (OJK)" | "Dikirim melalui koneksi terenkripsi" |
| `…foto_keamanan_desc` | "…dan diawasi Otoritas Jasa Keuangan (OJK)." | "Foto KTP Anda dikirim melalui koneksi terenkripsi dan hanya dipakai untuk verifikasi identitas." |
| `…dp_security` | "…dan terdaftar di OJK." | "…hanya dipakai untuk pembukaan rekening ini." |
| `…hasil_security` | "Enkripsi End-to-End **standar OJK** & Dukcapil" | "Dikirim terenkripsi, dicocokkan dengan data Dukcapil" |
| `…ringkasan_security` | "…dan diawasi OJK." | "…disimpan sesuai kebijakan privasi BCA." |
| `BiometricDto.kt`, `entity.go`, response API | `iso_30107_compliant` | **Field dihapus** dari DTO, entity, dan response |

**Yang sengaja DIPERTAHANKAN, dan alasannya** — mohon dikonfirmasi:

| Key | Teks | Alasan |
|---|---|---|
| `…ringkasan_ojk_footer`, `…kartu_ojk_lps` | "PT Bank Central Asia Tbk berizin dan diawasi oleh OJK" | Pernyataan tentang **bank**, bukan tentang pemeriksaan liveness. Izin OJK BCA adalah catatan publik, jadi ini pengungkapan institusional, bukan klaim sertifikasi produk yang tidak bisa dibuktikan. |
| `…sk_trust_subtitle` | "Terdaftar dan diawasi oleh Otoritas Jasa Keuangan (OJK)" | Idem. Juga ter-hardcode di pratinjau `BukaRekeningSyaratKetentuanScreen.kt:543`. |
| `…sk_section_2_body` | "…ketentuan OJK POJK No. 12/POJK.01/2017…" | Kutipan dasar hukum pemrosesan data, bukan klaim sertifikasi. |

Kalau Anda ingin ketiganya juga dihapus, sebutkan — perubahannya kecil.

---

## 7. Tabel kasus uji

Status dibedakan jujur: **OTOMATIS** = ada test yang dijalankan dan lulus;
**PARSIAL** = sebagian terbukti, sisanya butuh lapisan yang BLOCKED;
**BELUM** = butuh perangkat fisik, orang, atau Docker.

| TC | Isi | Status | Bukti |
|---|---|---|---|
| TC-01 | Wajah valid di oval → tantangan mulai otomatis, tanpa tombol | **OTOMATIS** | `tantangan mulai sendiri setelah gerbang kualitas bertahan`; tombol Mulai tidak ada lagi di `…BiometrikScreen.kt` |
| TC-02 | Setiap aksi terdeteksi benar di kamera depan (kiri = kiri pengguna) | **BELUM** | Pemetaan tanda dibuat konfigurasi dan diuji untuk kedua arah (`arah menoleh mengikuti kalibrasi tanda`), tapi nilai yang benar hanya bisa ditetapkan di perangkat fisik — §5 |
| TC-03 | Urutan acak server diikuti; gerakan salah tidak memajukan | **OTOMATIS** | `urutan server diikuti dan langkah berikutnya belum aktif`, `gerakan salah berulang menggagalkan tantangan`, `TestPickActions_BlinkPositionVaries` |
| TC-04 | Kedipan butuh buka→pejam→buka; mata dipejamkan terus tidak lolos | **OTOMATIS** | `kedipan butuh urutan buka pejam buka`, `mata dipejamkan terus tidak lolos lalu kehabisan waktu`, `mata terbuka lagi di luar jendela tidak dihitung kedipan` |
| TC-05 | Batas waktu langkah/total; 3 kegagalan → masa tunggu | **OTOMATIS** | `langkah kehabisan waktu…`, `total waktu habis…`, `TestProcessBiometric_CooldownIsReportedWithRetryAfter`, `masa tunggu diambil dari server dan dihitung turun`. Fallback PIN **tidak berlaku** di onboarding (Q6) |
| TC-06 | Cahaya rendah, kacamata, hijab/topi, warna kulit berbeda | **BELUM** | Butuh perangkat dan orang sungguhan. Gerbang kualitas memberi panduan spesifik alih-alih gagal diam (`panduan menyebut alasan gerbang tidak terpenuhi`) |
| TC-10 | Foto cetak / foto di layar lain → gagal | **PARSIAL** | Satu foto diam yang diulang untuk semua langkah ditolak lapisan deterministik — diverifikasi di server sungguhan, §7a #10, bahkan dengan stub aktif. Foto cetak yang **digerakkan tangan** meniru pose butuh PAD → BLOCKED |
| TC-11 | Replay video rekaman kiri→atas→bawah→kedip → gagal | **PARSIAL** | Urutan acak + nonce sekali pakai + tanda tangan yang menutupi digest tiap frame membuat rekaman lama tidak terpakai (`TestProcessBiometric_NonceIsSingleUse`, `…SwappedFrameInvalidatesSignature`). Video yang **mengerjakan urutan acak itu secara langsung** butuh PAD → BLOCKED |
| TC-12 | Dua wajah, atau wajah ditukar di tengah → tantangan reset | **PARSIAL** | Sisi client lengkap: `wajah kedua membatalkan tantangan dan meminta nonce baru`, `trackingId berubah membatalkan tantangan`. Verifikasi ulang kesinambungan orang **di server** butuh embedding → BLOCKED |
| TC-13 | Nonce dipakai ulang atau kedaluwarsa → server menolak | **OTOMATIS + END-TO-END** | `TestProcessBiometric_NonceIsSingleUse`, `…UnknownChallengeRefused`, `…ExpiredChallengeRefused`, `…ChallengeFromAnotherSessionRefused`; plus §7a #8 di server sungguhan |
| TC-14 | Client dimodifikasi mengirim "passed" tanpa frame/tanda tangan sah | **OTOMATIS + END-TO-END** | Tidak ada lagi field semacam itu di kontrak. `TestProcessBiometric_BadSignatureRefused`, `…SwappedFrameInvalidatesSignature`, `…SubmissionKeyIsIgnored`, `payload tanpa tanda tangan tidak pernah dikirim`; plus §7a #9 |
| TC-15 | Perangkat root/emulator/Play Integrity gagal → kebijakan risiko | **OTOMATIS** (kebijakan) / **BELUM** (end-to-end) | `TestProcessBiometric_FailedIntegrityRefusedInProduction`, `…IntegrityLogOnlyAllowsAttempt`, `TestIntegrityVerdict_Acceptable`. Verifikasi dengan Play Integrity sungguhan butuh kredensial — §3 |
| TC-16 | Screenshot diblokir; tidak ada frame di storage atau Logcat | **PARSIAL** | `FLAG_SECURE` sudah aktif seluruh aplikasi (`MainActivity.kt:70`) — ini salah satu dari dua butir audit yang sudah bersih sejak awal. Tidak-ke-disk dibuktikan `isi buffer ditimpa nol…` dan `frame ditimpa nol setelah pengiriman`; `LivenessDebugLog` hanya mencatat angka. Konfirmasi akhir butuh build release di perangkat — §5 |
| TC-20 | Perangkat low-end/mid/flagship, analisa ≥15 fps tanpa ANR | **BELUM** | Butuh perangkat. Diminta diukur bersamaan dengan kalibrasi — §5 |

### 7a. Smoke test end-to-end di server sungguhan

Dijalankan terhadap `make run` dengan Postgres + Redis sungguhan, satu sesi
onboarding nyata yang dibawa ke langkah `BIOMETRIC`, dan kunci EC P-256 asli yang
menandatangani payload kanonis. Frame-nya gambar sintetis, bukan wajah.

| # | Skenario | Hasil |
|---|---|---|
| 1 | Boot dengan provider internal tanpa analyzer | Dua peringatan muncul sebagaimana dirancang: Play Integrity tidak dikonfigurasi, dan lapisan ML tidak tersedia sehingga setiap percobaan akan FAIL |
| 2 | `POST /liveness/challenge` body kosong | `400 VALIDATION_ERROR` |
| 3 | `POST /liveness/challenge` sesi tidak dikenal | `404 ONBOARDING_NOT_FOUND` |
| 4 | `POST /liveness/challenge` sesi sah di langkah BIOMETRIC | `200` — `BLINK, LOOK_DOWN, TURN_LEFT` (acak, BLINK tidak selalu di depan), nonce 43 karakter, kedaluwarsa 60 detik |
| 5 | Kirim bukti bertanda tangan sah, Play Integrity tidak dikonfigurasi | `403 LIVENESS_INTEGRITY_FAILED` — **tanda tangannya lolos**, jadi urutan gerbangnya memang nonce → binding → tanda tangan → integritas → provider |
| 6 | Idem, `LIVENESS_INTEGRITY_LOG_ONLY=true`, provider `internal` | `503 LIVENESS_PROVIDER_UNAVAILABLE` — **fail-closed terbukti di server sungguhan** |
| 7 | Idem, provider `stub` | `200`, `liveness_verified: true`, langkah maju ke `VIDEO_CALL`, dan response **tidak** lagi memuat `iso_30107_compliant` |
| 8 | Kirim ulang payload yang sama persis (replay) | `422 LIVENESS_CHALLENGE_INVALID` (TC-13) |
| 9 | Satu frame ditukar **setelah** ditandatangani | `422 LIVENESS_SIGNATURE_INVALID` (TC-14) |
| 10 | Satu foto diam dipakai untuk semua langkah, ditandatangani benar, **stub aktif** | `422 BIO_LIVENESS_FAILED`, alasan audit `duplicate_frame` (TC-10 sebagian) |
| 11 | Kegagalan ketiga berturut-turut | `429 LIVENESS_COOLDOWN` dengan `details.retry_after_seconds: 300` — bentuk yang persis dibaca client (TC-05) |
| 12 | Jejak audit setelah seluruh rangkaian | 8 baris dengan alasan internal yang benar (`integrity_failed`, `ml_layer_unavailable`, `stub_provider`, `challenge_unknown_or_spent`, `signature_invalid`, `duplicate_frame`) — dan **tidak satu pun** alasan itu muncul di response |
| 13 | Kolom `bytea` di `liveness_attempts` | **nol** — tidak ada frame yang dipersistensi |

Dua temuan nyata lahir dari rangkaian ini: **F-NEW-06** (field ISO masih terkirim)
dan **F-NEW-07** (`integrity_ok` berbohong di jejak audit). Keduanya tidak akan
tertangkap oleh unit test yang saya tulis sendiri — yang pertama karena test saya
memeriksa nilai alih-alih keberadaan key, yang kedua karena hanya terlihat saat
membaca baris audit sungguhnya berdampingan.

**Ringkasan test yang dijalankan**

| Repo | Perintah | Hasil |
|---|---|---|
| BcaMobile | `./gradlew testLocalDebugUnitTest` | **161 test, 0 gagal** (naik dari 151; 10 test liveness baru + state machine & frame buffer) |
| BcaMobile | `./scripts/check-hardcoded-ui.sh` | **OK, tidak ada pelanggaran baru** |
| bca-mobile-api | `go build ./... && go vet ./...` | **bersih** |
| bca-mobile-api | `make lint` (golangci-lint) | **bersih** |
| bca-mobile-api | `go test ./internal/domain/... ./internal/pkg/... ./internal/config/ ./internal/handler/ ./internal/router/` | **semua ok** |
| bca-mobile-api | `make check` (lint + vet + **seluruh** test, Docker hidup) | **PASS** — termasuk `internal/repository/postgres` dan `internal/repository/redis` yang sebelumnya tidak bisa jalan |
| bca-mobile-api | migrasi `000040` up / down 1 / up | **bersih**, tabel hilang dan kembali sebagaimana mestinya |
| bca-mobile-api | smoke test end-to-end di server sungguhan | **8 skenario**, lihat §7a |

---

### Catatan tentang kenapa sepuluh bug ini lolos dari test saya sendiri

Tiga di antaranya (F-NEW-11, F-NEW-12, F-NEW-13) tidak tertangkap karena **harness
test ViewModel saya memintas jalur produksi**: ia menembakkan `FramesReady` langsung
alih-alih menggerakkan mesin status sampai fase CAPTURE dengan frame. Begitu
penjagaan `onServerVerdict` ditambahkan, tiga test itu langsung gagal — dan yang
gagal adalah test-nya, bukan kodenya. Harness-nya diperbaiki supaya menjalankan
siklus penuh seperti analyzer sungguhan, dan jalur yang dipintas itu sekarang
teruji.

Dua lainnya (F-NEW-06, F-NEW-07) hanya muncul di smoke test end-to-end, dan dua
lagi (F-NEW-15, F-NEW-16) dari membaca ulang kode sendiri dengan pertanyaan "apa
yang saya asumsikan di sini". Pelajarannya konsisten: test yang saya tulis sendiri
cenderung menguji asumsi saya, bukan keluaran sebenarnya.

## 8. Risiko yang tersisa — apa adanya

1. **Mesin internal ini TIDAK tersertifikasi ISO/IEC 30107-3.** Sertifikasi hanya
   datang dari pengujian lab terakreditasi. Vendor tersertifikasi tetap
   **direkomendasikan untuk produksi**, dan `LivenessProvider` sudah disiapkan
   supaya penggantiannya tidak menyentuh client maupun kontrak API.

2. **Di produksi, fitur ini menolak semua percobaan** sampai vendor atau model
   berlisensi dikonfigurasi. Terpasang penuh, belum bisa dipakai.

3. **Tanpa lapisan PAD, anti-spoofing yang sebenarnya belum ada.** Yang sudah
   bekerja menaikkan biaya serangan — urutan acak, nonce sekali pakai, tanda
   tangan per frame, kekhasan frame, Play Integrity — tapi tidak ada satu pun yang
   memeriksa apakah yang di depan kamera adalah kulit atau kertas. Jangan
   menyebut flow ini "aman dari pemalsuan".

4. **Tanda sumbu belum dikalibrasi.** Sebelum §5 dijalankan, ada kemungkinan nyata
   bahwa "tolehkan ke kiri" menolak nasabah yang menoleh ke kiri. Fungsional, bukan
   keamanan — tapi akan terasa seperti kerusakan total.

5. ~~`OnboardingLivenessCache` tidak punya test yang dijalankan.~~ **DITUTUP.**
   Sembilan test ditambahkan di `biometric_cache_test.go` dan dijalankan dengan
   Redis sungguhan: `GETDEL` sekali pakai, kedaluwarsa, tiga kegagalan memulai
   masa tunggu, dua putaran memblokir, blokir bertahan melewati masa tunggu,
   pemisahan per session+device, reset, dan jendela kegagalan yang **tidak** ikut
   bergeser setiap percobaan.

6. ~~Migrasi `000040` belum pernah dijalankan.~~ **DITUTUP.** `up` → versi 40,
   `down 1` → versi 39 dan tabelnya hilang, `up` lagi → tabelnya kembali. Bentuk
   tabel diverifikasi lewat `\d liveness_attempts`, termasuk batasan CHECK pada
   `outcome`. Tiga test Postgres ditambahkan, salah satunya menolak kalau ada
   kolom `bytea` atau bernama seperti gambar muncul di tabel itu.

7. **Play Integrity belum pernah diverifikasi dengan token sungguhan.** Jalur
   decode-nya ditulis mengikuti pola `internal/pkg/push/fcm.go` dan diuji dengan
   verifier tiruan; bentuk response sebenarnya dari
   `playintegrity.googleapis.com` belum dikonfirmasi.

8. **Haptics belum ditambahkan** (bagian dari F-22). TalkBack dan teks instruksi
   per langkah sudah ada; getaran per langkah selesai belum. Kecil, dan tidak
   dikerjakan supaya tidak menambah ketergantungan `Vibrator` tanpa diminta.

9. **`escalateToVideoCall` memindahkan langkah ke `VIDEO_CALL` tapi tidak membuat
   entri antrean.** Keputusan Q6 melarang membangun fitur video call baru, jadi
   pemohon masuk ke antrean yang sudah ada lewat jalur normal layar Antrean. Risk
   flag `liveness_failed` ada di jejak audit, **bukan** sebagai kolom yang bisa
   difilter petugas. Rekomendasi: tampilkan flag itu di layar CS.

10. **Keputusan layering yang diambil sadar:** verifier Play Integrity (klien HTTP)
    tinggal di `internal/domain/onboarding/biometric_engine.go`, mengikuti
    preseden repo yang menaruh implementasi integrasi eksternal di domain
    (`MockOCREngine`, `MockDukcapilClient`, dan kawan-kawan). Tempat yang lebih
    tepat adalah `internal/pkg/playintegrity/`, dan itu akan menambah satu paket
    baru di luar set berkas yang disetujui. Pindahkan kalau layering lebih penting.

11. **Jam perangkat yang meleset >2 menit mematikan liveness sepenuhnya.**
    `LIVENESS_MAX_CLOCK_SKEW` bawaan 2 menit, dan timestamp langkah diambil dari
    `System.currentTimeMillis()` di perangkat. Perangkat dengan jam yang disetel
    manual dan salah akan ditolak `timestamp_outside_window` pada **setiap**
    percobaan, lalu terkunci setelah tiga kali — dengan pesan yang tidak
    menjelaskan apa pun kepada nasabah. Alasannya terlihat di jejak audit, jadi
    bisa didiagnosis, tapi tidak bisa dipulihkan nasabah sendiri. Saya tidak
    melebarkan jendelanya sendiri karena itu keputusan kebijakan; pertimbangkan
    5 menit, atau ubah kontrak payload agar memakai waktu **relatif** terhadap
    tantangan alih-alih epoch absolut.

12. **`core/liveness/LivenessPayload.kt` dan `livenessSignedPayload` di Go adalah
    pasangan terikat.** Mengubah salah satunya tanpa yang lain memecah **semua**
    pengiriman dengan `LIVENESS_SIGNATURE_INVALID`. Keduanya sudah memuat komentar
    yang menyebut pasangannya, dan bentuknya dipatok test di kedua sisi.
