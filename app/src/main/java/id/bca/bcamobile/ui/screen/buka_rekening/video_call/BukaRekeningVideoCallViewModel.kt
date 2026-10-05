package id.bca.bcamobile.ui.screen.buka_rekening.video_call

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.R
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.core.videocall.MediaControlAction
import id.bca.bcamobile.core.videocall.SignalingClient
import id.bca.bcamobile.core.videocall.SignalingEvent
import id.bca.bcamobile.core.videocall.VideoCallMedia
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.domain.onboarding.model.QueueTicket
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer

/**
 * Mengorkestrasi satu panggilan video e-KYC: signaling, media, dan keadaan layar.
 *
 * `PeerConnection` dan socket signaling hidup **di sini**, bukan di layar, supaya keduanya
 * bertahan melewati rotasi (skill §10). Keduanya dilepas di [onCleared].
 */
@HiltViewModel
class BukaRekeningVideoCallViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
    private val signaling: SignalingClient,
    private val webRtc: VideoCallMedia,
) : BukaRekeningStepViewModel(repository, store) {

    private val _uiState = MutableStateFlow(VideoCallUiState())
    val uiState: StateFlow<VideoCallUiState> = _uiState.asStateFlow()

    /** Dibagikan ke `SurfaceViewRenderer` di layar; keduanya wajib satu konteks EGL. */
    val eglBaseContext: EglBase.Context? get() = webRtc.eglBaseContext

    private var timerJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0

    /**
     * Tiket antrean dibaca sekali dan disimpan: `signaling_url` di dalamnya yang menjadi
     * alamat sambungan, dan nilainya tidak boleh berubah di tengah panggilan.
     */
    /**
     * Tiket antrean yang sedang dipakai.
     *
     * **Bukan `val`.** `signaling_url` di dalamnya membawa token sekali pakai, jadi setiap
     * penyambungan ulang menukarnya dengan tiket baru dari `POST video-call/queue`.
     */
    private var ticket: QueueTicket? = store.current.queue

    /**
     * Panggilan **tidak** dimulai di `init`.
     *
     * Pemicunya [VideoCallEvent.MediaPermissionsChanged] dari graph, karena ViewModel tidak
     * boleh menyentuh `Context` untuk memeriksa izin. Dengan begitu satu jalur yang sama
     * melayani masuk pertama, pulih setelah proses mati, dan pencabutan di tengah panggilan.
     */
    private var isStarted = false

    fun onEvent(event: VideoCallEvent) {
        when (event) {
            is VideoCallEvent.MediaPermissionsChanged -> onPermissionsChanged(event)
            VideoCallEvent.MuteToggled -> toggleMute()
            VideoCallEvent.CameraSwitched -> switchCamera()
            VideoCallEvent.EndCallRequested -> endCall()
            VideoCallEvent.RejoinRequested -> prepareRejoin()
        }
    }

    // -- Gerbang izin -----------------------------------------------------------

    private fun onPermissionsChanged(event: VideoCallEvent.MediaPermissionsChanged) {
        if (!event.granted) {
            onPermissionsLost()
            return
        }
        if (isStarted) return
        isStarted = true
        startCall(allowBluetoothAudio = event.allowBluetooth)
    }

    /**
     * Izin hilang. Dua kemungkinan, dan keduanya berakhir di keadaan yang sama.
     *
     * Kalau panggilan belum jalan: layar meminta izin. Kalau sudah jalan, izin dicabut dari
     * Pengaturan di tengah verifikasi — panggilan **diakhiri** dan alasannya disampaikan,
     * sesuai skill §8. Membiarkannya hidup berarti petugas menatap layar beku tanpa tahu
     * kenapa, dan verifikasi identitas yang begitu tidak bisa disimpulkan apa-apa.
     */
    private fun onPermissionsLost() {
        if (isStarted) {
            stopTimer()
            reconnectJob?.cancel()
            signaling.disconnect()
            webRtc.release()
            isStarted = false
            emitMessage(ErrorText.Res(R.string.buka_rekening_vc_izin_dicabut))
        }
        _uiState.update {
            it.copy(connectionState = VideoCallConnectionState.IZIN_DIBUTUHKAN)
        }
    }

    // -- Awal panggilan ---------------------------------------------------------

    private fun startCall(allowBluetoothAudio: Boolean) {
        val queue = ticket
        // Tanpa tiket tidak ada `signaling_url`, jadi tidak ada yang bisa disambungi.
        // Terjadi kalau layar ini dibuka langsung tanpa melewati antrean.
        if (queue == null || queue.signalingUrl.isBlank()) {
            _uiState.update {
                it.copy(
                    connectionState = VideoCallConnectionState.GAGAL,
                    error = null,
                )
            }
            emitMessage(ErrorText.Res(R.string.buka_rekening_vc_error_tanpa_antrean))
            return
        }

        webRtc.start(
            iceServers = queue.iceServers,
            allowBluetoothAudio = allowBluetoothAudio,
            onLocalIceCandidate = signaling::sendIceCandidate,
            onConnectionStateChange = ::onPeerConnectionState,
            onRemoteVideoTrack = { },
            onMediaUnavailable = ::onMediaUnavailable,
        )
        if (_uiState.value.connectionState == VideoCallConnectionState.GAGAL) return

        _uiState.update { it.copy(connectionState = VideoCallConnectionState.MENUNGGU_PETUGAS) }
        observeSignaling()
        signaling.connect(
            url = queue.signalingUrl,
            sessionId = store.current.sessionId.orEmpty(),
            queueId = queue.queueId,
        )
    }

    /**
     * Izinnya ada tapi kamera atau mikrofon tetap tidak bisa diambil — mis. kamera sedang
     * dipakai aplikasi lain. Dulu kegagalan ini tidak terlihat sama sekali.
     */
    private fun onMediaUnavailable() {
        isStarted = false
        _uiState.update { it.copy(connectionState = VideoCallConnectionState.GAGAL) }
        emitMessage(ErrorText.Res(R.string.buka_rekening_vc_media_gagal))
    }

    private fun observeSignaling() {
        viewModelScope.launch {
            signaling.events.collect { event ->
                when (event) {
                    is SignalingEvent.QueueUpdate -> Unit

                    is SignalingEvent.AgentAssigned -> onAgentAssigned(event)

                    is SignalingEvent.Answer -> webRtc.onAnswer(event.sdp)

                    is SignalingEvent.IceCandidate -> webRtc.onRemoteIceCandidate(
                        candidate = event.candidate,
                        sdpMid = event.sdpMid,
                        sdpMLineIndex = event.sdpMLineIndex,
                    )

                    is SignalingEvent.Instruction ->
                        _uiState.update { it.copy(instruksiPetugas = event.text) }

                    is SignalingEvent.CallEnded -> onCallEndedByAgent(event)

                    is SignalingEvent.Failed -> scheduleReconnect()

                    SignalingEvent.Disconnected -> scheduleReconnect()
                }
            }
        }
    }

    private fun onAgentAssigned(event: SignalingEvent.AgentAssigned) {
        reconnectAttempt = 0
        _uiState.update {
            it.copy(
                connectionState = VideoCallConnectionState.MENGHUBUNGKAN,
                namaPetugas = event.name,
                idPetugas = event.employeeId,
            )
        }
        // Penawaran SDP dibuat **sekarang**, bukan saat layar dibuka: sebelum ada petugas
        // tidak ada yang menjawabnya, dan penawaran yang menggantung membuat negosiasi
        // berikutnya ditolak.
        webRtc.createOffer(signaling::sendOffer)
    }

    private fun onPeerConnectionState(state: PeerConnection.PeerConnectionState) {
        when (state) {
            PeerConnection.PeerConnectionState.CONNECTED -> {
                reconnectAttempt = 0
                _uiState.update { it.copy(connectionState = VideoCallConnectionState.TERHUBUNG) }
                startTimer()
            }

            PeerConnection.PeerConnectionState.DISCONNECTED -> {
                _uiState.update {
                    it.copy(connectionState = VideoCallConnectionState.MENYAMBUNG_ULANG)
                }
                // Jaringan berpindah (WiFi -> seluler) biasanya pulih dengan ICE restart
                // saja, tanpa membongkar PeerConnection atau antrean.
                webRtc.restartIce()
            }

            PeerConnection.PeerConnectionState.FAILED -> failCall()

            else -> Unit
        }
    }

    // -- Akhir panggilan --------------------------------------------------------

    private fun onCallEndedByAgent(event: SignalingEvent.CallEnded) {
        stopTimer()
        reconnectJob?.cancel()
        signaling.disconnect()
        _uiState.update {
            it.copy(
                connectionState = VideoCallConnectionState.SELESAI,
                namaPetugas = event.agentName.ifBlank { it.namaPetugas },
            )
        }
        completeStep(event.agentName.ifBlank { _uiState.value.namaPetugas })
    }

    private fun endCall() {
        stopTimer()
        reconnectJob?.cancel()
        signaling.disconnect()
        _uiState.update { it.copy(connectionState = VideoCallConnectionState.SELESAI) }
        completeStep(_uiState.value.namaPetugas)
    }

    /**
     * Menandai langkah selesai lalu **menanyakan** posisi berikutnya ke server.
     *
     * `result` dari `call_ended` sengaja tidak dipakai untuk menavigasi: hasil verifikasi
     * ditulis CS lewat `§5c`, dan `current_step` dari server tetap satu-satunya sumber
     * kebenaran arah — client tidak menyimpulkan lulus atau tidak dari pesan signaling.
     */
    private fun completeStep(csName: String) {
        store.update { it.copy(isVideoCallVerified = true, videoCallCsName = csName) }
        if (store.current.sessionId == null) return
        launchWithLoading { refreshStep() }
    }

    // -- Pemulihan --------------------------------------------------------------

    /**
     * Sambungan putus: dicoba ulang dengan jeda menanjak, lalu menyerah.
     *
     * Selisih kontrak yang perlu diketahui: skill §3 meminta auto-reconnect 5× dengan
     * exponential backoff, tapi `QueueTicket.signalingUrl` mendokumentasikan tokennya
     * **sekali pakai** — dan token sekali pakai tidak bisa disambung ulang. Keduanya
     * dijalankan di sini: percobaan ulang tetap ada karena putusnya TCP belum tentu
     * memakai token, tapi jumlahnya dibatasi dan kegagalannya berakhir di [failCall] yang
     * menawarkan antrean ulang — bukan mencoba selamanya ke token yang sudah mati.
     */
    private fun scheduleReconnect() {
        if (_uiState.value.connectionState == VideoCallConnectionState.SELESAI) return
        if (reconnectJob?.isActive == true) return
        if (reconnectAttempt >= MAX_RECONNECT) return failCall()

        val delayMs = BACKOFF_MS[reconnectAttempt.coerceAtMost(BACKOFF_MS.lastIndex)]
        reconnectAttempt += 1
        _uiState.update { it.copy(connectionState = VideoCallConnectionState.MENYAMBUNG_ULANG) }

        reconnectJob = viewModelScope.launch {
            delay(delayMs)
            reconnectWithFreshTicket()
        }
    }

    /**
     * Menyambung ulang dengan tiket **baru**, bukan dengan `signaling_url` yang sudah ada.
     *
     * Token di dalam URL itu sekali pakai: backend mencatat `jti`-nya di Redis saat socket
     * pertama dibuka (`SignalingTokenGuard.ConsumeSignalingToken`), jadi upaya kedua ke URL
     * yang sama dijawab `401 token already used` — selalu, bukan kadang-kadang. Versi
     * sebelumnya melakukan tepat itu, jadi kelima percobaannya dijamin gagal dan satu
     * sambungan yang terputus sesaat berakhir di keadaan GAGAL.
     *
     * `POST video-call/queue` idempoten untuk sesi yang masih mengantre atau sedang
     * dilayani: tiketnya **sama** (`queue_id` dan `queue_number` tidak berubah, jadi nomor
     * antrean nasabah tidak melompat), yang baru hanya `signaling_url`-nya.
     */
    private suspend fun reconnectWithFreshTicket() {
        when (val result = repository.joinVideoCallQueue()) {
            is DataResult.Success -> {
                val fresh = result.value
                ticket = fresh
                store.update { it.copy(queue = fresh) }
                signaling.connect(
                    url = fresh.signalingUrl,
                    sessionId = store.current.sessionId.orEmpty(),
                    queueId = fresh.queueId,
                )
            }

            is DataResult.Failure -> onReconnectFailure(result.error)
        }
    }

    /**
     * `ONBOARDING_INVALID_STEP` di sini bukan kegagalan — itu kabar baik.
     *
     * Antrean hanya menerima sesi yang sedang di `VIDEO_CALL`, jadi penolakan ini berarti
     * server sudah memindahkan step: panggilannya **selesai** saat kita terputus, dan
     * hasilnya sudah disubmit CS. Yang benar adalah mengikuti step itu, bukan menampilkan
     * kegagalan atas verifikasi yang sebenarnya berhasil.
     */
    private suspend fun onReconnectFailure(error: ApiFailure) {
        if (error is ApiFailure.InvalidStep) {
            stopTimer()
            signaling.disconnect()
            _uiState.update { it.copy(connectionState = VideoCallConnectionState.SELESAI) }
            completeStep(_uiState.value.namaPetugas)
            return
        }
        failCall()
    }

    private fun failCall() {
        stopTimer()
        _uiState.update { it.copy(connectionState = VideoCallConnectionState.GAGAL) }
    }

    /**
     * Tiket dibuang supaya layar Antrean mengambil yang baru.
     *
     * `queue = null` itulah pemicunya: `LaunchedEffect` di entri Antrean memanggil
     * `QueueJoinRequested` setiap kali tiketnya kosong.
     */
    private fun prepareRejoin() {
        signaling.disconnect()
        store.update { it.copy(queue = null) }
    }

    // -- Kontrol media ----------------------------------------------------------

    private fun toggleMute() {
        val muted = !_uiState.value.isMuted
        webRtc.setMuted(muted)
        signaling.sendMediaControl(
            if (muted) MediaControlAction.MUTE_AUDIO else MediaControlAction.UNMUTE_AUDIO,
        )
        _uiState.update { it.copy(isMuted = muted) }
    }

    private fun switchCamera() {
        val isFront = webRtc.switchCamera()
        signaling.sendMediaControl(MediaControlAction.SWITCH_CAMERA)
        _uiState.update { it.copy(isFrontCamera = isFront) }
    }

    fun attachLocalRenderer(renderer: SurfaceViewRenderer) = webRtc.attachLocalRenderer(renderer)

    fun attachRemoteRenderer(renderer: SurfaceViewRenderer) = webRtc.attachRemoteRenderer(renderer)

    fun pauseLocalVideo() = webRtc.pauseLocalVideo()

    fun resumeLocalVideo() = webRtc.resumeLocalVideo()

    // -- Timer ------------------------------------------------------------------

    private fun startTimer() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            var elapsed = 0
            while (true) {
                _uiState.update { it.copy(durasiPanggilan = formatDuration(elapsed)) }
                delay(TICK_MS)
                elapsed += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / SECONDS_PER_MINUTE
        val rest = seconds % SECONDS_PER_MINUTE
        return "%02d:%02d".format(minutes, rest)
    }

    override fun onCleared() {
        stopTimer()
        reconnectJob?.cancel()
        signaling.disconnect()
        webRtc.release()
        super.onCleared()
    }

    private companion object {
        const val TICK_MS = 1_000L
        const val SECONDS_PER_MINUTE = 60
        const val MAX_RECONNECT = 5
        val BACKOFF_MS = longArrayOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L)
    }
}
