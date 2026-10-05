package id.bca.bcamobile.core.videocall

import id.bca.bcamobile.core.network.OnboardingNetwork
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * [SignalingClient] di atas WebSocket OkHttp — klien yang sama dengan jaringan onboarding,
 * jadi certificate pinning dan header standar ikut berlaku tanpa konfigurasi kedua.
 *
 * Tidak di-`Singleton`: satu instance per panggilan. Socket signaling sekali pakai
 * (`QueueTicket.signalingUrl`), jadi instance yang dipakai ulang akan mencoba menyambung
 * dengan token yang sudah hangus.
 */
class OkHttpSignalingClient @Inject constructor(
    // `@param:` eksplisit: qualifier-nya memang untuk parameter konstruktor yang dibaca
    // Dagger, bukan untuk properti. Tanpa itu Kotlin memperingatkan bahwa target
    // bawaannya akan berubah di versi mendatang.
    @param:OnboardingNetwork private val okHttpClient: OkHttpClient,
    private val json: Json,
) : SignalingClient {

    private var webSocket: WebSocket? = null

    /**
     * `extraBufferCapacity` ada supaya [MutableSharedFlow.tryEmit] tidak pernah membuang
     * peristiwa: ICE candidate datang berurutan dan cepat, dan satu yang hilang membuat
     * negosiasi menggantung tanpa pesan error.
     */
    private val _events = MutableSharedFlow<SignalingEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<SignalingEvent> = _events.asSharedFlow()

    override fun connect(url: String, sessionId: String, queueId: String) {
        // Sambungan lama ditutup lebih dulu: dua socket hidup berarti dua jawaban SDP.
        disconnect()

        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    // `join` dikirim dari sini, bukan dari pemanggil: sebelum socket
                    // terbuka pesannya hanya masuk antrean OkHttp tanpa jaminan urutan.
                    send(JoinMessage(sessionId = sessionId, queueId = queueId))
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    parseSignalingMessage(json, text)?.let { _events.tryEmit(it) }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    _events.tryEmit(SignalingEvent.Failed(t.message.orEmpty()))
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _events.tryEmit(SignalingEvent.Disconnected)
                }
            },
        )
    }

    override fun sendOffer(sdp: String) {
        send(SdpMessage(type = TYPE_OFFER, sdp = sdp))
    }

    override fun sendIceCandidate(candidate: String, sdpMid: String?, sdpMLineIndex: Int) {
        send(
            IceCandidateMessage(
                candidate = IceCandidatePayload(
                    candidate = candidate,
                    sdpMid = sdpMid,
                    sdpMLineIndex = sdpMLineIndex,
                ),
            ),
        )
    }

    override fun sendMediaControl(action: MediaControlAction) {
        send(MediaControlMessage(action = action.wireValue))
    }

    override fun disconnect() {
        // 1000 = penutupan normal. Tanpa kode, server mencatatnya sebagai putus tak wajar
        // dan bisa menahan slot antrean.
        webSocket?.close(CLOSE_NORMAL, null)
        webSocket = null
    }

    private inline fun <reified T> send(message: T) {
        webSocket?.send(json.encodeToString(message))
    }

    private companion object {
        /** Penutupan normal. Tanpa kode ini server mencatatnya sebagai putus tak wajar. */
        const val CLOSE_NORMAL = 1000
    }
}
