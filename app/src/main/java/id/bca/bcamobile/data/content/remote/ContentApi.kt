package id.bca.bcamobile.data.content.remote

import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.data.content.remote.dto.ContactCsResponse
import id.bca.bcamobile.data.content.remote.dto.HelpCenterResponse
import retrofit2.Response
import retrofit2.http.GET

/**
 * Konten statis: Pusat Bantuan dan Kontak CS.
 *
 * Keduanya publik. Meski hidup di `@AppNetwork`, path-nya ada di
 * `AuthInterceptor.PUBLIC_PATHS` sehingga tidak membawa Authorization.
 */
interface ContentApi {

    @GET("content/help-center")
    suspend fun helpCenter(): Response<ApiEnvelope<HelpCenterResponse>>

    @GET("content/contact-cs")
    suspend fun contactCs(): Response<ApiEnvelope<ContactCsResponse>>
}
