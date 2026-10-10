package id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_biometrik

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.core.liveness.LivenessFaceGuide
import id.bca.bcamobile.core.liveness.LivenessPhase
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.components.bottomBarSafePadding
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningLangkah
import id.bca.bcamobile.ui.screen.buka_rekening.common.StepProgressIndicator

// -- Data Model ---------------------------------------------------------------


// -- Main Screen ---------------------------------------------------------------

@Composable
fun BukaRekeningVerifikasiBiometrikScreen(
    state: VerifikasiBiometrikUiState,
    /**
     * Hanya dipakai setelah satu percobaan gagal. Tidak ada callback "mulai":
     * tantangan berjalan sendiri begitu gerbang kualitas wajah bertahan.
     */
    onRetryClick: () -> Unit,
    onTipsClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Preview kamera depan; kosong di @Preview dan saat izin kamera belum ada. */
    cameraPreview: (@Composable () -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.buka_rekening_biometrik_title),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            BottomCtaSection(
                state = state,
                onRetryClick = onRetryClick,
                onTipsClick = onTipsClick,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Step progress
            StepProgressIndicator(
                langkah = BukaRekeningLangkah.VERIFIKASI_BIOMETRIK,
                modifier = Modifier.padding(
                    start = Spacing.s4,
                    end = Spacing.s4,
                    top = Spacing.s4,
                ),
            )

            Spacer(Modifier.height(Spacing.s3))

            // Header text
            BiometrikHeader(
                modifier = Modifier.padding(horizontal = Spacing.s4),
            )

            Spacer(Modifier.height(Spacing.s3))

            // Biometric viewfinder
            BiometrikViewfinder(
                state = state,
                cameraPreview = cameraPreview,
                modifier = Modifier.padding(horizontal = Spacing.s4),
            )

            Spacer(Modifier.height(Spacing.s2))

            // Panduan pengambilan card
            PanduanPengambilanCard(
                modifier = Modifier.padding(horizontal = Spacing.s4),
            )

            Spacer(Modifier.height(Spacing.s2))

            // Security regulation card
            SecurityRegulationCard(
                modifier = Modifier.padding(horizontal = Spacing.s4),
            )

            Spacer(Modifier.height(Spacing.s3))
        }
    }
}

// -- Header -------------------------------------------------------------------

@Composable
private fun BiometrikHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.buka_rekening_biometrik_heading),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.s1))
        Text(
            text = stringResource(R.string.buka_rekening_biometrik_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// -- Biometric Viewfinder -----------------------------------------------------

@Composable
private fun BiometrikViewfinder(
    state: VerifikasiBiometrikUiState,
    modifier: Modifier = Modifier,
    cameraPreview: (@Composable () -> Unit)? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clip(AppShape.R7)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        // Kamera jadi lapisan paling bawah; scrim, oval, dan pill tetap di atasnya.
        cameraPreview?.invoke()

        // Scrim "punch-out": gelap di luar elips panduan, bening di dalamnya.
        //
        // Menggantikan gradient vertikal yang dulu menutup seluruh viewfinder rata.
        // Gradient itu tidak memberi tahu nasabah ke mana wajahnya harus ditaruh —
        // yang memberi tahu adalah kontras antara dalam dan luar elips. Digambar
        // satu path even-odd, bukan empat Box, supaya bentuknya pasti sama dengan
        // garis panduan di bawahnya.
        val scrimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.A50)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val guideWidth = size.width * LivenessFaceGuide.widthFraction
            val guideHeight = guideWidth * LivenessFaceGuide.heightToWidth
            val punchOut = Path().apply {
                addRect(Rect(Offset.Zero, size))
                addOval(
                    Rect(
                        left = center.x - guideWidth / 2f,
                        top = center.y - guideHeight / 2f,
                        right = center.x + guideWidth / 2f,
                        bottom = center.y + guideHeight / 2f,
                    ),
                )
                fillType = PathFillType.EvenOdd
            }
            drawPath(path = punchOut, color = scrimColor)
        }

        // Elips panduan — seukuran wajah yang benar-benar diterima gerbang kualitas.
        //
        // Ukurannya datang dari LivenessFaceGuide, yang menurunkannya dari ambang
        // gerbang. Jangan menggantinya dengan angka di sini: begitu panduan dan
        // gerbang punya dua sumber, keduanya menyimpang dan gejalanya adalah
        // nasabah yang mengisi panduan dengan benar lalu ditolak.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth(LivenessFaceGuide.widthFraction)
                .aspectRatio(1f / LivenessFaceGuide.heightToWidth)
                .clip(FaceGuideShape)
                .border(
                    width = StrokeWidth.w1,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.A80),
                    shape = FaceGuideShape,
                ),
        ) {
            // Sweep line ikut terpotong elips karena induknya sudah di-clip.
            SweepScanLine()
        }

        // Face detected status pill (top)
        if (state.isFaceReady) {
            FaceDetectedPill(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Spacing.s7),
            )
        }

        // Instruction card (bottom inside viewfinder)
        LivenessInstructionCard(
            state = state,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = Spacing.s4,
                    end = Spacing.s4,
                    bottom = Spacing.s4,
                ),
        )
    }
}

// -- Face Guide Shape ---------------------------------------------------------

/**
 * Elips sungguhan, mengikuti kotak yang diberikan padanya.
 *
 * [AppShape.Full] adalah `CircleShape`, dan `CircleShape` di atas kotak
 * non-persegi berubah jadi **pill**, bukan oval — radiusnya terbatas sisi
 * terpendek. Itu sumber keluhan "frame wajahnya terlalu lebar": panduan lamanya
 * pill selebar hampir seluruh viewfinder, yang tidak menyerupai kepala dan tidak
 * menunjukkan ukuran yang diterima gerbang.
 */
private val FaceGuideShape = GenericShape { size, _ ->
    addOval(Rect(Offset.Zero, size))
}

// -- Sweep Scan Line ----------------------------------------------------------

@Composable
private fun SweepScanLine(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scan")
    val offsetY by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanOffset",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = Spacing.s10 * offsetY)
            .height(Spacing.s0)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                        AppColor.Primary100.copy(alpha = AppAlpha.A70),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                    ),
                ),
            ),
    )
}

// -- Face Detected Pill -------------------------------------------------------

/**
 * Pil status wajah.
 *
 * Tidak lagi menampilkan persentase: angka yang dulu tampil di sini adalah rata-rata
 * probabilitas mata terbuka dari ML Kit, diberi label "Presisi". Yang bisa dinyatakan
 * dengan jujur dari gerbang kualitas hanyalah bahwa wajahnya sudah siap.
 */
@Composable
private fun FaceDetectedPill(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = AppAlpha.A90),
                AppShape.Full,
            )
            .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(Spacing.s4),
        )
        Text(
            text = stringResource(R.string.buka_rekening_biometrik_face_ready),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

// -- Liveness Instruction Card ------------------------------------------------

/**
 * Kartu instruksi yang mengikuti langkah yang sedang diminta.
 *
 * Sebelumnya kartu ini memajang satu teks tetap ("Kedipkan Kedua Mata Anda") sepanjang
 * sesi, apa pun gerakan yang sedang dinilai — nasabah tidak punya cara tahu apa yang
 * diminta. Isinya sekarang: instruksi gerakan kalau tantangan berjalan, panduan posisi
 * kalau gerbang kualitas belum terpenuhi.
 *
 * `liveRegion` membuat TalkBack membacakan perubahan instruksi tanpa fokus dipindahkan.
 * Tanpa itu, seluruh tantangan ini tidak bisa diselesaikan dengan pembaca layar aktif.
 */
@Composable
private fun LivenessInstructionCard(
    state: VerifikasiBiometrikUiState,
    modifier: Modifier = Modifier,
) {
    val headlineRes = state.instructionRes
        ?: state.guidanceRes
        ?: R.string.buka_rekening_biometrik_guide_ready
    val headline = stringResource(headlineRes)
    // stringResource tidak boleh dipanggil di dalam lambda `semantics`, yang bukan
    // konteks composable; nilainya dihitung di sini lalu ikut sebagai tangkapan.
    val progressDescription = progressLabel(state)

    Surface(
        shape = AppShape.R6,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = AppAlpha.A90),
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            modifier = Modifier.padding(Spacing.s3),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(Spacing.s7)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.A10),
                            AppShape.Full,
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_visibility),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppSize.IconSmall),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        // Instruksi gerakan bisa lebih panjang dari satu baris dan
                        // memotongnya berarti menyembunyikan apa yang harus dilakukan.
                        maxLines = 2,
                    )
                    if (state.totalSteps > 0) {
                        Text(
                            text = stringResource(
                                R.string.buka_rekening_biometrik_liveness_progress,
                                state.completedSteps,
                                state.totalSteps,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = {
                    if (state.totalSteps > 0) {
                        state.completedSteps.toFloat() / state.totalSteps.toFloat()
                    } else {
                        0f
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.s1)
                    .clip(AppShape.Full)
                    .semantics { contentDescription = progressDescription },
                color = MaterialTheme.colorScheme.primaryContainer,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        }
    }
}

// -- Panduan Pengambilan Card -------------------------------------------------

@Composable
private fun PanduanPengambilanCard(modifier: Modifier = Modifier) {
    Surface(
        shape = AppShape.R7,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier.padding(Spacing.s4),
        ) {
            // Title row
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_verified_user),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Spacing.s5),
                )
                Text(
                    text = stringResource(R.string.buka_rekening_biometrik_panduan_title),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Checklist items
            PanduanItem(
                text = stringResource(R.string.buka_rekening_biometrik_panduan_1),
            )
            PanduanItem(
                text = stringResource(R.string.buka_rekening_biometrik_panduan_2),
            )
            PanduanItem(
                text = stringResource(R.string.buka_rekening_biometrik_panduan_3),
            )
        }
    }
}

@Composable
private fun PanduanItem(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.Top,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s5)
                .background(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = AppAlpha.A10),
                    AppShape.Full,
                ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(Spacing.s3),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

// -- Security Regulation Card -------------------------------------------------

@Composable
private fun SecurityRegulationCard(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(
                AppColor.Secondary100.copy(alpha = AppAlpha.A30),
                AppShape.R7,
            )
            .padding(Spacing.s3),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s8)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.A10),
                    AppShape.Full,
                ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_security),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Spacing.s5),
            )
        }
        Text(
            text = stringResource(R.string.buka_rekening_biometrik_security),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

// -- Bottom CTA Section -------------------------------------------------------

/**
 * Bar bawah: status, bukan pemicu.
 *
 * Tombol "Mulai Perekaman Biometrik" sudah dihapus. Semua yang dibutuhkan untuk
 * memutuskan kapan tantangan boleh dimulai — wajah tunggal, di dalam oval, lurus,
 * mata terbuka — sudah terbaca dari frame kamera, jadi menuntut satu ketukan lagi
 * hanya menambah langkah tanpa menambah informasi.
 *
 * Yang tersisa di sini: keterangan apa yang sedang terjadi, dan tombol Coba Lagi yang
 * **hanya** muncul setelah percobaan gagal dan masa tunggu server sudah habis.
 */
@Composable
private fun BottomCtaSection(
    state: VerifikasiBiometrikUiState,
    onRetryClick: () -> Unit,
    onTipsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shadowElevation = Spacing.s1,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .bottomBarSafePadding()
                .padding(
                    horizontal = Spacing.s4,
                    vertical = Spacing.s3,
                ),
        ) {
            StatusLine(state)

            if (state.canRetry) {
                Button(
                    onClick = onRetryClick,
                    shape = AppShape.R6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppSize.MinTouchTarget),
                ) {
                    Text(
                        text = stringResource(R.string.buka_rekening_biometrik_retry),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.width(Spacing.s2))
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = null,
                        modifier = Modifier.size(Spacing.s5),
                    )
                }
            }

            TextButton(
                onClick = onTipsClick,
                shape = AppShape.R6,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_help_outline),
                    contentDescription = null,
                    modifier = Modifier.size(AppSize.IconSmall),
                )
                Spacer(Modifier.width(Spacing.s1))
                Text(
                    text = stringResource(R.string.buka_rekening_biometrik_tips),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/**
 * Satu baris yang menjelaskan keadaan sekarang.
 *
 * Urutan cabangnya penting: blokir 24 jam dan masa tunggu harus menang atas teks
 * kegagalan, karena keduanya menjelaskan **kenapa tombol Coba Lagi tidak ada** —
 * pertanyaan yang muncul lebih dulu di kepala nasabah daripada gerakan mana yang gagal.
 */
@Composable
private fun StatusLine(
    state: VerifikasiBiometrikUiState,
    modifier: Modifier = Modifier,
) {
    val text = when {
        state.isBlocked -> stringResource(R.string.buka_rekening_biometrik_blocked)
        state.cooldownSeconds > 0 -> stringResource(
            R.string.buka_rekening_biometrik_cooldown,
            state.cooldownSeconds,
        )
        state.isVerifying -> stringResource(R.string.buka_rekening_biometrik_status_verifying)
        state.phase == LivenessPhase.SUCCESS ->
            stringResource(R.string.buka_rekening_biometrik_status_success)
        state.failureRes != null -> stringResource(state.failureRes)
        state.isRunning -> stringResource(R.string.buka_rekening_biometrik_status_preparing)
        else -> return
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Dibaca TalkBack sebagai ganti bilah progres yang tidak punya label. */
@Composable
private fun progressLabel(state: VerifikasiBiometrikUiState): String = stringResource(
    R.string.buka_rekening_biometrik_a11y_progress,
    state.completedSteps,
    state.totalSteps,
)

// -- Preview ------------------------------------------------------------------

@Preview(showBackground = true, name = "Tantangan berjalan")
@Composable
private fun VerifikasiBiometrikPreview() {
    BcaMobileTheme {
        BukaRekeningVerifikasiBiometrikScreen(
            state = VerifikasiBiometrikUiState(
                phase = LivenessPhase.CHALLENGE,
                isFaceReady = true,
                instructionRes = R.string.buka_rekening_biometrik_action_turn_left,
                completedSteps = 1,
                totalSteps = 3,
            ),
            onRetryClick = {},
            onTipsClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Belum siap")
@Composable
private fun VerifikasiBiometrikPositioningPreview() {
    BcaMobileTheme {
        BukaRekeningVerifikasiBiometrikScreen(
            state = VerifikasiBiometrikUiState(
                phase = LivenessPhase.POSITIONING,
                guidanceRes = R.string.buka_rekening_biometrik_guide_move_closer,
            ),
            onRetryClick = {},
            onTipsClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Gagal, masa tunggu")
@Composable
private fun VerifikasiBiometrikCooldownPreview() {
    BcaMobileTheme {
        BukaRekeningVerifikasiBiometrikScreen(
            state = VerifikasiBiometrikUiState(
                phase = LivenessPhase.FAILED,
                failureRes = R.string.buka_rekening_biometrik_fail_wrong_move,
                cooldownSeconds = 284,
            ),
            onRetryClick = {},
            onTipsClick = {},
            onBackClick = {},
        )
    }
}
