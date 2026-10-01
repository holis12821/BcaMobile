package id.bca.bcamobile.core.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enkripsi PIN dan kode akses untuk endpoint bernasabah.
 *
 * Yang dienkripsi **bukan** PIN telanjang, melainkan amplop JSON:
 *
 * ```json
 * {"pin":"123456","nonce":"<uuid-v4>","ts":1790335258}
 * ```
 *
 * `nonce` baru di setiap permintaan (server mengingatnya 120 detik dan menolak
 * pengulangan) dan `ts` detik Unix dengan toleransi 60 detik. Tanpa keduanya satu
 * `pin_encrypted` yang tersadap bisa diputar ulang selamanya. Bentuk ini dijaga
 * test di backend — lihat `internal/pkg/crypto/rsa.go`.
 *
 * Kuncinya datang dari [PinKeyProvider], bukan dari asset saja: kunci server
 * dirotasi, dan APK yang memegang PEM lama akan membuat server membalas "PIN
 * salah" untuk PIN yang benar.
 */
@Singleton
class PinEncryptor @Inject constructor(
    private val keyProvider: PinKeyProvider,
    private val rsaEncryptor: RsaEncryptor,
) {

    /**
     * @return ciphertext base64 beserta `key_id` kunci yang dipakai, atau null
     *   bila tidak ada kunci sama sekali. Null berarti operasi berbasis PIN
     *   **gagal** — mengirim PIN apa adanya tidak pernah menjadi pilihan.
     */
    suspend fun encrypt(plaintext: String): EncryptedPin? {
        val key = keyProvider.current() ?: return null
        val ciphertext = rsaEncryptor.encrypt(buildPinPayload(plaintext), key.pem) ?: return null
        return EncryptedPin(ciphertext = ciphertext, keyId = key.keyId)
    }

    /** Dipakai setelah server menolak kunci; pemanggil lalu mengenkripsi ulang. */
    fun invalidateKey() = keyProvider.invalidate()
}
