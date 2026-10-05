package id.bca.bcamobile.core.videocall

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import id.bca.bcamobile.domain.onboarding.model.IceServer
import javax.inject.Inject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

/**
 * Sisi media video call: `PeerConnection`, track kamera dan mikrofon, serta renderer.
 *
 * Signaling **tidak** ada di sini — itu [SignalingClient]. Pemisahannya disengaja: lapisan
 * ini butuh perangkat sungguhan dan `PeerConnectionFactory`, jadi mencampurnya membuat
 * seluruh protokol signaling ikut tidak bisa diuji.
 *
 * Medianya mengalir langsung antar-peer lewat **DTLS-SRTP** — terenkripsi secara bawaan
 * oleh WebRTC dan tidak pernah melewati server signaling. Tidak ada perekaman di sisi
 * perangkat (skill §9.4); rekaman untuk POJK urusan backend CS.
 *
 * Satu instance per panggilan. [release] wajib dipanggil, dan pemanggilnya
 * [id.bca.bcamobile.ui.screen.buka_rekening.video_call.BukaRekeningVideoCallViewModel]
 * di `onCleared()` — bukan layar, supaya `PeerConnection` bertahan saat rotasi.
 */
class WebRtcClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : VideoCallMedia {

    /** Dibagikan ke `SurfaceViewRenderer`; keduanya harus memakai konteks EGL yang sama. */
    val eglBase: EglBase = EglBase.create()

    override val eglBaseContext: EglBase.Context get() = eglBase.eglBaseContext

    private val factory: PeerConnectionFactory by lazy {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                // Tracer internal menulis berkas trace besar; di rilis perbankan jangan.
                .setEnableInternalTracer(false)
                .createInitializationOptions(),
        )
        PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true),
            )
            .createPeerConnectionFactory()
    }

    private var peerConnection: PeerConnection? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null
    private var remoteTrack: VideoTrack? = null
    private var isFrontCamera = true
    private val audioRouter = VideoCallAudioRouter(context)

    /**
     * Menyiapkan media dan `PeerConnection`.
     *
     * Dipanggil begitu layar terbuka, **bukan** saat agent ditugaskan: nasabah harus bisa
     * melihat dirinya sendiri sambil menunggu. Penawaran SDP-nya yang ditahan sampai
     * [createOffer] — itulah yang menunggu `agent_assigned`.
     */
    override fun start(
        iceServers: List<IceServer>,
        /**
         * Izin `BLUETOOTH_CONNECT`. `false` hanya melewati cabang headset; panggilan tetap
         * jalan lewat speaker, jadi ini bukan prasyarat.
         */
        allowBluetoothAudio: Boolean,
        onLocalIceCandidate: (candidate: String, sdpMid: String?, sdpMLineIndex: Int) -> Unit,
        onConnectionStateChange: (PeerConnection.PeerConnectionState) -> Unit,
        onRemoteVideoTrack: () -> Unit,
        /**
         * Kamera atau mikrofon tidak bisa diambil walau izinnya ada — mis. kamera dipakai
         * aplikasi lain, atau perangkat tanpa kamera.
         *
         * **Wajib** ada pemanggilnya. Sebelum ini kegagalannya keluar diam-diam lewat
         * `return`, dan karena track audio dibuat sesudah track video, panggilan tersambung
         * tanpa satu pun track: nasabah membaca "Terhubung", petugas tidak melihat dan tidak
         * mendengar apa pun, dan tidak ada satu pesan error di mana pun.
         */
        onMediaUnavailable: () -> Unit,
    ) {
        if (peerConnection != null) return

        peerConnection = factory.createPeerConnection(
            rtcConfig(iceServers),
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    val value = candidate ?: return
                    onLocalIceCandidate(value.sdp, value.sdpMid, value.sdpMLineIndex)
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    newState?.let(onConnectionStateChange)
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    val track = transceiver?.receiver?.track() ?: return
                    if (track.kind() != MediaStreamTrack.VIDEO_TRACK_KIND) return
                    remoteTrack = track as VideoTrack
                    // Renderer bisa belum terpasang kalau track datang lebih dulu;
                    // attachRemoteRenderer menyambungkannya belakangan.
                    remoteRenderer?.let { remoteTrack?.addSink(it) }
                    onRemoteVideoTrack()
                }

                override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) = Unit
                override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) = Unit
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
                override fun onAddStream(stream: org.webrtc.MediaStream?) = Unit
                override fun onRemoveStream(stream: org.webrtc.MediaStream?) = Unit
                override fun onDataChannel(channel: org.webrtc.DataChannel?) = Unit
                override fun onRenegotiationNeeded() = Unit
                override fun onAddTrack(
                    receiver: org.webrtc.RtpReceiver?,
                    streams: Array<out org.webrtc.MediaStream>?,
                ) = Unit
            },
        )

        if (!startLocalMedia()) {
            onMediaUnavailable()
            return
        }

        audioRouter.start(allowBluetooth = allowBluetoothAudio)
    }

    /**
     * `iceServers` kosong adalah jawaban yang sah, bukan kegagalan: artinya TURN belum
     * dikonfigurasi. Panggilan tetap jadi di jaringan ramah dan gagal di seluler
     * ber-NAT ketat — dan itu kegagalan infrastruktur, bukan bug client.
     */
    private fun rtcConfig(iceServers: List<IceServer>): PeerConnection.RTCConfiguration {
        val servers = iceServers.map { server ->
            PeerConnection.IceServer.builder(server.urls)
                .setUsername(server.username.orEmpty())
                .setPassword(server.credential.orEmpty())
                .createIceServer()
        }
        return PeerConnection.RTCConfiguration(servers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            // Jaringan berpindah (WiFi -> seluler) di tengah panggilan: kandidat baru
            // dikumpulkan tanpa menegosiasi ulang dari nol.
            continualGatheringPolicy =
                PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceTransportsType = PeerConnection.IceTransportsType.ALL
        }
    }

    /** `false` berarti kamera atau mikrofon tidak bisa diambil — pemanggil yang melaporkannya. */
    private fun startLocalMedia(): Boolean {
        val capturer = runCatching { createCameraCapturer() }.getOrNull() ?: return false
        videoCapturer = capturer

        val helper = SurfaceTextureHelper.create(CAPTURE_THREAD, eglBase.eglBaseContext)
        surfaceTextureHelper = helper

        val source = factory.createVideoSource(capturer.isScreencast)
        videoSource = source
        capturer.initialize(helper, context, source.capturerObserver)
        // Izin yang dicabut tepat di sela pemeriksaan dan pemanggilan ini membuat
        // startCapture melempar; itu kegagalan media, bukan crash.
        if (runCatching { capturer.startCapture(CAPTURE_WIDTH, CAPTURE_HEIGHT, CAPTURE_FPS) }
                .isFailure
        ) {
            return false
        }

        localVideoTrack = factory.createVideoTrack(TRACK_LOCAL_VIDEO, source).also { track ->
            localRenderer?.let(track::addSink)
            peerConnection?.addTrack(track, listOf(STREAM_ID))
        }

        val audio = factory.createAudioSource(MediaConstraints())
        audioSource = audio
        localAudioTrack = factory.createAudioTrack(TRACK_LOCAL_AUDIO, audio).also { track ->
            peerConnection?.addTrack(track, listOf(STREAM_ID))
        }

        // Keduanya harus ada. Video tanpa audio membuat petugas tidak bisa mengajukan
        // pertanyaan verifikasi, dan audio tanpa video membuat e-KTP tidak bisa diperiksa —
        // masing-masing menggagalkan e-KYC dengan caranya sendiri.
        return localVideoTrack != null && localAudioTrack != null
    }

    /** Kamera depan lebih dulu: verifikasi identitas butuh wajah, bukan pemandangan. */
    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val names = enumerator.deviceNames
        val front = names.firstOrNull(enumerator::isFrontFacing)
        val chosen = front ?: names.firstOrNull() ?: return null
        isFrontCamera = chosen == front
        return enumerator.createCapturer(chosen, null)
    }

    // -- Renderer ---------------------------------------------------------------

    override fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
        localRenderer = renderer
        localVideoTrack?.addSink(renderer)
    }

    override fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
        remoteRenderer = renderer
        remoteTrack?.addSink(renderer)
    }

    fun detachRenderers() {
        localRenderer?.let { localVideoTrack?.removeSink(it) }
        remoteRenderer?.let { remoteTrack?.removeSink(it) }
        localRenderer = null
        remoteRenderer = null
    }

    // -- Negosiasi --------------------------------------------------------------

    /** Dipanggil saat `agent_assigned` diterima — bukan lebih awal. */
    override fun createOffer(onSdpReady: (String) -> Unit) {
        val connection = peerConnection ?: return
        connection.createOffer(
            object : SdpObserver {
                override fun onCreateSuccess(description: SessionDescription?) {
                    val sdp = description ?: return
                    connection.setLocalDescription(NoopSdpObserver, sdp)
                    onSdpReady(sdp.description)
                }

                override fun onSetSuccess() = Unit
                override fun onCreateFailure(error: String?) = Unit
                override fun onSetFailure(error: String?) = Unit
            },
            MediaConstraints(),
        )
    }

    override fun onAnswer(sdp: String) {
        peerConnection?.setRemoteDescription(
            NoopSdpObserver,
            SessionDescription(SessionDescription.Type.ANSWER, sdp),
        )
    }

    override fun onRemoteIceCandidate(candidate: String, sdpMid: String?, sdpMLineIndex: Int) {
        peerConnection?.addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
    }

    /** ICE restart saat jaringan berpindah; tidak membongkar `PeerConnection`. */
    override fun restartIce() {
        peerConnection?.restartIce()
    }

    // -- Kontrol ----------------------------------------------------------------

    override fun setMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    /** `true` kalau sesudahnya kamera depan yang aktif. */
    override fun switchCamera(): Boolean {
        val capturer = videoCapturer as? Camera2Capturer ?: return isFrontCamera
        isFrontCamera = !isFrontCamera
        capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFrontFacing: Boolean) {
                isFrontCamera = isFrontFacing
            }

            override fun onCameraSwitchError(error: String?) {
                // Gagal berpindah berarti kamera sebelumnya masih yang aktif.
                isFrontCamera = !isFrontCamera
            }
        })
        return isFrontCamera
    }

    /** Aplikasi ke background: kamera dilepas, sambungan dibiarkan hidup (skill §10). */
    override fun pauseLocalVideo() {
        runCatching { videoCapturer?.stopCapture() }
    }

    override fun resumeLocalVideo() {
        runCatching {
            videoCapturer?.startCapture(CAPTURE_WIDTH, CAPTURE_HEIGHT, CAPTURE_FPS)
        }
    }

    /**
     * Urutannya mengikat: capturer berhenti dulu, lalu track, lalu source, baru
     * `PeerConnection`. Dibalik, `stopCapture` berjalan di atas source yang sudah mati dan
     * proses bisa crash setelah panggilan selesai — crash yang tampaknya tanpa sebab.
     */
    override fun release() {
        // Mode dan speaker adalah setelan global; dibiarkan menyala, pemutar musik
        // sesudahnya ikut keluar dari speaker.
        audioRouter.restore()
        detachRenderers()
        runCatching { videoCapturer?.stopCapture() }
        videoCapturer?.dispose()
        videoCapturer = null
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
        localVideoTrack?.dispose()
        localVideoTrack = null
        localAudioTrack?.dispose()
        localAudioTrack = null
        videoSource?.dispose()
        videoSource = null
        audioSource?.dispose()
        audioSource = null
        remoteTrack = null
        peerConnection?.close()
        peerConnection = null
        eglBase.release()
    }

    private object NoopSdpObserver : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription?) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) = Unit
        override fun onSetFailure(error: String?) = Unit
    }

    private companion object {
        const val CAPTURE_THREAD = "BcaVideoCallCapture"
        const val CAPTURE_WIDTH = 720
        const val CAPTURE_HEIGHT = 1280
        const val CAPTURE_FPS = 30
        const val TRACK_LOCAL_VIDEO = "bca_local_video"
        const val TRACK_LOCAL_AUDIO = "bca_local_audio"
        const val STREAM_ID = "bca_ekyc_stream"
    }
}
