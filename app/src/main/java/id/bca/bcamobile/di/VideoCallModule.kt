package id.bca.bcamobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.bca.bcamobile.core.videocall.OkHttpSignalingClient
import id.bca.bcamobile.core.videocall.SignalingClient
import id.bca.bcamobile.core.videocall.VideoCallMedia
import id.bca.bcamobile.core.videocall.WebRtcClient

/**
 * Sisi video call e-KYC.
 *
 * Keduanya **tanpa** `@Singleton`: socket signaling sekali pakai (`signaling_url` membawa
 * JWT yang hangus), dan `PeerConnection` tidak boleh hidup lebih lama dari panggilannya.
 * Instance yang dibagi antar-panggilan akan mencoba menyambung dengan token mati, dan
 * WebRtcClient yang dipakai ulang membawa track yang sudah di-dispose.
 */
@Module
@InstallIn(SingletonComponent::class)
interface VideoCallModule {

    @Binds
    fun bindSignalingClient(impl: OkHttpSignalingClient): SignalingClient

    @Binds
    fun bindVideoCallMedia(impl: WebRtcClient): VideoCallMedia
}
