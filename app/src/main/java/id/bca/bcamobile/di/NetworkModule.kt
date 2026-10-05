package id.bca.bcamobile.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.bca.bcamobile.BuildConfig
import id.bca.bcamobile.core.network.AppNetwork
import id.bca.bcamobile.core.network.AuthInterceptor
import id.bca.bcamobile.core.network.HeaderInterceptor
import id.bca.bcamobile.core.network.OnboardingNetwork
import id.bca.bcamobile.data.card.remote.CardApi
import id.bca.bcamobile.data.content.remote.ContentApi
import id.bca.bcamobile.core.network.TokenAuthenticator
import id.bca.bcamobile.data.account.remote.AccountApi
import id.bca.bcamobile.data.auth.remote.AuthApi
import id.bca.bcamobile.data.config.remote.HealthApi
import id.bca.bcamobile.data.ewallet.remote.EWalletApi
import id.bca.bcamobile.data.notification.remote.NotificationApi
import id.bca.bcamobile.data.qris.remote.QrisApi
import id.bca.bcamobile.data.onboarding.remote.OnboardingApi
import id.bca.bcamobile.data.transaction.remote.TransactionApi
import id.bca.bcamobile.data.transfer.remote.TransferApi
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Dua jaringan yang sengaja dipisah:
 *
 * - [OnboardingNetwork] — base URL `…/v1/onboarding/`, **tanpa** Authorization.
 *   Flow buka rekening berjalan sebelum nasabah punya akun.
 * - [AppNetwork] — base URL `…/v1/`, dengan Authorization dan auto-refresh token.
 *
 * Menyatukan keduanya membuat `AuthApi` menembak `…/v1/onboarding/auth/login/pin`.
 * Keduanya tetap berbagi [HeaderInterceptor] supaya `X-Device-ID` dan `X-Request-ID`
 * konsisten — lihat `bca-mobile-api/docs/06-BUKA-REKENING-API-SPEC.md` §0.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true      // server boleh menambah field tanpa memecah client lama
        explicitNulls = false
        encodeDefaults = true
    }

    @Provides
    @Singleton
    @OnboardingNetwork
    fun provideOnboardingClient(
        headerInterceptor: HeaderInterceptor,
    ): OkHttpClient = baseClientBuilder(headerInterceptor).build()

    @Provides
    @Singleton
    @AppNetwork
    fun provideAppClient(
        headerInterceptor: HeaderInterceptor,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = baseClientBuilder(headerInterceptor)
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .build()

    @Provides
    @Singleton
    @OnboardingNetwork
    fun provideOnboardingRetrofit(
        @OnboardingNetwork client: OkHttpClient,
        json: Json,
    ): Retrofit = retrofit(BuildConfig.ONBOARDING_BASE_URL, client, json)

    @Provides
    @Singleton
    @AppNetwork
    fun provideAppRetrofit(
        @AppNetwork client: OkHttpClient,
        json: Json,
    ): Retrofit = retrofit(BuildConfig.APP_BASE_URL, client, json)

    @Provides
    @Singleton
    fun provideOnboardingApi(@OnboardingNetwork retrofit: Retrofit): OnboardingApi =
        retrofit.create(OnboardingApi::class.java)

    @Provides
    @Singleton
    fun provideAuthApi(@AppNetwork retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideAccountApi(@AppNetwork retrofit: Retrofit): AccountApi =
        retrofit.create(AccountApi::class.java)

    @Provides
    @Singleton
    fun provideTransactionApi(@AppNetwork retrofit: Retrofit): TransactionApi =
        retrofit.create(TransactionApi::class.java)

    @Provides
    @Singleton
    fun provideCardApi(@AppNetwork retrofit: Retrofit): CardApi =
        retrofit.create(CardApi::class.java)

    /**
     * Pusat Bantuan dan Kontak CS **publik** — path-nya terdaftar di
     * [AuthInterceptor] sebagai jalur tanpa Authorization, jadi klien
     * bernasabah ini tidak menempelkan token ke keduanya. Nasabah yang terkunci
     * di luar aplikasi tetap bisa menghubungi Halo BCA.
     */
    @Provides
    @Singleton
    fun provideContentApi(@AppNetwork retrofit: Retrofit): ContentApi =
        retrofit.create(ContentApi::class.java)

    @Provides
    @Singleton
    fun provideTransferApi(@AppNetwork retrofit: Retrofit): TransferApi =
        retrofit.create(TransferApi::class.java)

    @Provides
    @Singleton
    fun provideEWalletApi(@AppNetwork retrofit: Retrofit): EWalletApi =
        retrofit.create(EWalletApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationApi(@AppNetwork retrofit: Retrofit): NotificationApi =
        retrofit.create(NotificationApi::class.java)

    @Provides
    @Singleton
    fun provideQrisApi(@AppNetwork retrofit: Retrofit): QrisApi =
        retrofit.create(QrisApi::class.java)

    /**
     * `GET /health` publik, tapi tetap di klien bernasabah: alamatnya `APP_BASE_URL`,
     * dan [AuthInterceptor] tidak menyisipkan apa pun selama token belum ada.
     */
    @Provides
    @Singleton
    fun provideHealthApi(@AppNetwork retrofit: Retrofit): HealthApi =
        retrofit.create(HealthApi::class.java)

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

    private fun baseClientBuilder(headerInterceptor: HeaderInterceptor): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            // Body log hanya di debug — response memuat PII dan token.
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY },
            )
        }

        if (BuildConfig.CERTIFICATE_PINNING_ENABLED) {
            builder.certificatePinner(certificatePinner())
        }

        return builder
    }

    private fun retrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(APPLICATION_JSON.toMediaType()))
            .build()

    /**
     * Pin sertifikat produksi.
     *
     * TODO(infra): isi hash SPKI asli dari tim infrastruktur BCA sebelum rilis.
     * Selama daftar pin kosong, pinning tidak diaktifkan agar build release tidak
     * gagal total saat handshake — ini sengaja, bukan kelalaian.
     */
    private fun certificatePinner(): CertificatePinner {
        val pins = emptyList<Pair<String, String>>()
        return CertificatePinner.Builder()
            .apply { pins.forEach { (host, pin) -> add(host, pin) } }
            .build()
    }

    private const val APPLICATION_JSON = "application/json"
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 60L
    private const val WRITE_TIMEOUT_SECONDS = 60L
}
