package id.bca.bcamobile.core.security

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Amplop yang dienkripsi untuk setiap rahasia — PIN, kode akses, maupun
 * kredensial onboarding:
 *
 * ```json
 * {"pin":"123456","nonce":"<uuid-v4>","ts":1790335258}
 * ```
 *
 * Server membongkarnya lewat satu jalur yang sama (`DecryptPIN` di
 * `internal/pkg/crypto/rsa.go`), jadi rahasia yang dikirim tanpa amplop ini
 * gagal dengan `AUTH_INVALID_PIN` atau `CRED_DECRYPTION_FAILED` — bukan dengan
 * pesan yang menyebut bentuk payload.
 *
 * `nonce` diingat server 120 detik dan `ts` ditolak bila selisihnya lebih dari
 * 60 detik, jadi keduanya **wajib** dibuat ulang setiap permintaan. Memakai
 * ulang satu ciphertext berarti permintaan kedua ditolak sebagai replay.
 */
@Serializable
internal data class PinPayload(
    val pin: String,
    val nonce: String,
    @SerialName("ts") val timestampSeconds: Long,
)

private val json = Json { encodeDefaults = true }

private const val MILLIS_PER_SECOND = 1000L

/** Membungkus [secret] dengan nonce dan timestamp segar, siap dienkripsi RSA. */
internal fun buildPinPayload(secret: String): String = json.encodeToString(
    PinPayload(
        pin = secret,
        nonce = UUID.randomUUID().toString(),
        timestampSeconds = System.currentTimeMillis() / MILLIS_PER_SECOND,
    ),
)
