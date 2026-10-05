package id.bca.bcamobile.domain.config

import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.config.model.AppConfig

/**
 * Health check + konfigurasi aplikasi.
 * Kontrak: `bca-mobile-api/docs/01-API-SPECIFICATION.md` §1.
 *
 * Endpoint ini publik — tidak butuh access token, jadi bisa dipanggil sebelum login.
 */
interface AppConfigRepository {

    /** Config terakhir yang berhasil diambil sesi ini, atau null bila belum ada. */
    fun cached(): AppConfig?

    suspend fun refresh(): DataResult<AppConfig>
}
