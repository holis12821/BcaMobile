package id.bca.bcamobile.core.security

import id.bca.bcamobile.core.device.DeviceIdProvider
import javax.inject.Inject
import javax.inject.Singleton

/** Kunci perangkat yang didaftarkan ke server bersama tantangan. */
data class DevicePublicKey(
    val keyId: String,
    val base64: String,
)

/**
 * Semua yang mengikat satu pengiriman bukti liveness ke perangkat ini.
 *
 * Dipisah jadi antarmuka karena isinya **seluruhnya** bergantung Android: AndroidKeyStore,
 * Play Integrity, dan `Settings.Secure.ANDROID_ID`. Tanpa batas ini, aturan paling penting
 * di ViewModel — payload tanpa tanda tangan tidak dikirim, dan keputusan lulus datang dari
 * server — hanya bisa diuji di perangkat, yaitu tidak diuji.
 */
interface LivenessAttestor {

    fun deviceId(): String

    /** null bila Keystore perangkat menolak membuat kunci. */
    fun publicKey(): DevicePublicKey?

    /** null bila penandatanganan gagal. Pemanggil **tidak boleh** mengirim tanpa ini. */
    fun sign(payload: String): String?

    /**
     * Token integritas untuk [nonce], atau null bila belum dikonfigurasi/gagal.
     *
     * Null bukan keputusan: server yang memutuskan apa artinya token yang tidak ada,
     * sesuai kebijakan lingkungannya (keputusan Q3).
     */
    suspend fun integrityToken(nonce: String): String?

    fun riskSignals(): DeviceRiskSignals

    val signatureAlgorithm: String
}

/** Implementasi nyata; satu-satunya tempat ketiga sumber Android itu dirangkai. */
@Singleton
class AndroidLivenessAttestor @Inject constructor(
    private val keys: DeviceKeyManager,
    private val integrity: DeviceIntegrityProvider,
    private val deviceIds: DeviceIdProvider,
) : LivenessAttestor {

    override val signatureAlgorithm: String = DeviceKeyManager.WIRE_ALGORITHM

    override fun deviceId(): String = deviceIds.deviceId()

    override fun publicKey(): DevicePublicKey? {
        val base64 = keys.publicKeyBase64() ?: return null
        return DevicePublicKey(keyId = keys.keyId(), base64 = base64)
    }

    override fun sign(payload: String): String? = keys.sign(payload)

    override suspend fun integrityToken(nonce: String): String? =
        when (val result = integrity.requestToken(nonce)) {
            is IntegrityTokenResult.Available -> result.token
            IntegrityTokenResult.NotConfigured, is IntegrityTokenResult.Failed -> null
        }

    override fun riskSignals(): DeviceRiskSignals = integrity.riskSignals()
}
