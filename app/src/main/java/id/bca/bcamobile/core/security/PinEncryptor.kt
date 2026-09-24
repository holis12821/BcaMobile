package id.bca.bcamobile.core.security

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enkripsi PIN dan kode akses untuk endpoint bernasabah.
 *
 * Kunci publik dibaca dari `assets/pin_public.pem`, berbeda dengan onboarding yang
 * mengambilnya dari `GET /onboarding/credentials/public-key`. Pembedaan ini mengikuti
 * `.claude/skills/bca-mobile-api/SKILL.md` §3.
 *
 * **Berkas `assets/pin_public.pem` belum ada di repo** — harus disediakan tim backend.
 * Selama belum ada, [encrypt] mengembalikan null dan pemanggil wajib memperlakukannya
 * sebagai kegagalan, bukan mengirim PIN apa adanya.
 */
@Singleton
class PinEncryptor @Inject constructor(
    private val context: Context,
    private val rsaEncryptor: RsaEncryptor,
) {

    private val publicKeyPem: String? by lazy {
        runCatching {
            context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        }.getOrNull()
    }

    /** true bila kunci tersedia; false berarti fitur berbasis PIN belum bisa dipakai. */
    val isAvailable: Boolean get() = !publicKeyPem.isNullOrBlank()

    /** Base64 hasil RSA-OAEP-SHA256, atau null bila kunci tidak tersedia. */
    fun encrypt(plaintext: String): String? {
        val pem = publicKeyPem?.takeIf { it.isNotBlank() } ?: return null
        return rsaEncryptor.encrypt(plaintext, pem)
    }

    private companion object {
        const val ASSET_NAME = "pin_public.pem"
    }
}
