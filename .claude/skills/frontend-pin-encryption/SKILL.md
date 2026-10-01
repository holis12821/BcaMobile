---
name: frontend-pin-encryption
description: Enkripsi PIN dan kode akses sisi client untuk aplikasi BCA Mobile — dari mana kunci publik RSA diambil (GET /v1/auth/pin/public-key, bukan hanya assets/pin_public.pem), cache key_id, urutan fallback ke asset, dan penanganan 422 AUTH_PIN_KEY_UNKNOWN saat kunci dirotasi. Gunakan saat mengerjakan PinEncryptor, RsaEncryptor, login kode akses, POST /auth/pin/verify, ganti PIN, ganti kode akses, atau saat menemui CLIENT_PIN_KEY_MISSING dan "PIN selalu salah" setelah rotasi kunci. Trigger juga pada "pin_public.pem", "pin_encrypted", "encryption_key_id", "kunci publik PIN", "RSA-OAEP", "OAEPWithSHA-256AndMGF1Padding", "rotasi kunci", dan "AUTH_PIN_KEY_UNKNOWN". JANGAN dipakai untuk kunci biometrik EC P-256 (itu `android-biometric-keystore`), kredensial onboarding buka rekening (itu `buka-rekening-api`), atau implementasi dekripsi di server (itu repo backend).
---

# Enkripsi PIN — dari mana kuncinya datang

PIN dan kode akses **tidak pernah** dikirim apa adanya. Keduanya dienkripsi
RSA-OAEP-SHA256 dengan kunci publik server, lalu dikirim sebagai `pin_encrypted`.

Skill ini menjawab satu pertanyaan yang sempat dijawab keliru di repo ini:
**kunci publiknya diambil dari mana — asset di APK, atau response backend?**

---

## 1. Keputusan: endpoint dulu, asset cadangan

**Ambil dari `GET /v1/auth/pin/public-key`. Simpan `assets/pin_public.pem`
sebagai cadangan, bukan sebagai sumber utama.**

Keadaan sekarang di repo ini salah arah: `PinEncryptor.kt` hanya membaca asset,
dan `.claude/skills/bca-mobile-api/SKILL.md` §"Enkripsi PIN" masih menyuruh
begitu. Backend sudah menerbitkan endpointnya **dan** sudah menerapkan
`PIN_KEY_ID`, jadi panduan lama itu sudah usang.

Kenapa bukan asset saja:

- **Kunci dirotasi.** Backend menamai pasangan kunci aktif dengan `PIN_KEY_ID`
  (`pin-key-v1`). Saat dinaikkan, APK yang memegang PEM lama mengenkripsi dengan
  kunci yang sudah tidak dipegang server. Server tidak bisa mendekripsinya, dan
  gejalanya di layar adalah **"PIN salah"** — untuk PIN yang benar. Perbaikannya
  rilis baru ke Play Store dan menunggu semua orang update.
- **Ada endpointnya, publik, tanpa token.** Tidak ada alasan menebak.

Kenapa asset tetap perlu:

- **Login pertama bisa terjadi tanpa jaringan yang sehat.** Kalau endpoint tidak
  terjangkau dan tidak ada cadangan, nasabah tidak bisa masuk sama sekali.
  Asset membuat keadaan itu turun dari "mati total" menjadi "mati hanya kalau
  kuncinya juga sudah dirotasi".
- Asset adalah **cadangan**, bukan kebenaran. Begitu endpoint menjawab, nilainya
  yang dipakai dan disimpan.

Urutannya:

```
1. Cache di memori (masih segar?)          → pakai
2. Cache tersimpan (DataStore/SharedPrefs) → pakai, lalu segarkan di latar
3. GET /v1/auth/pin/public-key             → pakai + simpan
4. assets/pin_public.pem                   → pakai, JANGAN disimpan sebagai cache
5. Tidak ada satu pun                      → CLIENT_PIN_KEY_MISSING (jangan kirim PIN)
```

Langkah 5 tidak boleh dilewati. PIN apa adanya tidak pernah menjadi pilihan,
bahkan saat semua yang lain gagal.

---

## 2. Kontrak endpoint

```
GET /v1/auth/pin/public-key      (publik, tanpa Authorization)
Cache-Control: max-age=300
```

```json
{
  "status": "success",
  "data": {
    "algorithm": "RSA-OAEP-SHA256",
    "key_id": "pin-key-v1",
    "public_key_pem": "-----BEGIN PUBLIC KEY-----\nMIIBIjANBg…\n-----END PUBLIC KEY-----\n",
    "payload_shape": "{\"pin\":\"123456\",\"nonce\":\"<uuid-v4>\",\"ts\":<unix-seconds>}",
    "encoding": "base64(RSA-OAEP-SHA256(json))",
    "max_skew_sec": 60
  }
}
```

Bentuknya **identik** dengan `GET /onboarding/credentials/public-key`, dan
kuncinya **sama** — satu pasangan RSA-2048, satu `key_id`. Satu jalur kode
client cukup untuk keduanya; `OnboardingApi.getPublicKey()` yang sudah ada
memakai response yang sama persis.

### Yang dienkripsi

Bukan PIN telanjang, melainkan JSON:

```json
{"pin":"123456","nonce":"<uuid-v4>","ts":1790335258}
```

- `nonce` UUID v4 **baru setiap permintaan** — server mengingatnya 120 detik dan
  menolak pengulangan.
- `ts` detik Unix. Selisih lebih dari 60 detik ditolak, jadi jam perangkat yang
  meleset jauh akan gagal login. Kalau muncul laporan "PIN salah" yang aneh pada
  satu perangkat, periksa jamnya sebelum menyalahkan kunci.
- Hasilnya base64 tanpa pembungkus baris (`Base64.NO_WRAP`).

Transformation di Android — jangan diubah:

```kotlin
"RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
```

Server **menolak** PKCS#1 v1.5 dan ada test di backend yang menjaganya, jadi
salah padding tidak akan pernah "kebetulan jalan".

---

## 3. Kirim `encryption_key_id`

Setiap request ber-PIN boleh membawa field ini:

```json
{
  "device_id": "…",
  "pin_encrypted": "…",
  "encryption_key_id": "pin-key-v1"
}
```

Field ini **opsional** di server, tapi kirimkan. Tanpa itu, kunci yang sudah
kedaluwarsa terbaca sebagai PIN salah — dan PIN salah punya lockout, jadi
nasabah bisa terkunci karena kesalahan yang sama sekali bukan miliknya.

Dengan field itu, jawabannya jelas:

```
422 AUTH_PIN_KEY_UNKNOWN
details.expected_key_id = "pin-key-v2"
```

Berlaku di `/auth/login/pin`, `/auth/pin/verify`, `/auth/pin/change`, dan
`/auth/access-code/change`.

> `POST /onboarding/credentials` memakai kode yang berbeda untuk keadaan yang
> sama: **`CRED_DECRYPTION_FAILED`**, juga dengan `details.expected_key_id`.
> Tangani keduanya.

---

## 4. Penanganan `AUTH_PIN_KEY_UNKNOWN`: segarkan lalu ulang SEKALI

```kotlin
// Inti perilakunya, bukan potongan siap tempel.
suspend fun <T> withPinKeyRetry(block: suspend () -> DataResult<T>): DataResult<T> {
    val first = block()
    val failure = (first as? DataResult.Failure)?.error
    val stale = failure is ApiFailure.Business &&
        (failure.code == "AUTH_PIN_KEY_UNKNOWN" || failure.code == "CRED_DECRYPTION_FAILED")

    if (!stale) return first

    // Buang cache, ambil kunci baru, enkripsi ulang, coba sekali lagi.
    pinKeyProvider.invalidate()
    return block()
}
```

Tiga aturan:

1. **Sekali saja.** Gagal lagi berarti masalahnya bukan kunci basi; mengulang
   terus hanya menghabiskan jatah rate limit.
2. **Enkripsi ulang dari awal.** `pin_encrypted` lama tidak bisa dipakai — nonce
   dan `ts`-nya sudah hangus, dan kuncinya memang berbeda.
3. **Jangan hitung sebagai PIN salah.** Layar tidak boleh menampilkan sisa
   percobaan atau peringatan lockout untuk kegagalan ini.

---

## 5. Perubahan yang perlu di `PinEncryptor.kt`

Sekarang (`core/security/PinEncryptor.kt`) kunci dibaca sekali dari asset lewat
`by lazy` dan `isAvailable` menjadi false selamanya bila file itu tidak ada.
Yang perlu berubah:

| Sekarang | Jadi |
|---|---|
| `publicKeyPem` dari `context.assets` saja | provider dengan urutan §1 |
| `by lazy` — sekali seumur proses | cache ber-TTL (mis. 5 menit, mengikuti `Cache-Control` endpoint) + bisa di-`invalidate()` |
| `fun encrypt(plaintext): String?` | `suspend fun encrypt(plaintext): EncryptedPin?` yang membawa `keyId` |
| `isAvailable` dihitung dari asset | dihitung dari hasil provider |

`encrypt` menjadi `suspend` karena ia mungkin memanggil jaringan. Pemanggilnya
(`AuthRepositoryImpl.loginWithAccessCode`, `verifyPin`, `changePin`,
`changeAccessCode`) semuanya sudah `suspend`, jadi tidak ada yang perlu dipaksa.

Kembalikan `keyId` bersama ciphertext-nya:

```kotlin
data class EncryptedPin(val ciphertext: String, val keyId: String?)
```

Tanpa itu, `encryption_key_id` yang dikirim bisa saja bukan kunci yang benar-benar
dipakai mengenkripsi — dan itu justru menciptakan kegagalan yang hendak dicegah.

`RsaEncryptor` **tidak perlu diubah**: ia sudah menerima PEM sebagai parameter,
bukan membacanya sendiri.

### Yang harus ditambahkan ke `AuthApi`

```kotlin
@GET("auth/pin/public-key")
suspend fun pinPublicKey(): Response<ApiEnvelope<PublicKeyResponse>>
```

Pakai ulang `PublicKeyResponse` milik onboarding — bentuknya sama persis.
Endpoint ini publik, jadi pastikan interceptor tidak memaksakan `Authorization`
dan kegagalannya tidak memicu refresh token.

---

## 6. Menyiapkan `assets/pin_public.pem`

Berkas ini **belum ada** di repo. Ambil dari backend:

```bash
# di repo bca-mobile-api
make pin-public-key      # mencetak PEM + key_id yang aktif
```

Salin keluarannya ke `app/src/main/assets/pin_public.pem`. Berkas ini kunci
**publik** — tidak rahasia — tetapi versinya penting: catat `key_id`-nya di
commit message supaya jelas cadangan ini milik kunci yang mana.

Perbarui berkas ini setiap kali backend merotasi kunci. Cadangan yang basi tidak
berbahaya (endpoint tetap menang), tapi ia hanya berguna kalau masih cocok.

---

## 7. Salah kaprah yang sudah pernah terjadi

- **"Kode akses beda kunci dengan PIN."** Tidak. Satu pasangan kunci untuk
  keduanya, dan sama pula dengan kredensial onboarding. Yang berbeda adalah
  **endpoint dan tujuannya**: kode akses untuk masuk, PIN untuk menyetujui
  transaksi.
- **"Kalau `pin_public.pem` tidak ada, kirim saja PIN-nya, nanti server yang
  mengurus."** Tidak pernah. `CLIENT_PIN_KEY_MISSING` sudah ada di
  `AuthRepositoryImpl` untuk keadaan itu dan harus tetap ada.
- **"Cache kunci di memori saja cukup."** Proses Android dimatikan kapan saja;
  cache memori berarti panggilan jaringan tambahan pada setiap cold start, tepat
  di layar login. Simpan juga secara persisten.
- **"PIN salah terus setelah rilis backend."** Periksa `key_id` lebih dulu:
  panggil endpointnya dan bandingkan dengan yang dipakai client. Ini gejala
  kunci basi, bukan gejala PIN.

---

## 8. Definition of Done

- [ ] `GET /v1/auth/pin/public-key` dipanggil dan hasilnya di-cache beserta `key_id`
- [ ] `assets/pin_public.pem` ada dan hanya dipakai saat endpoint gagal
- [ ] `encryption_key_id` ikut dikirim di keempat endpoint ber-PIN
- [ ] `AUTH_PIN_KEY_UNKNOWN` dan `CRED_DECRYPTION_FAILED` memicu satu kali ambil-ulang + enkripsi ulang
- [ ] Kegagalan kunci **tidak** ditampilkan sebagai "PIN salah" dan tidak mengurangi sisa percobaan
- [ ] Tidak ada jalur kode yang mengirim PIN tanpa enkripsi, termasuk saat semua sumber kunci gagal
- [ ] `nonce` baru dan `ts` segar di setiap permintaan — tidak ada ciphertext yang dipakai dua kali

Kontrak server: `docs/01-API-SPECIFICATION.md` §2 di repo `bca-mobile-api`.
Sisi biometrik ada di skill `android-biometric-keystore`.
