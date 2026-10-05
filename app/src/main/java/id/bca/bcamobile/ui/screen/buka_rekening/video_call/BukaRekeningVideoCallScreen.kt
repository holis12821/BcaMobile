package id.bca.bcamobile.ui.screen.buka_rekening.video_call

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

// -- Data Model ---------------------------------------------------------------


// -- Main Screen ---------------------------------------------------------------

@Composable
fun BukaRekeningVideoCallScreen(
    state: VideoCallUiState,
    /**
     * Konteks EGL dari `WebRtcClient`. `null` hanya di preview: tanpa konteks tidak ada
     * `SurfaceViewRenderer` yang bisa diinisialisasi, dan layar jatuh ke latar polos.
     */
    eglBaseContext: EglBase.Context?,
    onLocalRendererReady: (SurfaceViewRenderer) -> Unit,
    onRemoteRendererReady: (SurfaceViewRenderer) -> Unit,
    onBack: () -> Unit,
    onMuteToggle: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEndCall: () -> Unit,
    onRejoin: () -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.buka_rekening_vc_verifikasi_title),
                onBackClick = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AppColor.Neutral900),
        ) {
            // Layer 0: video petugas. Ini yang dulu tidak ada sama sekali — area ini cuma
            // Neutral900 polos, dan itu sumber "blank"-nya.
            if (eglBaseContext != null) {
                VideoRenderer(
                    eglBaseContext = eglBaseContext,
                    mirror = false,
                    onRendererReady = onRemoteRendererReady,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Selama belum ada media petugas, katakan apa yang sedang terjadi alih-alih
            // membiarkan layar hitam mengaku "Terhubung".
            if (state.connectionState != VideoCallConnectionState.TERHUBUNG) {
                RemotePlaceholder(
                    state = state,
                    onRejoin = onRejoin,
                    onRequestPermissions = onRequestPermissions,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            // Layer 1: Gradient overlays (top & bottom darkening)
            VideoGradientOverlay()

            // Layer 2: Top status overlay
            TopStatusOverlay(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart),
            )

            // Layer 3: reticle hanya berarti kalau ada video untuk dibingkai.
            if (state.connectionState == VideoCallConnectionState.TERHUBUNG) {
                VerificationReticle(
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            // Layer 4: Bottom section (instruction, PiP, controls, POJK)
            BottomSection(
                state = state,
                eglBaseContext = eglBaseContext,
                onLocalRendererReady = onLocalRendererReady,
                onMuteToggle = onMuteToggle,
                onSwitchCamera = onSwitchCamera,
                onEndCall = onEndCall,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            )
        }
    }
}

// -- Renderer video ------------------------------------------------------------

/**
 * `SurfaceViewRenderer` WebRTC di dalam Compose.
 *
 * `AndroidView` karena WebRTC merender ke permukaan OpenGL milik View, bukan ke canvas
 * Compose — tidak ada padanan Compose-native-nya.
 *
 * [onRendererReady] dipanggil sekali saat permukaannya siap, dan di situ ViewModel
 * menyambungkan track. Urutannya tidak dijamin: track bisa datang sebelum permukaan ada,
 * jadi `WebRtcClient` menyimpan keduanya dan menyambungkan mana pun yang datang terakhir.
 *
 * `onRelease` wajib `release()`: tanpa itu permukaan dan buffer EGL-nya menggantung setiap
 * kali layar dilepas, dan panggilan kedua kehabisan memori grafis.
 */
@Composable
private fun VideoRenderer(
    eglBaseContext: EglBase.Context,
    mirror: Boolean,
    onRendererReady: (SurfaceViewRenderer) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                init(eglBaseContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
                onRendererReady(this)
            }
        },
        update = { it.setMirror(mirror) },
        onRelease = { it.release() },
        modifier = modifier,
    )
}

/**
 * Apa yang tampil selama media petugas belum mengalir.
 *
 * Ada supaya layar tidak pernah lagi hitam tanpa penjelasan: setiap tahap punya
 * kalimatnya sendiri, dan keadaan gagal membawa jalan keluarnya.
 */
@Composable
private fun RemotePlaceholder(
    state: VideoCallUiState,
    onRejoin: () -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier.padding(Spacing.s6),
    ) {
        // Memuat hanya saat memang sedang menunggu sesuatu. Keadaan izin, gagal, dan
        // selesai menunggu tindakan nasabah, bukan jaringan.
        if (!state.isPerluAntreanUlang &&
            !state.isPerluIzin &&
            state.connectionState != VideoCallConnectionState.SELESAI
        ) {
            CircularProgressIndicator(color = AppColor.Neutral100)
        }
        Text(
            text = stringResource(
                when (state.connectionState) {
                    VideoCallConnectionState.IZIN_DIBUTUHKAN ->
                        R.string.buka_rekening_vc_status_izin
                    VideoCallConnectionState.MENUNGGU_PETUGAS ->
                        R.string.buka_rekening_vc_status_menunggu
                    VideoCallConnectionState.MENGHUBUNGKAN ->
                        R.string.buka_rekening_vc_status_menghubungkan
                    VideoCallConnectionState.MENYAMBUNG_ULANG ->
                        R.string.buka_rekening_vc_status_menyambung_ulang
                    VideoCallConnectionState.GAGAL ->
                        R.string.buka_rekening_vc_status_gagal
                    VideoCallConnectionState.SELESAI ->
                        R.string.buka_rekening_vc_status_selesai
                    VideoCallConnectionState.TERHUBUNG ->
                        R.string.buka_rekening_vc_status_menghubungkan
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColor.Neutral100,
            textAlign = TextAlign.Center,
        )
        when {
            state.isPerluIzin -> TextButton(onClick = onRequestPermissions) {
                Text(
                    text = stringResource(R.string.buka_rekening_vc_izinkan),
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColor.Primary200,
                )
            }

            state.isPerluAntreanUlang -> TextButton(onClick = onRejoin) {
                Text(
                    text = stringResource(R.string.buka_rekening_vc_antrean_ulang),
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColor.Primary200,
                )
            }
        }
    }
}

// -- Gradient Overlay ---------------------------------------------------------

@Composable
private fun VideoGradientOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to AppColor.Neutral1000.copy(alpha = AppAlpha.A60),
                        0.3f to Color.Transparent,
                        0.6f to Color.Transparent,
                        1.0f to AppColor.Neutral1000.copy(alpha = AppAlpha.A90),
                    ),
                ),
            ),
    )
}

// -- Top Status Overlay -------------------------------------------------------

@Composable
private fun TopStatusOverlay(
    state: VideoCallUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier.padding(Spacing.s4),
    ) {
        // Row 1: Encryption badge + Timer badge
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            EncryptionBadge()
            // Durasi yang berjalan sebelum media mengalir hanya angka palsu.
            if (state.isDurasiTampil) {
                TimerBadge(durasiPanggilan = state.durasiPanggilan)
            }
        }

        // Row 2: tag petugas baru ada setelah `agent_assigned` membawa namanya.
        if (state.namaPetugas.isNotBlank()) {
            AgentInfoTag(
                namaPetugas = state.namaPetugas,
                statusTerhubung = state.connectionState == VideoCallConnectionState.TERHUBUNG,
            )
        }
    }
}

@Composable
private fun EncryptionBadge(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                AppColor.Neutral900.copy(alpha = AppAlpha.A70),
                AppShape.Full,
            )
            .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = AppColor.Success400,
            modifier = Modifier.size(Spacing.s4),
        )
        Text(
            text = stringResource(R.string.buka_rekening_vc_enkripsi),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = AppColor.Neutral100,
        )
    }
}

@Composable
private fun TimerBadge(
    durasiPanggilan: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                AppColor.Neutral900.copy(alpha = AppAlpha.A70),
                AppShape.Full,
            )
            .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
    ) {
        Box(
            modifier = Modifier
                .size(Spacing.s2)
                .background(AppColor.Danger500, AppShape.Full),
        )
        Text(
            text = durasiPanggilan,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = AppColor.Neutral100,
        )
    }
}

@Composable
private fun AgentInfoTag(
    namaPetugas: String,
    statusTerhubung: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                AppColor.Neutral100.copy(alpha = AppAlpha.A90),
                AppShape.R4,
            )
            .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
    ) {
        if (statusTerhubung) {
            Box(
                modifier = Modifier
                    .size(Spacing.s2)
                    .background(AppColor.Success500, AppShape.Full),
            )
        }
        Text(
            text = namaPetugas,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = AppColor.Neutral900,
        )
        Text(
            text = stringResource(R.string.buka_rekening_vc_petugas_halo),
            style = MaterialTheme.typography.labelSmall,
            color = AppColor.Neutral600,
        )
        Text(
            text = stringResource(R.string.buka_rekening_vc_hd),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = AppColor.Primary900,
            modifier = Modifier
                .background(AppColor.Primary100, AppShape.R2)
                .padding(horizontal = Spacing.s1, vertical = Spacing.s0),
        )
    }
}

// -- Verification Reticle -----------------------------------------------------

@Composable
private fun VerificationReticle(modifier: Modifier = Modifier) {
    val reticleColor = AppColor.Primary200.copy(alpha = AppAlpha.A50)
    val reticleStroke = StrokeWidth.w1
    val reticleCorner = Spacing.s7

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .width(AppSize.ScannerFrame)
            .aspectRatio(8f / 9f)
            .drawBehind {
                drawRoundRect(
                    color = reticleColor,
                    style = Stroke(
                        width = reticleStroke.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(12f, 12f),
                        ),
                    ),
                    cornerRadius = CornerRadius(reticleCorner.toPx()),
                )
            }
            .padding(bottom = Spacing.s3),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    AppColor.Neutral900.copy(alpha = AppAlpha.A60),
                    AppShape.Full,
                )
                .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_face),
                contentDescription = null,
                tint = AppColor.Primary200,
                modifier = Modifier.size(Spacing.s4),
            )
            Text(
                text = stringResource(R.string.buka_rekening_vc_fokus_wajah_ktp),
                style = MaterialTheme.typography.labelSmall,
                color = AppColor.Neutral100,
            )
        }
    }
}

// -- Bottom Section -----------------------------------------------------------

@Composable
private fun BottomSection(
    state: VideoCallUiState,
    eglBaseContext: EglBase.Context?,
    onLocalRendererReady: (SurfaceViewRenderer) -> Unit,
    onMuteToggle: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        modifier = modifier.padding(Spacing.s4),
    ) {
        // Instruction bubble + PiP side-by-side
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.instruksiPetugas.isNotBlank()) {
                InstructionBubble(
                    text = state.instruksiPetugas,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            PipSelfView(
                eglBaseContext = eglBaseContext,
                isFrontCamera = state.isFrontCamera,
                onRendererReady = onLocalRendererReady,
            )
        }

        // Floating call controls bar
        FloatingControlsBar(
            isMuted = state.isMuted,
            isEnabled = state.isKontrolAktif,
            onMuteToggle = onMuteToggle,
            onSwitchCamera = onSwitchCamera,
            onEndCall = onEndCall,
        )

        // Regulatory POJK banner
        PojkBanner(modifier = Modifier.fillMaxWidth())
    }
}

// -- Instruction Bubble -------------------------------------------------------

@Composable
private fun InstructionBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val prefixText = stringResource(R.string.buka_rekening_vc_petugas_prefix)

    Surface(
        shape = AppShape.R7,
        color = AppColor.Neutral100.copy(alpha = AppAlpha.A90),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s1),
            modifier = Modifier.padding(Spacing.s3),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_record_voice_over),
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(Spacing.s4),
                )
                Text(
                    text = stringResource(R.string.buka_rekening_vc_instruksi_langsung),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = primaryColor,
                )
            }
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor,
                        ),
                    ) {
                        append(prefixText)
                        append(" ")
                    }
                    append(text)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = AppColor.Neutral900,
            )
        }
    }
}

// -- PiP Self View ------------------------------------------------------------

@Composable
private fun PipSelfView(
    eglBaseContext: EglBase.Context?,
    isFrontCamera: Boolean,
    onRendererReady: (SurfaceViewRenderer) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(AppSize.LogoContainer)
            .aspectRatio(3f / 4f)
            .clip(AppShape.R6)
            .background(AppColor.Neutral700)
            .border(StrokeWidth.w0, AppColor.Neutral100.copy(alpha = AppAlpha.A30), AppShape.R6),
    ) {
        if (eglBaseContext != null) {
            // Kamera sendiri hidup sejak layar dibuka, sebelum petugas datang: itu yang
            // membuat layar berhenti blank selama menunggu. Dulu di sini hanya ikon statis.
            VideoRenderer(
                eglBaseContext = eglBaseContext,
                // Cermin hanya untuk kamera depan — kamera belakang yang dicermin
                // membuat tulisan di e-KTP terbalik.
                mirror = isFrontCamera,
                onRendererReady = onRendererReady,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_face),
                contentDescription = null,
                tint = AppColor.Neutral500,
                modifier = Modifier
                    .size(Spacing.s7)
                    .align(Alignment.Center),
            )
        }

        // "Anda" label (bottom-start)
        Text(
            text = stringResource(R.string.buka_rekening_vc_anda),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = AppColor.Neutral100,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = Spacing.s2, bottom = Spacing.s1)
                .background(
                    AppColor.Neutral900.copy(alpha = AppAlpha.A70),
                    AppShape.R2,
                )
                .padding(horizontal = Spacing.s1, vertical = Spacing.s0),
        )

        // Verified badge (top-end)
        Icon(
            painter = painterResource(R.drawable.ic_verified),
            contentDescription = null,
            tint = AppColor.Success500,
            modifier = Modifier
                .size(Spacing.s4)
                .align(Alignment.TopEnd)
                .padding(top = Spacing.s1, end = Spacing.s1),
        )
    }
}

// -- Floating Controls Bar ----------------------------------------------------

@Composable
private fun FloatingControlsBar(
    isMuted: Boolean,
    isEnabled: Boolean,
    onMuteToggle: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth(0.75f)
            .background(
                AppColor.Neutral900.copy(alpha = AppAlpha.A90),
                AppShape.Full,
            )
            .padding(horizontal = Spacing.s6, vertical = Spacing.s3),
    ) {
        // Mic toggle
        IconButton(
            onClick = onMuteToggle,
            enabled = isEnabled,
            modifier = Modifier
                .size(AppSize.MinTouchTarget)
                .background(
                    if (isMuted) AppColor.Danger500
                    else AppColor.Neutral100.copy(alpha = AppAlpha.A20),
                    AppShape.Full,
                ),
        ) {
            Icon(
                painter = painterResource(
                    if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic,
                ),
                contentDescription = stringResource(
                    if (isMuted) R.string.buka_rekening_vc_unmute
                    else R.string.buka_rekening_vc_mute,
                ),
                tint = AppColor.Neutral100,
                modifier = Modifier.size(Spacing.s6),
            )
        }

        // Camera switch
        IconButton(
            onClick = onSwitchCamera,
            enabled = isEnabled,
            modifier = Modifier
                .size(AppSize.MinTouchTarget)
                .background(
                    AppColor.Neutral100.copy(alpha = AppAlpha.A20),
                    AppShape.Full,
                ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cameraswitch),
                contentDescription = stringResource(R.string.buka_rekening_vc_balik_kamera),
                tint = AppColor.Neutral100,
                modifier = Modifier.size(Spacing.s6),
            )
        }

        // Akhiri panggilan tidak pernah dimatikan: nasabah harus selalu bisa keluar,
        // termasuk saat antrean masih berjalan dan saat sambungan gagal.
        IconButton(
            onClick = onEndCall,
            modifier = Modifier
                .size(AppSize.MinTouchTarget)
                .background(AppColor.Danger500, AppShape.Full),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_call_end),
                contentDescription = stringResource(R.string.buka_rekening_vc_akhiri),
                tint = AppColor.Neutral100,
                modifier = Modifier.size(Spacing.s6),
            )
        }
    }
}

// -- POJK Banner --------------------------------------------------------------

@Composable
private fun PojkBanner(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                AppColor.Neutral900.copy(alpha = AppAlpha.A60),
                AppShape.R4,
            )
            .padding(horizontal = Spacing.s3, vertical = Spacing.s2),
    ) {
        Text(
            text = stringResource(R.string.buka_rekening_vc_pojk_regulasi),
            style = MaterialTheme.typography.labelSmall,
            color = AppColor.Neutral100.copy(alpha = AppAlpha.A80),
            textAlign = TextAlign.Center,
        )
    }
}

// -- Previews -----------------------------------------------------------------

/**
 * `eglBaseContext = null` di seluruh preview: `SurfaceViewRenderer` butuh konteks EGL
 * sungguhan yang hanya ada saat `WebRtcClient` hidup, jadi preview menampilkan tata
 * letaknya tanpa video.
 */
@Preview(showBackground = true, name = "Terhubung")
@Composable
private fun VideoCallTerhubungPreview() {
    BcaMobileTheme {
        BukaRekeningVideoCallScreen(
            state = VideoCallUiState(
                connectionState = VideoCallConnectionState.TERHUBUNG,
                namaPetugas = "Sarah Adisti",
                idPetugas = "CS-1042",
                durasiPanggilan = "03:15",
                instruksiPetugas =
                    "Mohon posisikan fisik e-KTP Anda di depan kamera samping wajah.",
            ),
            eglBaseContext = null,
            onLocalRendererReady = {},
            onRemoteRendererReady = {},
            onBack = {},
            onMuteToggle = {},
            onSwitchCamera = {},
            onEndCall = {},
            onRejoin = {},
            onRequestPermissions = {},
        )
    }
}

@Preview(showBackground = true, name = "Izin dibutuhkan")
@Composable
private fun VideoCallIzinPreview() {
    BcaMobileTheme {
        BukaRekeningVideoCallScreen(
            state = VideoCallUiState(
                connectionState = VideoCallConnectionState.IZIN_DIBUTUHKAN,
            ),
            eglBaseContext = null,
            onLocalRendererReady = {},
            onRemoteRendererReady = {},
            onBack = {},
            onMuteToggle = {},
            onSwitchCamera = {},
            onEndCall = {},
            onRejoin = {},
            onRequestPermissions = {},
        )
    }
}

@Preview(showBackground = true, name = "Menunggu petugas")
@Composable
private fun VideoCallMenungguPreview() {
    BcaMobileTheme {
        BukaRekeningVideoCallScreen(
            state = VideoCallUiState(
                connectionState = VideoCallConnectionState.MENUNGGU_PETUGAS,
            ),
            eglBaseContext = null,
            onLocalRendererReady = {},
            onRemoteRendererReady = {},
            onBack = {},
            onMuteToggle = {},
            onSwitchCamera = {},
            onEndCall = {},
            onRejoin = {},
            onRequestPermissions = {},
        )
    }
}

@Preview(showBackground = true, name = "Gagal, perlu antrean ulang")
@Composable
private fun VideoCallGagalPreview() {
    BcaMobileTheme {
        BukaRekeningVideoCallScreen(
            state = VideoCallUiState(connectionState = VideoCallConnectionState.GAGAL),
            eglBaseContext = null,
            onLocalRendererReady = {},
            onRemoteRendererReady = {},
            onBack = {},
            onMuteToggle = {},
            onSwitchCamera = {},
            onEndCall = {},
            onRejoin = {},
            onRequestPermissions = {},
        )
    }
}

@Preview(showBackground = true, name = "Bisu")
@Composable
private fun VideoCallMutedPreview() {
    BcaMobileTheme {
        BukaRekeningVideoCallScreen(
            state = VideoCallUiState(
                connectionState = VideoCallConnectionState.TERHUBUNG,
                namaPetugas = "Sarah Adisti",
                isMuted = true,
            ),
            eglBaseContext = null,
            onLocalRendererReady = {},
            onRemoteRendererReady = {},
            onBack = {},
            onMuteToggle = {},
            onSwitchCamera = {},
            onEndCall = {},
            onRejoin = {},
            onRequestPermissions = {},
        )
    }
}
