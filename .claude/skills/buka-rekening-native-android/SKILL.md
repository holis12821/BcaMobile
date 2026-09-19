---
name: buka-rekening-native-android
description: Native Android integration untuk flow buka rekening BCA — CameraX capture KTP, ML Kit OCR e-KTP Indonesia, ML Kit Face Liveness Detection, permission handling, credential encryption RSA-OAEP Android Keystore, clipboard/share, dan shared ViewModel flow-scoped. Gunakan saat mengintegrasikan CameraX, ML Kit OCR, face detection, permission request, RSA encryption, atau state management onboarding di Android. JANGAN gunakan untuk backend Go (itu skill buka-rekening-backend), layout/UI composable (itu `stitch-to-compose`), atau navigasi/state (itu `compose-architecture`).
---

# Skill: Buka Rekening — Native Android Integration

Gunakan skill ini saat mengintegrasikan capability native Android ke flow buka rekening:
CameraX, ML Kit OCR, ML Kit Face Detection, WebRTC, permission handling,
credential encryption, clipboard/share.

**Jangan** gunakan untuk layout/UI composable (itu `stitch-to-compose`) atau
navigasi/state (itu `compose-architecture`).

---

## Prinsip

1. **Native first** — Pakai API Android bawaan sebelum library pihak ketiga
2. **Tanpa dependency baru tanpa izin** — Per CLAUDE.md, minta persetujuan dulu
3. **Permission before feature** — Selalu cek & minta izin sebelum akses hardware
4. **Encrypt everything sensitive** — Pakai Android Keystore + EncryptedSharedPreferences
5. **Comply POJK** — Semua biometrik harus ISO 30107-3 compliant

---

## 1. CameraX — Foto e-KTP & Biometrik

### Dependency (perlu persetujuan)
```kotlin
// build.gradle.kts
implementation("androidx.camera:camera-core:1.4.1")
implementation("androidx.camera:camera-camera2:1.4.1")
implementation("androidx.camera:camera-lifecycle:1.4.1")
implementation("androidx.camera:camera-view:1.4.1")
```

### Architecture Pattern
```
KameraFotoScreen (Composable, stateless)
  └─ AndroidView { PreviewView }
       └─ CameraController (lifecycle-aware)
            ├─ ImageCapture use case → foto KTP
            └─ ImageAnalysis use case → auto-detect dokumen

KameraFotoViewModel (di luar composable)
  ├─ cameraState: StateFlow<CameraUiState>
  ├─ capturePhoto() → simpan ke temp file → navigate ke HasilFoto
  ├─ toggleFlash(mode: FlashMode)
  └─ setAutoCapture(enabled: Boolean)
```

### Permission Flow
```kotlin
// Di ViewModel atau Activity
val cameraPermission = ActivityResultContracts.RequestPermission()

fun checkCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

// Di Composable: pakai rememberLauncherForActivityResult
val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { granted ->
    if (granted) viewModel.startCamera()
    else viewModel.onPermissionDenied()
}
```

### Aturan
- PreviewView via `AndroidView` dalam Compose — JANGAN buat Fragment
- Gunakan `CameraController` (bukan `CameraProvider`) untuk simplicity
- Flash mode: AUTO, ON, OFF — simpan preference di ViewModel
- Auto-capture: analisis frame via ImageAnalysis, trigger saat dokumen terdeteksi
- Hasil foto: simpan ke `cacheDir` (bukan external storage), hapus setelah upload
- Orientation: lock Activity ke portrait saat kamera aktif

---

## 2. ML Kit — OCR e-KTP Indonesia

### Dependency (perlu persetujuan)
```kotlin
implementation("com.google.mlkit:text-recognition:16.0.1")
// Atau bundled (tanpa download model):
implementation("com.google.mlkit:text-recognition-bundled:16.0.1")
```

### Processing Flow
```
Foto KTP (Bitmap/URI)
  → InputImage.fromBitmap(bitmap, rotation)
  → TextRecognizer.process(image)
  → Text { blocks[] → lines[] → elements[] }
  → KtpParser.parse(text) → KtpOcrResult
```

### KTP Parser (custom logic, bukan library)
```kotlin
data class KtpOcrResult(
    val nik: String,           // 16 digit
    val nama: String,
    val tempatLahir: String,
    val tanggalLahir: String,  // DD-MM-YYYY
    val jenisKelamin: String,
    val alamat: String,
    val rtRw: String,
    val kelurahan: String,
    val kecamatan: String,
    val kota: String,
    val provinsi: String,
    val agama: String,
    val statusPerkawinan: String,
    val accuracy: Float,
)

// Parsing strategy:
// 1. Cari baris mengandung "NIK" → extract 16 digit berikutnya
// 2. Cari baris mengandung "Nama" → ambil value setelahnya
// 3. Cari baris "Tempat/Tgl Lahir" → split by ","
// 4. Gender dari NIK: if tanggal > 40 then perempuan
// 5. Alamat: multi-line setelah label "Alamat"
```

### Aturan
- Proses OCR on-device (JANGAN kirim foto mentah ke cloud untuk OCR)
- Hasil OCR dikirim ke backend `/onboarding/ocr` bersama foto untuk validasi Dukcapil
- Confidence score < 80% → minta ambil ulang foto
- Mask NIK di log: `3174****0001`

---

## 3. ML Kit — Face Liveness Detection

### Dependency (perlu persetujuan)
```kotlin
implementation("com.google.mlkit:face-detection:16.1.7")
```

### Liveness Challenge Flow
```
1. Start kamera depan (front-facing)
2. Detect face via FaceDetector (PERFORMANCE_MODE_ACCURATE)
3. Challenge sequence:
   a. "Tatap lurus ke kamera" → detect face centered
   b. "Kedipkan mata" → detect blink (eye open probability < 0.3 lalu > 0.7)
   c. (Opsional) "Gerakkan kepala ke kiri" → detect head rotation
4. Setiap challenge berhasil → capture frame
5. Setelah 3 challenges selesai → kirim ke backend
```

### Face Detection Options
```kotlin
val options = FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // smile, eyes open
    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE) // not needed, save perf
    .setMinFaceSize(0.3f) // face harus min 30% dari frame
    .build()
```

### Anti-Spoofing (on-device basic)
- Cek eye open probability berubah (blink): NOT static photo
- Cek face bounds berubah antar frame: NOT printed photo
- Cek lighting variance antar frame: NOT screen replay
- Untuk anti-spoofing lanjutan: kirim frame ke backend model

### Aturan
- Gunakan kamera depan (LENS_FACING_FRONT)
- Face detector dalam mode ACCURATE (bukan FAST)
- Minimal 3 challenge frames + 1 main face photo
- JANGAN simpan face data di device setelah upload
- Tampilkan oval guide overlay — wajah harus di dalam oval
- Precision score = `(leftEyeOpenProb + rightEyeOpenProb) / 2 * 100`

---

## 4. Permission Handling Pattern

### AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<uses-feature android:name="android.hardware.camera" android:required="true" />
<uses-feature android:name="android.hardware.camera.front" android:required="true" />
<uses-feature android:name="android.hardware.microphone" android:required="true" />
```

### Composable Permission Pattern
```kotlin
@Composable
fun CameraPermissionGate(
    onGranted: @Composable () -> Unit,
    onDenied: @Composable () -> Unit,
) {
    val permissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    when {
        permissionState.status.isGranted -> onGranted()
        else -> onDenied()
    }
}
```

### Aturan
- CAMERA: minta saat masuk screen Kamera Foto (screen 4)
- RECORD_AUDIO: minta saat masuk screen Video Call (screen 8B)
- Jika ditolak: tampilkan explainer + tombol ke Settings
- Jangan minta semua permission sekaligus di awal flow
- `shouldShowRationale` → tampilkan dialog penjelasan dulu

---

## 5. Credential Encryption

### Android Keystore + RSA
```kotlin
// Generate RSA key pair di Android Keystore
val keyPairGenerator = KeyPairGenerator.getInstance(
    KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore"
)
keyPairGenerator.initialize(
    KeyGenParameterSpec.Builder("bca_onboarding_key", PURPOSE_ENCRYPT or PURPOSE_DECRYPT)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
        .setDigests(KeyProperties.DIGEST_SHA256)
        .build()
)

// Encrypt credential sebelum kirim ke server
fun encryptCredential(plaintext: String, serverPublicKey: PublicKey): String {
    val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
    cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey)
    return Base64.encodeToString(cipher.doFinal(plaintext.toByteArray()), Base64.NO_WRAP)
}
```

### Aturan
- Kode akses dan PIN JANGAN pernah disimpan di SharedPreferences / Room
- Encrypt dengan RSA public key dari server sebelum kirim
- Public key di-fetch dari `/auth/public-key` endpoint, cache di memory saja
- Setelah submit berhasil, clear semua credential dari memory
- JANGAN log credential value apapun (bahkan encrypted)

---

## 6. Clipboard & Share

### Copy to Clipboard
```kotlin
fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
// Untuk Android 13+: sistem otomatis tampilkan toast
// Untuk Android 12-: tampilkan Snackbar/Toast sendiri
```

### Share Account Number
```kotlin
fun shareAccountNumber(context: Context, accountNumber: String, accountHolder: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Nomor Rekening BCA: $accountNumber a/n $accountHolder")
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan Nomor Rekening"))
}
```

### Aturan
- Clipboard: auto-clear setelah 60 detik (security, Android 13+ does this natively)
- Share: JANGAN sertakan data sensitif selain nomor rekening + nama

---

## 7. Shared ViewModel Pattern (Flow-scoped)

Seluruh data buka rekening harus persist selama flow berlangsung.

```kotlin
// Scoped ke navigation graph buka rekening
@HiltViewModel
class BukaRekeningFlowViewModel @Inject constructor(
    private val onboardingRepo: OnboardingRepository,
) : ViewModel() {

    private val _flowState = MutableStateFlow(BukaRekeningFlowState())
    val flowState: StateFlow<BukaRekeningFlowState> = _flowState.asStateFlow()

    // Setiap screen memanggil method update yang relevan
    fun setSelectedProduct(product: ProductType) { ... }
    fun setOcrResult(result: KtpOcrResult) { ... }
    fun setPersonalData(data: PersonalData) { ... }
    fun setBiometricResult(result: BiometricResult) { ... }
    fun setVideoCallResult(result: VideoCallResult) { ... }
    fun setCredentials(accessCode: String, pin: String) { ... }

    // Tiap screen mengambil slice state yang dibutuhkan
    // via derived StateFlow atau computed property
}
```

### Aturan
- ViewModel di-scope ke `navigation(route = BukaRekeningFlow)` graph
- JANGAN buat ViewModel per-screen untuk data yang shared
- State bertahan saat user navigasi back/forward dalam flow
- Clear state saat flow selesai atau dibatalkan