package id.bca.bcamobile.core.videocall

import id.bca.bcamobile.domain.onboarding.model.IceServer
import org.webrtc.EglBase
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer

/**
 * Sisi media video call, dilihat dari ViewModel.
 *
 * Ada supaya [id.bca.bcamobile.ui.screen.buka_rekening.video_call.BukaRekeningVideoCallViewModel]
 * tidak bergantung pada [WebRtcClient] konkret. Klien itu membuat `EglBase` dan memuat
 * pustaka native di konstruktornya, jadi ViewModel yang menyebutnya langsung **tidak bisa
 * diuji di JVM sama sekali** — termasuk logika penyambungan ulang yang tidak menyentuh media
 * sedikit pun.
 *
 * Tipe `org.webrtc` di tanda tangan tidak masalah: implementasi palsu hanya perlu
 * menyebutnya, bukan membuat objeknya.
 */
interface VideoCallMedia {

    /**
     * Konteks EGL untuk `SurfaceViewRenderer`, atau `null` kalau media tidak tersedia —
     * yang terjadi pada implementasi palsu di test dan membuat layar jatuh ke latar polos.
     */
    val eglBaseContext: EglBase.Context?

    fun start(
        iceServers: List<IceServer>,
        allowBluetoothAudio: Boolean,
        onLocalIceCandidate: (candidate: String, sdpMid: String?, sdpMLineIndex: Int) -> Unit,
        onConnectionStateChange: (PeerConnection.PeerConnectionState) -> Unit,
        onRemoteVideoTrack: () -> Unit,
        onMediaUnavailable: () -> Unit,
    )

    fun attachLocalRenderer(renderer: SurfaceViewRenderer)

    fun attachRemoteRenderer(renderer: SurfaceViewRenderer)

    fun createOffer(onSdpReady: (String) -> Unit)

    fun onAnswer(sdp: String)

    fun onRemoteIceCandidate(candidate: String, sdpMid: String?, sdpMLineIndex: Int)

    fun restartIce()

    fun setMuted(muted: Boolean)

    /** `true` kalau sesudahnya kamera depan yang aktif. */
    fun switchCamera(): Boolean

    fun pauseLocalVideo()

    fun resumeLocalVideo()

    fun release()
}
