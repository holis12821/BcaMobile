package id.bca.bcamobile.ui.screen.buka_rekening.ringkasan

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.components.bottomBarSafePadding
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningLangkah
import id.bca.bcamobile.ui.screen.buka_rekening.common.StepProgressIndicator

// -- Data Model ---------------------------------------------------------------


// -- Main Screen --------------------------------------------------------------

@Composable
fun BukaRekeningRingkasanScreen(
    state: RingkasanUiState,
    onAgreementToggle: (Boolean) -> Unit,
    onUbahRekeningClick: () -> Unit,
    onUbahNasabahClick: () -> Unit,
    onProsesClick: () -> Unit,
    onSimpanDrafClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.buka_rekening_dp_title),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            RingkasanBottomBar(
                isEnabled = state.isAgreed,
                onProsesClick = onProsesClick,
                onSimpanDrafClick = onSimpanDrafClick,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            Spacer(Modifier.height(Spacing.s0))

            // Step progress
            StepProgressIndicator(
                langkah = BukaRekeningLangkah.RINGKASAN,
            )

            // Header banner
            RingkasanHeaderBanner()

            // Security banner
            RingkasanSecurityBanner()

            // Section 1: Pilihan Rekening
            PilihanRekeningSection(
                state = state,
                onUbahClick = onUbahRekeningClick,
            )

            // Section 2: Data Nasabah
            DataNasabahSection(
                state = state,
                onUbahClick = onUbahNasabahClick,
            )

            // Section 3: Status Verifikasi Identitas
            StatusVerifikasiSection(state = state)

            // Section 4: Persetujuan
            PersetujuanSection(
                isAgreed = state.isAgreed,
                onToggle = onAgreementToggle,
            )

            Spacer(Modifier.height(Spacing.s2))
        }
    }
}

// -- Header Banner ------------------------------------------------------------

@Composable
private fun RingkasanHeaderBanner(modifier: Modifier = Modifier) {
    Surface(
        shape = AppShape.R6,
        shadowElevation = Spacing.s0,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.s4)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(Spacing.s8)
                        .background(AppColor.Primary100, AppShape.R4),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_verified_user),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Spacing.s6),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.buka_rekening_ringkasan_heading),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Spacing.s1))
                    Text(
                        text = stringResource(R.string.buka_rekening_ringkasan_periksa_heading),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.s3))

            Text(
                text = stringResource(R.string.buka_rekening_ringkasan_periksa_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// -- Security Banner ----------------------------------------------------------

@Composable
private fun RingkasanSecurityBanner(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, AppShape.R6)
            .padding(Spacing.s3),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s7)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    AppShape.Full,
                ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(AppSize.IconSmall),
            )
        }
        Text(
            text = stringResource(R.string.buka_rekening_ringkasan_security_banner),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s6)
                .background(AppColor.Primary100, AppShape.R2),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shield),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Spacing.s3),
            )
        }
    }
}

// -- Section Header with Ubah -------------------------------------------------

@Composable
private fun RingkasanSectionHeader(
    title: String,
    iconRes: Int,
    onUbahClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s7)
                .background(AppColor.Primary100, AppShape.Full),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppSize.IconSmall),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (onUbahClick != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(AppShape.R4)
                    .clickable(onClick = onUbahClick)
                    .padding(horizontal = Spacing.s2, vertical = Spacing.s1),
            ) {
                Text(
                    text = stringResource(R.string.buka_rekening_ringkasan_ubah),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Spacing.s4),
                )
            }
        }
    }
}

// -- Data Field (label + value) -----------------------------------------------

@Composable
private fun DataField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

// -- Section 1: Pilihan Rekening ----------------------------------------------

@Composable
private fun PilihanRekeningSection(
    state: RingkasanUiState,
    onUbahClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        shadowElevation = Spacing.s0,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            modifier = Modifier.padding(Spacing.s4),
        ) {
            RingkasanSectionHeader(
                title = stringResource(R.string.buka_rekening_ringkasan_pilihan_rekening),
                iconRes = R.drawable.ic_account_balance_wallet,
                onUbahClick = onUbahClick,
            )

            // Product card
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary, AppShape.R4)
                    .padding(horizontal = Spacing.s3, vertical = Spacing.s3),
            ) {
                Text(
                    text = state.produkRekening,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                if (state.isPalingPopuler) {
                    Text(
                        text = stringResource(R.string.buka_rekening_paling_populer),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = AppColor.Success700,
                        modifier = Modifier
                            .background(AppColor.Success100, AppShape.Full)
                            .padding(horizontal = Spacing.s2, vertical = Spacing.s1),
                    )
                }
            }

            DataField(
                label = stringResource(R.string.buka_rekening_ringkasan_mata_uang_label),
                value = state.mataUang,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            DataField(
                label = stringResource(R.string.buka_rekening_ringkasan_setoran_label),
                value = state.setoranAwalMinimum,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            DataField(
                label = stringResource(R.string.buka_rekening_ringkasan_fasilitas_label),
                value = state.fasilitasDigital,
            )
        }
    }
}

// -- Section 2: Data Nasabah --------------------------------------------------

@Composable
private fun DataNasabahSection(
    state: RingkasanUiState,
    onUbahClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        shadowElevation = Spacing.s0,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            modifier = Modifier.padding(Spacing.s4),
        ) {
            RingkasanSectionHeader(
                title = stringResource(R.string.buka_rekening_ringkasan_section_nasabah),
                iconRes = R.drawable.ic_credit_card,
                onUbahClick = onUbahClick,
            )

            DataField(
                label = stringResource(R.string.buka_rekening_ringkasan_nama_label),
                value = state.namaLengkap,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            DataField(
                label = stringResource(R.string.buka_rekening_ringkasan_nik_label),
                value = state.nik,
                trailingContent = if (state.isNikVerified) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.ic_check_circle),
                            contentDescription = null,
                            tint = AppColor.Success500,
                            modifier = Modifier.size(Spacing.s5),
                        )
                    }
                } else null,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            DataField(
                label = stringResource(R.string.buka_rekening_ringkasan_tempat_ttl_label),
                value = state.tempatTanggalLahir,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            DataField(
                label = stringResource(R.string.buka_rekening_ringkasan_alamat_ktp_label),
                value = state.alamatKtp,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            DataField(
                label = stringResource(R.string.buka_rekening_ringkasan_pekerjaan_single_label),
                value = state.pekerjaan,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            DataField(
                label = stringResource(R.string.buka_rekening_ringkasan_hp_label),
                value = state.nomorHandphone,
                trailingContent = if (state.isOtpVerified) {
                    {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(AppColor.Success100, AppShape.Full)
                                .padding(horizontal = Spacing.s2, vertical = Spacing.s1),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_circle),
                                contentDescription = null,
                                tint = AppColor.Success700,
                                modifier = Modifier.size(Spacing.s3),
                            )
                            Text(
                                text = stringResource(R.string.buka_rekening_ringkasan_otp_terverifikasi),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = AppColor.Success700,
                            )
                        }
                    }
                } else null,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            DataField(
                label = stringResource(R.string.buka_rekening_ringkasan_email_label),
                value = state.alamatEmail,
            )
        }
    }
}

// -- Section 3: Status Verifikasi Identitas -----------------------------------

@Composable
private fun StatusVerifikasiSection(
    state: RingkasanUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        shadowElevation = Spacing.s0,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier.padding(Spacing.s4),
        ) {
            RingkasanSectionHeader(
                title = stringResource(R.string.buka_rekening_ringkasan_verifikasi_title),
                iconRes = R.drawable.ic_verified_user,
            )

            Text(
                text = stringResource(R.string.buka_rekening_ringkasan_verifikasi_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            VerifikasiItem(
                iconRes = R.drawable.ic_credit_card,
                title = stringResource(R.string.buka_rekening_ringkasan_foto_ektp),
                description = stringResource(R.string.buka_rekening_ringkasan_ocr_match),
                isVerified = state.isOcrVerified,
            )

            VerifikasiItem(
                iconRes = R.drawable.ic_face,
                title = stringResource(R.string.buka_rekening_ringkasan_biometrik),
                description = stringResource(R.string.buka_rekening_ringkasan_liveness_pass),
                isVerified = state.isBiometrikVerified,
            )

            VerifikasiItem(
                iconRes = R.drawable.ic_video_call,
                title = stringResource(R.string.buka_rekening_ringkasan_video_call),
                description = if (state.isVideoCallVerified && state.videoCallCsName.isNotEmpty()) {
                    stringResource(R.string.buka_rekening_ringkasan_vc_approved, state.videoCallCsName)
                } else {
                    stringResource(R.string.buka_rekening_ringkasan_belum_verifikasi)
                },
                isVerified = state.isVideoCallVerified,
            )
        }
    }
}

@Composable
private fun VerifikasiItem(
    iconRes: Int,
    title: String,
    description: String,
    isVerified: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isVerified) AppColor.Success100
                else MaterialTheme.colorScheme.surfaceContainerLow,
                AppShape.R4,
            )
            .padding(Spacing.s3),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s7)
                .background(
                    if (isVerified) AppColor.Success200
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                    AppShape.Full,
                ),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (isVerified) AppColor.Success700
                else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(AppSize.IconSmall),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s0),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = if (isVerified) AppColor.Success700
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            tint = if (isVerified) AppColor.Success700
            else MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.size(Spacing.s5),
        )
    }
}

// -- Section 4: Persetujuan ---------------------------------------------------

@Composable
private fun PersetujuanSection(
    isAgreed: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShape.R6)
            .background(
                if (isAgreed) AppColor.Primary100
                else MaterialTheme.colorScheme.surfaceContainerLow,
            )
            .clickable(role = Role.Checkbox) { onToggle(!isAgreed) }
            .padding(Spacing.s3),
    ) {
        Checkbox(
            checked = isAgreed,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
        Text(
            text = stringResource(R.string.buka_rekening_ringkasan_consent_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

// -- Bottom Bar ---------------------------------------------------------------

@Composable
private fun RingkasanBottomBar(
    isEnabled: Boolean,
    onProsesClick: () -> Unit,
    onSimpanDrafClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shadowElevation = Spacing.s1,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .bottomBarSafePadding()
                .padding(
                    horizontal = Spacing.s4,
                    vertical = Spacing.s3,
                ),
        ) {
            Button(
                onClick = onProsesClick,
                enabled = isEnabled,
                shape = AppShape.R6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSize.MinTouchTarget),
            ) {
                Text(
                    text = stringResource(R.string.buka_rekening_ringkasan_proses),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            Spacer(Modifier.height(Spacing.s2))

            OutlinedButton(
                onClick = onSimpanDrafClick,
                shape = AppShape.R6,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSize.MinTouchTarget),
            ) {
                Text(
                    text = stringResource(R.string.buka_rekening_ringkasan_simpan_draf),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            Spacer(Modifier.height(Spacing.s2))

            // OJK footer
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = AppColor.Success500,
                    modifier = Modifier.size(Spacing.s3),
                )
                Text(
                    text = stringResource(R.string.buka_rekening_ringkasan_ojk_footer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// -- Previews -----------------------------------------------------------------

private fun previewState() = RingkasanUiState(
    produkRekening = "Tahapan BCA",
    isPalingPopuler = true,
    mataUang = "IDR (Rupiah)",
    setoranAwalMinimum = "Rp 500.000",
    fasilitasDigital = "Paspor BCA Mastercard Debit, m-BCA & KlikBCA",
    namaLengkap = "MUHAMMAD ARDAN PRAYOGI",
    nik = "3174 0821 0495 0001",
    isNikVerified = true,
    tempatTanggalLahir = "Jakarta, 21 Apr 1995",
    alamatKtp = "Jl. Sudirman Kav. 45 No. 12B, Jakarta Selatan",
    pekerjaan = "Karyawan Swasta",
    nomorHandphone = "0812 \u2022\u2022\u2022\u2022 8889",
    isOtpVerified = true,
    alamatEmail = "m.ardan@example.com",
    isOcrVerified = true,
    isBiometrikVerified = true,
    isVideoCallVerified = true,
    videoCallCsName = "Sarah Adisti",
    isAgreed = true,
)

@Preview(showBackground = true)
@Composable
private fun RingkasanPreview() {
    BcaMobileTheme {
        BukaRekeningRingkasanScreen(
            state = previewState(),
            onAgreementToggle = {},
            onUbahRekeningClick = {},
            onUbahNasabahClick = {},
            onProsesClick = {},
            onSimpanDrafClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RingkasanDarkPreview() {
    BcaMobileTheme {
        BukaRekeningRingkasanScreen(
            state = previewState(),
            onAgreementToggle = {},
            onUbahRekeningClick = {},
            onUbahNasabahClick = {},
            onProsesClick = {},
            onSimpanDrafClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RingkasanUnagreedPreview() {
    BcaMobileTheme {
        BukaRekeningRingkasanScreen(
            state = previewState().copy(isAgreed = false),
            onAgreementToggle = {},
            onUbahRekeningClick = {},
            onUbahNasabahClick = {},
            onProsesClick = {},
            onSimpanDrafClick = {},
            onBackClick = {},
        )
    }
}