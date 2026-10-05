package id.bca.bcamobile.ui.screen.buka_rekening

import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.core.videocall.MediaControlAction
import id.bca.bcamobile.core.videocall.SignalingClient
import id.bca.bcamobile.core.videocall.SignalingEvent
import id.bca.bcamobile.core.videocall.VideoCallMedia
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.model.IceServer
import id.bca.bcamobile.domain.onboarding.model.OnboardingSession
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.domain.onboarding.model.QueueTicket
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.video_call.BukaRekeningVideoCallViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.video_call.VideoCallEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.webrtc.EglBase
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer

/**
 * Penyambungan ulang signaling video call.
 *
 * Satu aturan yang diuji di sini, dan pelanggarannya tidak pernah terlihat sebagai bug
 * sampai di perangkat: token di `signaling_url` **sekali pakai**. Backend mencatat `jti`-nya
 * di Redis saat socket pertama dibuka, jadi menyambung ulang ke URL yang sama dijawab
 * `401 token already used` — selalu. Pemulihan yang benar mengambil tiket baru lewat
 * `POST video-call/queue`, yang idempoten dan mengembalikan `queue_id` yang sama dengan
 * `signaling_url` baru.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BukaRekeningVideoCallReconnectTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Mencatat URL yang benar-benar dipakai menyambung. */
    private class RecordingSignalingClient : SignalingClient {
        private val _events = MutableSharedFlow<SignalingEvent>(extraBufferCapacity = 16)
        override val events: SharedFlow<SignalingEvent> = _events.asSharedFlow()

        val connectedUrls = mutableListOf<String>()
        var disconnectCount = 0

        suspend fun emit(event: SignalingEvent) = _events.emit(event)

        override fun connect(url: String, sessionId: String, queueId: String) {
            connectedUrls += url
        }

        override fun sendOffer(sdp: String) = Unit
        override fun sendIceCandidate(candidate: String, sdpMid: String?, sdpMLineIndex: Int) = Unit
        override fun sendMediaControl(action: MediaControlAction) = Unit
        override fun disconnect() {
            disconnectCount += 1
        }
    }

    @Test
    fun `putus sambungan mengambil tiket baru, bukan memakai url yang sudah hangus`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            val store = storeWithTicket(URL_PERTAMA)
            repo.joinQueueResult = DataResult.Success(ticket(URL_KEDUA))
            val signaling = RecordingSignalingClient()

            videoCall(repo, store, signaling)
            runCurrent()

            signaling.emit(SignalingEvent.Disconnected)
            runCurrent()
            advanceTimeBy(BACKOFF_PERTAMA_MS + 1)
            runCurrent()

            assertEquals(1, repo.joinQueueCount)
            // URL kedua wajib berbeda; memakai yang pertama dijamin 401.
            assertEquals(2, signaling.connectedUrls.size)
            assertNotEquals(signaling.connectedUrls[0], signaling.connectedUrls[1])
            assertEquals(URL_KEDUA, signaling.connectedUrls[1])
            // Tiket baru disimpan supaya percobaan berikutnya tidak mundur ke yang lama.
            assertEquals(URL_KEDUA, store.current.queue?.signalingUrl)

        }

    @Test
    fun `antrean yang menolak karena step sudah lewat berarti panggilan selesai`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            val store = storeWithTicket(URL_PERTAMA)
            // Server memindahkan step saat kita terputus: CS sudah menyubmit hasilnya.
            repo.joinQueueResult = DataResult.Failure(ApiFailure.InvalidStep)
            repo.getSessionResult = DataResult.Success(
                OnboardingSession(
                    sessionId = SESSION_ID,
                    product = null,
                    currentStep = OnboardingStep.CREDENTIALS,
                ),
            )
            val signaling = RecordingSignalingClient()

            videoCall(repo, store, signaling)
            runCurrent()

            signaling.emit(SignalingEvent.Disconnected)
            runCurrent()
            advanceTimeBy(BACKOFF_PERTAMA_MS + 1)
            runCurrent()

            // Verifikasi yang sebenarnya berhasil tidak boleh berakhir sebagai kegagalan.
            assertTrue("step harus ditanyakan ke server", repo.getSessionCount >= 1)
            assertTrue("langkah selesai harus ditandai", store.current.isVideoCallVerified)
        }

    @Test
    fun `percobaan berhenti setelah batas, tidak mencoba selamanya`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val store = storeWithTicket(URL_PERTAMA)
        repo.joinQueueResult = DataResult.Failure(ApiFailure.Network)
        val signaling = RecordingSignalingClient()

        videoCall(repo, store, signaling)
        runCurrent()

        // Gagal berulang: tiap kegagalan jaringan memicu percobaan berikutnya.
        repeat(MAX_RECONNECT + 2) {
            signaling.emit(SignalingEvent.Disconnected)
            runCurrent()
            advanceTimeBy(TOTAL_BACKOFF_MS)
            runCurrent()
        }

        assertTrue(
            "percobaan = ${repo.joinQueueCount}, tidak boleh melebihi batas",
            repo.joinQueueCount <= MAX_RECONNECT,
        )
    }

    /**
     * Media palsu. `eglBaseContext` null dan tidak ada satu pun tipe native yang dibuat —
     * itulah gunanya [VideoCallMedia] dipisah dari `WebRtcClient`.
     */
    private class FakeMedia : VideoCallMedia {
        override val eglBaseContext: EglBase.Context? = null
        var released = false

        override fun start(
            iceServers: List<IceServer>,
            allowBluetoothAudio: Boolean,
            onLocalIceCandidate: (String, String?, Int) -> Unit,
            onConnectionStateChange: (PeerConnection.PeerConnectionState) -> Unit,
            onRemoteVideoTrack: () -> Unit,
            onMediaUnavailable: () -> Unit,
        ) = Unit

        override fun attachLocalRenderer(renderer: SurfaceViewRenderer) = Unit
        override fun attachRemoteRenderer(renderer: SurfaceViewRenderer) = Unit
        override fun createOffer(onSdpReady: (String) -> Unit) = Unit
        override fun onAnswer(sdp: String) = Unit
        override fun onRemoteIceCandidate(candidate: String, sdpMid: String?, sdpMLineIndex: Int) = Unit
        override fun restartIce() = Unit
        override fun setMuted(muted: Boolean) = Unit
        override fun switchCamera(): Boolean = true
        override fun pauseLocalVideo() = Unit
        override fun resumeLocalVideo() = Unit
        override fun release() {
            released = true
        }
    }

    // -- Helper ----------------------------------------------------------------

    /** Izin diberikan lewat event, sama seperti yang dilakukan graph. */
    private fun videoCall(
        repo: FakeOnboardingRepository,
        store: BukaRekeningSessionStore,
        signaling: SignalingClient,
    ): BukaRekeningVideoCallViewModel {
        val viewModel = BukaRekeningVideoCallViewModel(repo, store, signaling, FakeMedia())
        viewModel.onEvent(
            VideoCallEvent.MediaPermissionsChanged(granted = true, allowBluetooth = false),
        )
        return viewModel
    }

    private fun storeWithTicket(url: String) = BukaRekeningSessionStore().apply {
        update { it.copy(sessionId = SESSION_ID, queue = ticket(url)) }
    }

    private fun ticket(url: String) = QueueTicket(
        queueId = QUEUE_ID,
        queueNumber = "A-042",
        position = 1,
        estimatedWaitSeconds = 180,
        operatingHours = null,
        signalingUrl = url,
        signalingExpiresInSeconds = 300,
        iceServers = emptyList(),
    )

    private companion object {
        const val SESSION_ID = "onb_vc_reconnect"
        const val QUEUE_ID = "q_reconnect"
        const val URL_PERTAMA = "wss://signal.test/v1/onboarding/video-call/signal?token=pertama"
        const val URL_KEDUA = "wss://signal.test/v1/onboarding/video-call/signal?token=kedua"
        const val BACKOFF_PERTAMA_MS = 1_000L
        const val TOTAL_BACKOFF_MS = 20_000L
        const val MAX_RECONNECT = 5
    }
}
