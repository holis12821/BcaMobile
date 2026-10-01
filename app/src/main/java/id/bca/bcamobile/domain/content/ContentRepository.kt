package id.bca.bcamobile.domain.content

import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.content.model.ContactCs
import id.bca.bcamobile.domain.content.model.HelpCategory

/**
 * Pusat Bantuan dan Kontak CS.
 *
 * **Keduanya tanpa `Authorization`, dan itu disengaja** — nasabah yang terkunci
 * di luar aplikasi justru yang paling butuh nomor Halo BCA. Karena itu path-nya
 * terdaftar di `AuthInterceptor.PUBLIC_PATHS` dan layarnya tidak boleh
 * diletakkan di belakang gerbang login.
 *
 * Kedua respons di-cache server 24 jam; client tidak perlu cache tambahan.
 */
interface ContentRepository {

    suspend fun helpCenter(): DataResult<List<HelpCategory>>

    suspend fun contactCs(): DataResult<ContactCs>
}
