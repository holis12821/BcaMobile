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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// -- Data Model ---------------------------------------------------------------


// -- Main Screen ---------------------------------------------------------------

@Composable
fun BukaRekeningVideoCallScreen(
    state: VideoCallUiState,
    onBack: () -> Unit,
    onMuteToggle: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEndCall: () -> Unit,
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
            // Layer 1: Gradient overlays (top & bottom darkening)
            VideoGradientOverlay()

            // Layer 2: Top status overlay
            TopStatusOverlay(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart),
            )

            // Layer 3: Center verification reticle
            VerificationReticle(
                modifier = Modifier.align(Alignment.Center),
            )

            // Layer 4: Bottom section (instruction, PiP, controls, POJK)
            BottomSection(
                state = state,
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
            TimerBadge(durasiPanggilan = state.durasiPanggilan)
        }

        // Row 2: Agent info tag
        AgentInfoTag(
            namaPetugas = state.namaPetugas,
            statusTerhubung = state.statusTerhubung,
        )
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
            PipSelfView()
        }

        // Floating call controls bar
        FloatingControlsBar(
            isMuted = state.isMuted,
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
private fun PipSelfView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(AppSize.LogoContainer)
            .aspectRatio(3f / 4f)
            .clip(AppShape.R6)
            .background(AppColor.Neutral700)
            .border(StrokeWidth.w0, AppColor.Neutral100.copy(alpha = AppAlpha.A30), AppShape.R6),
    ) {
        // Placeholder icon
        Icon(
            painter = painterResource(R.drawable.ic_face),
            contentDescription = null,
            tint = AppColor.Neutral500,
            modifier = Modifier
                .size(Spacing.s7)
                .align(Alignment.Center),
        )

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

        // End call
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

@Preview(showBackground = true)
@Composable
private fun VideoCallVerifikasiPreview() {
    BcaMobileTheme {
        BukaRekeningVideoCallScreen(
            state = VideoCallUiState(
                instruksiPetugas = "Mohon posisikan fisik e-KTP Anda di depan kamera samping wajah.",
            ),
            onBack = {},
            onMuteToggle = {},
            onSwitchCamera = {},
            onEndCall = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VideoCallMutedPreview() {
    BcaMobileTheme {
        BukaRekeningVideoCallScreen(
            state = VideoCallUiState(isMuted = true),
            onBack = {},
            onMuteToggle = {},
            onSwitchCamera = {},
            onEndCall = {},
        )
    }
}