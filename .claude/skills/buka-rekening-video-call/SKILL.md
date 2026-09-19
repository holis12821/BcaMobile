---
name: buka-rekening-video-call
description: Video call e-KYC workflow untuk buka rekening BCA — WebRTC native PeerConnection, WebSocket signaling OkHttp, ICE/TURN configuration, media tracks, SurfaceViewRenderer di Compose, antrean queue management, call controls (mute/switch camera/end call), reconnection strategy, dan security (DTLS-SRTP, FLAG_SECURE). Gunakan saat implementasi video call screen, WebRTC setup, WebSocket signaling, antrean video call, camera/microphone permission untuk video call, PiP layout, atau koneksi real-time dengan CS. JANGAN trigger untuk kamera foto KTP (itu `buka-rekening-native-android`), biometrik face (idem), atau navigasi (itu `compose-architecture`).
---

# Skill: Buka Rekening — Video Call e-KYC Workflow

Skill khusus untuk implementasi video call verifikasi identitas antara nasabah
dan Customer Service Halo BCA. Mencakup: permission, WebRTC native, signaling,
UI state, connection handling, dan keamanan.

**Trigger**: saat menyentuh screen Antrean Video Call, Video Call e-KYC,
WebRTC setup, WebSocket signaling, camera/microphone permission untuk video call,
PiP layout video call, atau koneksi real-time dengan CS.

**Jangan trigger** untuk: kamera foto KTP (itu `buka-rekening-native-android`),
biometrik face (idem), navigasi (itu `compose-architecture`).

---

## Architecture Overview

```
┌──────────────────────────────────────────────────┐
│                   Mobile App                      │
│                                                   │
│  AntreanScreen ──→ VideoCallScreen                │
│       │                  │                        │
│  QueueViewModel    VideoCallViewModel             │
│       │                  │                        │
│  WebSocketClient ←──────→│                        │
│       │            PeerConnection                 │
│       │            ├─ localVideoTrack             │
│       │            ├─ remoteVideoTrack            │
│       │            ├─ localAudioTrack             │
│       │            └─ dataChannel (optional)      │
└───────┼──────────────────┼────────────────────────┘
        │                  │
   TLS WebSocket      DTLS-SRTP (media)
        │                  │
┌───────┼──────────────────┼────────────────────────┐
│       ▼                  ▼                        │
│  Signaling Server    TURN Server                  │
│  (relay SDP/ICE)     (media relay if NAT)         │
│                                                   │
│              Backend Infrastructure               │
└───────────────────────────────────────────────────┘
```

---

## 1. Dependency (perlu persetujuan)

```kotlin
// build.gradle.kts
// Native WebRTC — BUKAN Twilio/Agora (cost & data sovereignty)
implementation("io.getstream:stream-webrtc-android:1.3.1")
// Atau official Google WebRTC:
// implementation("org.webrtc:google-webrtc:1.0.32006")
```

**Kenapa native WebRTC, bukan Twilio/Agora:**
- Data sovereignty: video call perbankan POJK harus di-serve dari Indonesia
- No vendor lock-in: WebRTC adalah standard W3C/IETF
- Cost: per-minute pricing Twilio/Agora mahal untuk volume banking
- Compliance: bisa audit kode, kontrol encryption end-to-end

---

## 2. Permission Flow (wajib sebelum video call)

### Permissions Needed
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Sequential Permission Request
```
[Antrean Screen]
  User tap "Tunggu Panggilan"
    ↓
  Check CAMERA permission
    ├─ Granted → Check RECORD_AUDIO
    │              ├─ Granted → Check network → Join queue
    │              └─ Denied → Show explainer "Mikrofon diperlukan untuk video call"
    └─ Denied → Show explainer "Kamera diperlukan untuk verifikasi identitas"
```

### Aturan Permission
- Minta CAMERA dan RECORD_AUDIO saat user tap "Tunggu Panggilan" (bukan saat screen dibuka)
- Jika `shouldShowRationale` = true → tampilkan dialog penjelasan + tombol "Izinkan"
- Jika permanently denied → tampilkan instruksi buka Settings + deep link ke app settings
- Check koneksi internet dan kualitas (WiFi/4G minimum) sebelum join queue
- JANGAN proceed ke video call tanpa kedua permission

---

## 3. WebSocket Signaling Client

### Connection Lifecycle
```
1. User tap "Tunggu Panggilan"
2. Call POST /onboarding/video-call/queue → get signaling_url
3. Connect WebSocket ke signaling_url
4. Send: {"type": "join", "session_id": "...", "queue_id": "..."}
5. Receive queue_update messages (posisi antrean)
6. Receive agent_assigned → init WebRTC PeerConnection
7. Exchange SDP offer/answer via WebSocket
8. Exchange ICE candidates via WebSocket
9. Media flows peer-to-peer (bypasses signaling server)
10. call_ended → close PeerConnection + WebSocket
```

### SignalingClient Interface
```kotlin
interface SignalingClient {
    val events: SharedFlow<SignalingEvent>

    suspend fun connect(url: String)
    suspend fun sendOffer(sdp: SessionDescription)
    suspend fun sendAnswer(sdp: SessionDescription)
    suspend fun sendIceCandidate(candidate: IceCandidate)
    suspend fun sendMediaControl(action: MediaControlAction)
    fun disconnect()
}

sealed interface SignalingEvent {
    data class QueueUpdate(val position: Int, val estimatedWait: Int) : SignalingEvent
    data class AgentAssigned(val name: String, val employeeId: String) : SignalingEvent
    data class Answer(val sdp: SessionDescription) : SignalingEvent
    data class IceCandidate(val candidate: IceCandidate) : SignalingEvent
    data class Instruction(val text: String) : SignalingEvent
    data class CallEnded(val result: String, val duration: Int) : SignalingEvent
    data class Error(val message: String) : SignalingEvent
    data object Disconnected : SignalingEvent
}

enum class MediaControlAction { MUTE_AUDIO, UNMUTE_AUDIO, SWITCH_CAMERA }
```

### WebSocket Implementation
```kotlin
// Gunakan OkHttp WebSocket (sudah ada di project)
class OkHttpSignalingClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) : SignalingClient {

    private var webSocket: WebSocket? = null
    private val _events = MutableSharedFlow<SignalingEvent>(extraBufferCapacity = 16)
    override val events = _events.asSharedFlow()

    override suspend fun connect(url: String) {
        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val event = parseSignalingMessage(text)
                _events.tryEmit(event)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _events.tryEmit(SignalingEvent.Error(t.message ?: "Connection failed"))
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _events.tryEmit(SignalingEvent.Disconnected)
            }
        })
    }
    // ... send methods: webSocket?.send(json.encodeToString(message))
}
```

### Reconnection Strategy
- Auto-reconnect dengan exponential backoff: 1s → 2s → 4s → 8s → 16s (max)
- Max retry: 5 kali
- Jika reconnect dalam grace period (60s): resume session
- Jika melebihi grace period: tampilkan error, tawarkan rejoin queue

---

## 4. WebRTC PeerConnection Setup

### Initialization
```kotlin
class WebRtcClient(
    private val context: Context,
    private val signalingClient: SignalingClient,
) {
    private val eglBase = EglBase.create()
    private val peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false) // production: false
                .createInitializationOptions()
        )
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .createPeerConnectionFactory()
    }
}
```

### ICE/TURN Configuration
```kotlin
private fun createPeerConnection(): PeerConnection {
    val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.bcamobile.id:3478").createIceServer(),
        PeerConnection.IceServer.builder("turn:turn.bcamobile.id:3478")
            .setUsername(turnUsername)  // time-limited credential dari server
            .setPassword(turnPassword)
            .createIceServer(),
    )
    val config = PeerConnection.RTCConfiguration(iceServers).apply {
        sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        iceTransportsType = PeerConnection.IceTransportsType.ALL
    }
    return peerConnectionFactory.createPeerConnection(config, peerConnectionObserver)!!
}
```

### Media Tracks
```kotlin
// Local video (kamera depan)
fun startLocalVideo(surfaceViewRenderer: SurfaceViewRenderer) {
    val videoCapturer = Camera2Enumerator(context).run {
        deviceNames.firstOrNull { isFrontFacing(it) }?.let { createCapturer(it, null) }
    } ?: throw IllegalStateException("No front camera")

    val videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast)
    videoCapturer.initialize(
        SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext),
        context, videoSource.capturerObserver
    )
    videoCapturer.startCapture(720, 1280, 30) // 720p, 30fps

    localVideoTrack = peerConnectionFactory.createVideoTrack("local_video", videoSource)
    localVideoTrack?.addSink(surfaceViewRenderer)

    // Audio
    val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
    localAudioTrack = peerConnectionFactory.createAudioTrack("local_audio", audioSource)
}

// Remote video (agent CS)
fun onRemoteTrackReceived(track: VideoTrack, renderer: SurfaceViewRenderer) {
    track.addSink(renderer)
}
```

### Offer/Answer Flow
```kotlin
// Saat agent_assigned diterima:
fun createOfferAndSend() {
    peerConnection?.createOffer(object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) {
            peerConnection?.setLocalDescription(this, sdp)
            signalingClient.sendOffer(sdp) // kirim via WebSocket
        }
    }, MediaConstraints())
}

// Saat answer diterima via WebSocket:
fun onAnswerReceived(sdp: SessionDescription) {
    peerConnection?.setRemoteDescription(sdpObserver, sdp)
}

// ICE candidates (kedua arah):
// PeerConnectionObserver.onIceCandidate → signalingClient.sendIceCandidate(candidate)
// signalingClient.events.IceCandidate → peerConnection.addIceCandidate(candidate)
```

---

## 5. ViewModel & UI State

### VideoCallUiState
```kotlin
data class VideoCallUiState(
    val connectionState: ConnectionState = ConnectionState.CONNECTING,
    val agentName: String = "",
    val agentEmployeeId: String = "",
    val callDuration: String = "00:00",
    val isMuted: Boolean = false,
    val isFrontCamera: Boolean = true,
    val currentInstruction: String = "",
    val isEncrypted: Boolean = true,
)

enum class ConnectionState {
    CONNECTING,    // WebRTC negotiating
    CONNECTED,     // Media flowing
    RECONNECTING,  // Temporary disconnect
    FAILED,        // Cannot recover
    ENDED,         // Call ended normally
}
```

### AntreanUiState
```kotlin
data class AntreanUiState(
    val queueState: QueueState = QueueState.IDLE,
    val queueNumber: String = "",
    val position: Int = 0,
    val estimatedWaitSeconds: Int = 0,
    val isWithinOperatingHours: Boolean = true,
)

enum class QueueState {
    IDLE,          // Belum join
    JOINING,       // Calling API
    WAITING,       // Dalam antrean
    AGENT_FOUND,   // Agent assigned, preparing call
    ERROR,         // Gagal join/connect
}
```

### Call Duration Timer
```kotlin
// Di ViewModel
private var timerJob: Job? = null

fun startCallTimer() {
    val startTime = SystemClock.elapsedRealtime()
    timerJob = viewModelScope.launch {
        while (isActive) {
            val elapsed = (SystemClock.elapsedRealtime() - startTime) / 1000
            val minutes = elapsed / 60
            val seconds = elapsed % 60
            _uiState.update { it.copy(callDuration = "%02d:%02d".format(minutes, seconds)) }
            delay(1000)
        }
    }
}
```

---

## 6. Compose UI Integration

### Video Renderer di Compose
```kotlin
@Composable
fun VideoRenderer(
    modifier: Modifier = Modifier,
    onSurfaceReady: (SurfaceViewRenderer) -> Unit,
) {
    val eglBase = remember { EglBase.create() }

    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                init(eglBase.eglBaseContext, null)
                setMirror(true) // mirror untuk front camera
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                onSurfaceReady(this)
            }
        },
        modifier = modifier,
        onRelease = { it.release() },
    )

    DisposableEffect(Unit) {
        onDispose { eglBase.release() }
    }
}
```

### PiP Layout (Agent besar, Nasabah kecil)
```kotlin
@Composable
fun VideoCallLayout(
    onLocalSurfaceReady: (SurfaceViewRenderer) -> Unit,
    onRemoteSurfaceReady: (SurfaceViewRenderer) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Agent video (full screen)
        VideoRenderer(
            modifier = Modifier.fillMaxSize(),
            onSurfaceReady = onRemoteSurfaceReady,
        )

        // Local video (PiP, bottom-right)
        VideoRenderer(
            modifier = Modifier
                .size(width = Spacing.s10 * 2, height = Spacing.s10 * 3)
                .align(Alignment.BottomEnd)
                .padding(Spacing.s4)
                .clip(AppShape.R6),
            onSurfaceReady = onLocalSurfaceReady,
        )
    }
}
```

---

## 7. Call Controls

### Mute/Unmute
```kotlin
fun toggleMute() {
    val newMuted = !_uiState.value.isMuted
    localAudioTrack?.setEnabled(!newMuted)
    signalingClient.sendMediaControl(
        if (newMuted) MUTE_AUDIO else UNMUTE_AUDIO
    )
    _uiState.update { it.copy(isMuted = newMuted) }
}
```

### Switch Camera
```kotlin
fun switchCamera() {
    (videoCapturer as? Camera2Capturer)?.switchCamera(null)
    _uiState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    signalingClient.sendMediaControl(SWITCH_CAMERA)
}
```

### End Call
```kotlin
fun endCall() {
    timerJob?.cancel()
    localVideoTrack?.dispose()
    localAudioTrack?.dispose()
    peerConnection?.close()
    signalingClient.disconnect()
    // Navigate ke screen berikutnya (Buat Kredensial)
}
```

---

## 8. Error Handling & Recovery

| Error | Recovery |
|-------|---------|
| WebSocket disconnect | Auto-reconnect (5 retry, exponential backoff) |
| ICE connection failed | Retry ICE gathering, switch TURN relay |
| Media permission revoked mid-call | End call, show error |
| Network switch (WiFi → 4G) | ICE restart, brief reconnect |
| Agent disconnect | Show "Agent terputus, menunggu...", auto-reassign |
| Timeout (no agent 15min) | Offer jadwalkan ulang |

### ICE Restart
```kotlin
fun restartIce() {
    peerConnection?.restartIce()
    // Akan trigger onIceGatheringChange → kirim candidates baru
}
```

---

## 9. Security Requirements

1. **DTLS-SRTP** — WebRTC media encrypted by default (jangan disable)
2. **TLS 1.3** — WebSocket signaling wajib pakai WSS (bukan WS)
3. **Certificate pinning** — Pin certificate signaling server
4. **No recording on device** — JANGAN rekam video di sisi mobile
5. **FLAG_SECURE** — Set pada Activity saat video call aktif
6. **Screen capture prevention** — setSecure(true) pada Window
7. **Call metadata** — Log hanya duration + agent_id, BUKAN konten call

```kotlin
// Di Activity saat video call screen aktif
window.setFlags(
    WindowManager.LayoutParams.FLAG_SECURE,
    WindowManager.LayoutParams.FLAG_SECURE
)
```

---

## 10. Lifecycle Handling

```kotlin
// ViewModel harus handle lifecycle:
// - App ke background: keep connection, pause local video
// - App kembali: resume local video
// - Screen rotation: PeerConnection survive (scoped ke ViewModel)
// - Process death: connection lost, user harus rejoin queue

// Di Compose screen:
LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
    viewModel.pauseLocalVideo()
}
LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    viewModel.resumeLocalVideo()
}
```

### Aturan Lifecycle
- PeerConnection dan WebSocket hidup di ViewModel (survive rotation)
- Kamera pause saat app background, resume saat foreground
- Jika app di-kill OS: connection hilang, user harus rejoin queue
- JANGAN keep wake lock — biarkan OS manage
- Audio routing: speaker by default (bukan earpiece) untuk video call