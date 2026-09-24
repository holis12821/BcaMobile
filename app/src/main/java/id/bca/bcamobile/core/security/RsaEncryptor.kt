package id.bca.bcamobile.core.security

import android.util.Base64
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enkripsi kredensial (kode akses dan PIN) dengan kunci publik server.
 *
 * Algoritma dikunci ke RSA-OAEP-SHA256 sesuai kontrak backend. Kunci publik
 * diambil runtime dari `GET /credentials/public-key` — tidak pernah ditanam di APK.
 */
@Singleton
class RsaEncryptor @Inject constructor() {

    /** @return ciphertext base64, atau null bila PEM tidak bisa dipakai. */
    fun encrypt(plaintext: String, publicKeyPem: String): String? = runCatching {
        val keySpec = X509EncodedKeySpec(decodePem(publicKeyPem))
        val publicKey = KeyFactory.getInstance(ALGORITHM).generatePublic(keySpec)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        Base64.encodeToString(cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }.getOrNull()

    private fun decodePem(pem: String): ByteArray {
        val body = pem
            .replace(PEM_HEADER, "")
            .replace(PEM_FOOTER, "")
            .replace("\\s".toRegex(), "")
        return Base64.decode(body, Base64.DEFAULT)
    }

    private companion object {
        const val ALGORITHM = "RSA"
        const val TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        const val PEM_HEADER = "-----BEGIN PUBLIC KEY-----"
        const val PEM_FOOTER = "-----END PUBLIC KEY-----"
    }
}
