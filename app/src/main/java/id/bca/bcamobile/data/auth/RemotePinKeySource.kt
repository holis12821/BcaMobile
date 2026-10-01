package id.bca.bcamobile.data.auth

import id.bca.bcamobile.core.network.ApiCaller
import id.bca.bcamobile.core.security.PinKey
import id.bca.bcamobile.core.security.PinKeySource
import id.bca.bcamobile.data.auth.remote.AuthApi
import id.bca.bcamobile.domain.common.DataResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mengambil kunci publik PIN dari `GET auth/pin/public-key`.
 *
 * Kegagalan dikembalikan sebagai null, bukan dilempar: [PinKeySource] adalah satu
 * langkah di rantai cadangan [id.bca.bcamobile.core.security.PinKeyProvider], dan
 * langkah berikutnya masih punya kesempatan menjawab.
 *
 * `allowRetry = false` — rantai cadangannya sudah menjadi penanganan gagal, dan
 * layar masuk tidak boleh menahan nasabah selama tiga kali percobaan ulang.
 */
@Singleton
class RemotePinKeySource @Inject constructor(
    private val api: AuthApi,
    private val caller: ApiCaller,
) : PinKeySource {

    override suspend fun fetch(): PinKey? {
        val result = caller.call(allowRetry = false) { api.pinPublicKey() }
        val response = (result as? DataResult.Success)?.value ?: return null
        return PinKey(
            pem = response.publicKeyPem.takeIf { it.isNotBlank() } ?: return null,
            keyId = response.keyId.takeIf { it.isNotBlank() },
        )
    }
}
