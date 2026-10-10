package id.bca.bcamobile.core.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kunci pengikat perangkat untuk payload yang harus dibuktikan asalnya.
 *
 * **Berbeda dari [BiometricKeyManager] dan tidak boleh disatukan dengannya.** Kunci
 * login biometrik dibuat dengan `setUserAuthenticationRequired(true)`, jadi setiap
 * pemakaiannya menuntut BiometricPrompt. Itu benar untuk login, tapi mustahil untuk
 * buka rekening: nasabah baru belum tentu punya biometrik perangkat terdaftar, dan
 * memunculkan dialog sistem di tengah tantangan liveness akan menutup kameranya.
 *
 * Kunci ini karena itu **tanpa syarat autentikasi pengguna**. Yang dibuktikannya pun
 * lebih sempit dan harus dibaca apa adanya: payload ini datang dari instalasi aplikasi
 * yang memegang kunci ini di perangkat ini — bukan bahwa penggunanya hadir. Jaminan
 * "pengguna hidup dan hadir" datang dari verifikasi server atas frame-nya, bukan dari
 * tanda tangan ini.
 */
@Suppress("DEPRECATION")
@Singleton
class DeviceKeyManager @Inject constructor(
    private val context: Context,
) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val keyStore: KeyStore
        get() = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    /** Pengenal kunci yang dikirim ke server, dibuat sekali bersama kuncinya. */
    fun keyId(): String = prefs.getString(KEY_ID, null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString(KEY_ID, it).apply()
    }

    /** X.509 SubjectPublicKeyInfo base64, tanpa pembungkus PEM. */
    fun publicKeyBase64(): String? = runCatching {
        ensureKey()
        val certificate = keyStore.getCertificate(KEY_ALIAS) ?: return null
        Base64.encodeToString(certificate.publicKey.encoded, Base64.NO_WRAP)
    }.getOrNull()

    /**
     * Menandatangani [payload] dengan SHA256withECDSA.
     *
     * Mengembalikan null bila Keystore perangkat menolak — pemanggil **tidak boleh**
     * mengirim payload tanpa tanda tangan sebagai jalan pintas; server akan menolaknya,
     * dan itu memang perilaku yang benar.
     */
    fun sign(payload: String): String? = runCatching {
        ensureKey()
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as? PrivateKey ?: return null
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM).apply {
            initSign(privateKey)
            update(payload.toByteArray())
        }
        Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
    }.getOrNull()

    private fun ensureKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) return
        val builder = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec(CURVE))
            .setDigests(KeyProperties.DIGEST_SHA256)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { builder.setIsStrongBoxBacked(true) }
        }
        runCatching {
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE).apply {
                initialize(builder.build())
                generateKeyPair()
            }
        }.onFailure {
            // StrongBox tidak tersedia di sebagian besar perangkat; ulangi tanpa itu.
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE).apply {
                initialize(
                    KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                        .setAlgorithmParameterSpec(ECGenParameterSpec(CURVE))
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .build(),
                )
                generateKeyPair()
            }
        }
        keyId()
    }

    companion object {
        /** Nama algoritma seperti yang dibaca server. */
        const val WIRE_ALGORITHM = "EC-P256"

        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "bca_device_binding_v1"
        private const val CURVE = "secp256r1"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        private const val PREFS_FILE = "device_binding_key"
        private const val KEY_ID = "key_id"
    }
}
