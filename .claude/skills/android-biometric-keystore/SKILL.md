---
name: android-biometric-keystore
description: Login biometrik Android dengan AndroidKeyStore dan BiometricPrompt untuk bca_mobile — pembuatan pasangan kunci EC P-256 yang terkunci biometrik, penandatanganan challenge server (SHA256withECDSA), CryptoObject, key attestation, penanganan kunci hangus saat sidik jari baru didaftarkan, dan penyambungan ke endpoint /auth/biometric/challenge, /auth/biometric/register, serta /auth/login/biometric. Gunakan saat mengerjakan layar Face ID atau Touch ID, pendaftaran biometrik, penandatanganan challenge, atau kegagalan login biometrik. Trigger juga pada "login sidik jari", "fingerprint", "FingerPrintScreen", "FaceIdScreen", "KeyPermanentlyInvalidatedException", "signed_challenge", "key attestation", dan "tanda tangan challenge". JANGAN gunakan untuk liveness dan face matching e-KYC saat buka rekening (itu skill `buka-rekening-native-android`), enkripsi PIN dan kode akses (itu `RsaEncryptor` dan `PinEncryptor`), atau navigasi dan batas ViewModel (itu `compose-architecture`).
---

# Login Biometrik — AndroidKeyStore

Biometrik di sini **bukan** mengirim data biometrik ke server. Yang dikirim adalah
tanda tangan digital atas challenge dari server, memakai kunci privat yang terkunci
di dalam perangkat keras dan hanya bisa dipakai setelah pengguna lolos verifikasi
biometrik. Sidik jari dan wajah tidak pernah meninggalkan perangkat.

**Trigger**: layar Face ID atau Touch ID, pendaftaran biometrik, penandatanganan
challenge, `KeyPermanentlyInvalidatedException`, atau kegagalan login biometrik.

**Jangan trigger** untuk:

| Wilayah | Skill |
|---|---|
| Liveness dan face matching saat buka rekening | `buka-rekening-native-android` |
| Enkripsi PIN dan kode akses | `RsaEncryptor`, `PinEncryptor` di `core/security/` |
| Navigasi, batas ViewModel, keamanan sesi | `compose-architecture` |
| Backend `/auth/biometric/*` | Lihat `docs/backend/01-API-SPECIFICATION.md` §2 |

---

## Sebelum menulis kode: tiga hal yang belum siap

Skill ini mendeskripsikan cara yang benar, tetapi **implementasinya belum bisa
diselesaikan** sampai tiga hal ini beres. Jangan mulai tanpa memeriksa ulang.

1. **`androidx.biometric` belum jadi dependency.** `gradle/libs.versions.toml` tidak
   memuatnya. CLAUDE.md melarang menambah dependency tanpa persetujuan — minta dulu.
2. **`MainActivity` adalah `ComponentActivity`, bukan `FragmentActivity`.**
   `androidx.biometric.BiometricPrompt` mensyaratkan `FragmentActivity`. Mengubah
   kelas induknya menyentuh titik masuk aplikasi, jadi perlu disepakati lebih dulu.
3. **Algoritma tanda tangan belum ditetapkan backend.** Spec tidak menyebut jenis
   kunci, algoritma, maupun format `signed_challenge`. Daftar pertanyaannya ada di
   `docs/backend/10-HANDOVER-BLOCKER-BACKEND.md` butir 2. Menebak berarti server
   akan menolak setiap tanda tangan, dan gejalanya sulit dibedakan dari PIN salah.

Nilai yang dipakai di seluruh dokumen ini adalah **rekomendasi**, bukan kontrak yang
sudah disetujui: EC P-256, `SHA256withECDSA`, `signed_challenge` base64 dari DER.

---

## Alur lengkap

```text
PENDAFTARAN (sekali, setelah login PIN berhasil)
  1. Buat pasangan kunci di AndroidKeyStore, terkunci biometrik
  2. Ambil kunci publik + rantai attestation
  3. POST /auth/biometric/register  { public_key, key_id, attestation, device_id }

LOGIN (setiap kali)
  1. GET /auth/biometric/challenge?device_id=...  → { challenge_id, challenge }
  2. Signature.initSign(privateKey)        ← bisa lempar KeyPermanentlyInvalidated
  3. BiometricPrompt.authenticate(CryptoObject(signature))
  4. signature.update(challenge); signature.sign()
  5. POST /auth/login/biometric  { challenge_id, signed_challenge, key_id, device_id }
```

Langkah 3 yang membuat ini aman: kunci privat **tidak bisa dipakai** sebelum
sistem operasi memverifikasi biometrik. Aplikasi tidak pernah melihat hasil
verifikasinya, hanya menerima objek `Signature` yang sudah boleh dipakai.

---

## 1. Membuat kunci

```kotlin
private const val KEY_ALIAS = "bca_biometric_login_v1"
private const val KEYSTORE = "AndroidKeyStore"

fun generateKeyPair(attestationChallenge: ByteArray): KeyPair {
    val generator = KeyPairGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_EC,
        KEYSTORE,
    )

    val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
        .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
        .setDigests(KeyProperties.DIGEST_SHA256)
        // Kunci hanya boleh dipakai setelah verifikasi biometrik.
        .setUserAuthenticationRequired(true)
        // Menambah sidik jari baru menghanguskan kunci — lihat bagian 5.
        .setInvalidatedByBiometricEnrollment(true)
        // Challenge dari server mengikat attestation ke sesi pendaftaran ini.
        .setAttestationChallenge(attestationChallenge)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // 0 detik = wajib autentikasi setiap kali kunci dipakai.
                setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            }
        }
        .build()

    generator.initialize(spec)
    return generator.generateKeyPair()
}
```

### Yang mudah salah

- **`setUserAuthenticationValidityDurationSeconds` jangan dipakai.** Nilai positif
  membuat kunci bisa dipakai berulang dalam rentang waktu itu **tanpa** biometrik,
  dan membuat `CryptoObject` tidak berlaku. Di bawah API 30, cukup tidak memanggilnya
  sama sekali — perilaku bawaannya sudah "wajib autentikasi setiap kali".
- **StrongBox tidak ada di semua perangkat.** `setIsStrongBoxBacked(true)` hanya API 28+
  dan melempar `StrongBoxUnavailableException` di perangkat yang tidak punya. Kalau
  dipakai, bungkus dan ulangi tanpa StrongBox:

  ```kotlin
  runCatching { generateWithStrongBox() }
      .recoverCatching { if (it is StrongBoxUnavailableException) generateWithoutStrongBox() else throw it }
  ```

- **Attestation juga bisa tidak didukung** di perangkat lama dan emulator.
  Tangani kegagalannya, jangan biarkan pendaftaran gagal total tanpa penjelasan —
  kebijakan penolakannya milik backend (butir 2 dokumen handover).
- **minSdk project ini 26**, jadi setiap API 28+ dan 30+ wajib dijaga
  `Build.VERSION.SDK_INT`.

---

## 2. Mengambil kunci publik dan attestation

```kotlin
fun publicKeyBase64(): String? {
    val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
    val certificate = keyStore.getCertificate(KEY_ALIAS) ?: return null
    // X.509 SubjectPublicKeyInfo, tanpa header PEM.
    return Base64.encodeToString(certificate.publicKey.encoded, Base64.NO_WRAP)
}

fun attestationChain(): List<String> {
    val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
    val chain = keyStore.getCertificateChain(KEY_ALIAS) ?: return emptyList()
    return chain.map { Base64.encodeToString(it.encoded, Base64.NO_WRAP) }
}
```

Spec menulis `attestation` sebagai satu string base64, sedangkan attestation
sebenarnya adalah **rantai** sertifikat. Tanyakan backend: satu string berisi
sertifikat daun saja, atau rantai yang digabung. Jangan diputuskan sendiri.

---

## 3. Memeriksa ketersediaan sebelum menawarkan

```kotlin
fun biometricAvailability(context: Context): Int =
    BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
```

| Hasil | Arti | Yang ditampilkan |
|---|---|---|
| `BIOMETRIC_SUCCESS` | Siap | Tawarkan login biometrik |
| `BIOMETRIC_ERROR_NONE_ENROLLED` | Belum ada sidik jari/wajah terdaftar | Arahkan ke pengaturan sistem |
| `BIOMETRIC_ERROR_NO_HARDWARE` | Perangkat tidak punya sensor | Sembunyikan opsi biometrik |
| `BIOMETRIC_ERROR_HW_UNAVAILABLE` | Sensor sementara tidak bisa dipakai | Tawarkan kode akses |
| `BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED` | Butuh pembaruan keamanan | Tawarkan kode akses |

**Wajib `BIOMETRIC_STRONG`.** `BIOMETRIC_WEAK` tidak bisa dipakai bersama
`CryptoObject`, jadi tidak bisa menandatangani apa pun. Jangan menurunkan ke
`BIOMETRIC_WEAK` hanya supaya lebih banyak perangkat lolos — itu menghilangkan
seluruh jaminan keamanannya.

---

## 4. Menandatangani challenge

```kotlin
suspend fun signChallenge(
    activity: FragmentActivity,
    challenge: ByteArray,
): SignResult {
    val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
    val privateKey = keyStore.getKey(KEY_ALIAS, null) as? PrivateKey
        ?: return SignResult.NeedsRegistration

    val signature = Signature.getInstance("SHA256withECDSA")
    try {
        signature.initSign(privateKey)
    } catch (e: KeyPermanentlyInvalidatedException) {
        // Sidik jari atau wajah baru didaftarkan — kunci lama tidak berlaku lagi.
        deleteKey()
        return SignResult.NeedsRegistration
    }

    val authenticated = promptBiometric(activity, BiometricPrompt.CryptoObject(signature))
        ?: return SignResult.Cancelled

    val signed = authenticated.signature ?: return SignResult.Failed
    signed.update(challenge)
    return SignResult.Success(Base64.encodeToString(signed.sign(), Base64.NO_WRAP))
}
```

Ketentuan:

- `challenge` dari server datang sebagai **base64**. Decode dulu sebelum
  `signature.update()`. Menandatangani string base64-nya menghasilkan tanda tangan
  yang tidak akan diterima server.
- Objek `Signature` yang dipakai untuk menandatangani **harus** yang keluar dari
  `result.cryptoObject`, bukan variabel asli. Keduanya memang objek yang sama,
  tetapi mengambilnya dari hasil membuat asumsinya eksplisit.
- Sekali pakai. Satu challenge, satu tanda tangan, satu percobaan login.

### PromptInfo

```kotlin
BiometricPrompt.PromptInfo.Builder()
    .setTitle(getString(R.string.biometric_prompt_title))
    .setSubtitle(getString(R.string.biometric_prompt_subtitle))
    .setNegativeButtonText(getString(R.string.biometric_prompt_use_access_code))
    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    .setConfirmationRequired(true)
    .build()
```

- `setNegativeButtonText` **wajib** saat hanya `BIOMETRIC_STRONG` yang diizinkan;
  tanpa itu `build()` melempar.
- `setDeviceCredentialAllowed` sudah usang, dan menggabungkan kredensial perangkat
  dengan `CryptoObject` baru didukung API 30+. Untuk login perbankan, biarkan
  hanya biometrik — jalur cadangannya kode akses di layar kita sendiri.
- Semua teks lewat `strings.xml` sesuai aturan wajib #3 CLAUDE.md.

---

## 5. Kunci hangus — kasus yang paling sering terlewat

`setInvalidatedByBiometricEnrollment(true)` berarti kunci **otomatis hangus** ketika
pengguna menambah sidik jari atau wajah baru. Ini fitur, bukan gangguan: tanpa itu,
orang lain yang berhasil menambahkan sidik jarinya ke perangkat bisa memakai kunci
login nasabah.

Gejalanya `KeyPermanentlyInvalidatedException` saat `initSign`. Penanganannya:

1. Hapus kunci lama: `keyStore.deleteEntry(KEY_ALIAS)`
2. Jangan tampilkan pesan teknis. Beri tahu bahwa biometrik perlu didaftarkan ulang.
3. Minta login dengan kode akses lebih dulu — `/auth/biometric/register` butuh
   Bearer Token.
4. Daftarkan ulang, lalu beri tahu server agar kunci lama dicabut.

Pemicu lain yang menghanguskan kunci: kunci layar dimatikan lalu dinyalakan lagi,
dan reset pabrik. Perlakukan semuanya sama — daftar ulang, jangan mencoba pulih.

---

## 6. Penyambungan ke kode yang sudah ada

Lapisan data sudah siap. `AuthRepository` di `domain/auth/` sudah punya:

```kotlin
suspend fun biometricChallenge(): DataResult<BiometricChallenge>

suspend fun loginWithBiometric(
    type: BiometricType,      // FACE_ID | FINGERPRINT
    challengeId: String,
    signedChallenge: String,  // hasil bagian 4
    keyId: String,
): DataResult<AuthUser>
```

Yang belum ada:

- `BiometricKeyManager` di `core/security/` — pembuatan kunci, kunci publik,
  attestation, penghapusan
- `BiometricSigner` — pembungkus `BiometricPrompt` yang mengembalikan tanda tangan
- `AuthRepository.registerBiometric(...)` beserta endpointnya di `AuthApi`
- `FaceIdViewModel` dan `TouchIdViewModel`

`FaceIdScreen` dan `TouchIdScreen` sudah stateless dengan `FaceIdUiState(status)` dan
`TouchIdUiState(status)`, jadi tidak perlu mengubah signature composable — cukup
memetakan status ke `SCANNING` / `SUCCESS` / `FAILED` dan menyambungkan di
`AuthGraph`, seperti layar lain.

### Di mana `BiometricPrompt` dipanggil

`BiometricPrompt` butuh `FragmentActivity` dan **bukan** urusan composable. Pola yang
dipakai: composable memanggil lambda, `AuthGraph` mengambil activity dari
`LocalContext`, ViewModel yang mengurus sisanya. Jangan memanggil `BiometricPrompt`
dari dalam `*Screen.kt` — itu melanggar aturan wajib #4 CLAUDE.md.

---

## Aturan wajib

1. **Data biometrik tidak pernah dikirim, disimpan, atau di-log.** Yang berpindah
   hanya tanda tangan dan kunci publik.
2. **Kunci privat tidak pernah diekspor.** `KeyStore.getKey` mengembalikan
   pegangan, bukan bahan kuncinya. Tidak ada jalur kode yang boleh mencoba
   mengeluarkannya.
3. **Selalu `BIOMETRIC_STRONG`.** Tanpa itu `CryptoObject` tidak berlaku.
4. **Jangan diam saat gagal.** Setiap kegagalan biometrik berakhir di satu dari dua
   tempat: mencoba lagi, atau kembali ke kode akses. Tidak ada jalan buntu.
5. **Satu challenge satu tanda tangan.** Jangan pernah memakai ulang challenge.
6. **Alias kunci berversi** (`..._v1`). Mengganti parameter kunci berarti alias baru,
   bukan menimpa yang lama.
7. **Teks lewat `strings.xml`**, termasuk seluruh teks di `PromptInfo`.
8. **Jangan menambah dependency tanpa persetujuan** — termasuk `androidx.biometric`.

---

## Uji yang wajib ada

Emulator tidak cukup. Yang berikut hanya terlihat di perangkat asli:

- [ ] Pendaftaran berhasil, kunci publik terkirim, server menerima
- [ ] Login berhasil dengan sidik jari terdaftar
- [ ] Login ditolak saat sidik jari lain dipakai
- [ ] **Tambah sidik jari baru → kunci hangus → alur daftar ulang berjalan**
- [ ] Batal di dialog biometrik → kembali ke kode akses, bukan layar kosong
- [ ] Perangkat tanpa biometrik terdaftar → opsi biometrik tidak ditawarkan
- [ ] Challenge kedaluwarsa (lebih dari 60 detik) → pesan yang bisa dipahami
- [ ] Perangkat tanpa StrongBox tetap bisa mendaftar

Butir keempat yang paling sering lolos dari pengujian dan paling sering muncul
sebagai keluhan "tiba-tiba tidak bisa login".