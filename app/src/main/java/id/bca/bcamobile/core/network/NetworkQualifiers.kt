package id.bca.bcamobile.core.network

import javax.inject.Qualifier

/**
 * Onboarding berjalan tanpa access token dan base URL-nya berakhir di
 * `/v1/onboarding/`. Aplikasi bernasabah memakai `/v1/` dengan Authorization.
 * Keduanya tidak boleh tertukar — karena itu setiap Retrofit/OkHttp diberi tanda.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OnboardingNetwork

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppNetwork
