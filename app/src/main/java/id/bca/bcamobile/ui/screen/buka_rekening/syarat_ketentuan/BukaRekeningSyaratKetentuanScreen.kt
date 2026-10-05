package id.bca.bcamobile.ui.screen.buka_rekening.syarat_ketentuan

import androidx.annotation.DrawableRes
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.domain.onboarding.model.TncConsent
import id.bca.bcamobile.domain.onboarding.model.TncDocument
import id.bca.bcamobile.domain.onboarding.model.TncNotice
import id.bca.bcamobile.domain.onboarding.model.TncSection
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.components.bottomBarSafePadding
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningLangkah
import id.bca.bcamobile.ui.screen.buka_rekening.common.StepProgressIndicator
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// -- Main Screen ---------------------------------------------------------------

@Composable
fun BukaRekeningSyaratKetentuanScreen(
    state: BukaRekeningSyaratKetentuanUiState,
    onAgreeClick: () -> Unit,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Dikunci ke versi dokumen, bukan `remember {}` kosong: begitu server mengirim versi
    // lain, centangnya lahir ulang kosong dengan sendirinya. Itu wajib — nasabah yang
    // sudah mencentang lalu teksnya berganti di bawahnya belum menyetujui apa pun, dan
    // membiarkan centang itu hidup mengulangi persis kerusakan yang `409` tolak.
    var isChecked by remember(state.document?.version) { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.buka_rekening_baru_title),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = Spacing.s1,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .bottomBarSafePadding()
                        .padding(Spacing.s4),
                ) {
                    // Kegagalan `POST sessions` dulu tidak tampil di mana pun: state-nya
                    // ada, pembacanya tidak. Tempatnya di sini, dekat tombol penyebabnya.
                    if (state.error != null) {
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(Spacing.s2))
                    }
                    AgreeButton(
                        label = state.document?.agreeCta,
                        enabled = isChecked && state.isAgreeAllowed,
                        onClick = onAgreeClick,
                    )
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Progres langkah bukan isi S&K, jadi tetap tampil di ketiga keadaan.
            StepProgressIndicator(
                langkah = BukaRekeningLangkah.SYARAT_KETENTUAN,
                modifier = Modifier.padding(
                    start = Spacing.s4,
                    end = Spacing.s4,
                    top = Spacing.s2,
                    bottom = Spacing.s4,
                ),
            )

            val document = state.document
            when {
                // Dokumen menang atas penanda muat: saat memuat ulang setelah `409`, teks
                // lama tetap terbaca sampai yang baru datang, bukan berkedip jadi kosong.
                document != null -> TncContent(
                    document = document,
                    isChecked = isChecked,
                    onCheckedChange = { isChecked = it },
                )

                state.documentError != null -> TncErrorState(
                    message = state.documentError,
                    onRetryClick = onRetryClick,
                )

                else -> TncLoadingState()
            }
        }
    }
}

// -- Keadaan muat & gagal ------------------------------------------------------

@Composable
private fun TncLoadingState(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.s8),
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun TncErrorState(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.s4),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.s4))
        TextButton(onClick = onRetryClick) {
            Text(
                text = stringResource(R.string.buka_rekening_coba_lagi),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

// -- Isi dokumen ---------------------------------------------------------------

@Composable
private fun TncContent(
    document: TncDocument,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier.padding(
                start = Spacing.s4,
                end = Spacing.s4,
                bottom = Spacing.s4,
            ),
        ) {
            Text(
                text = document.heading,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.s1))
            Text(
                text = document.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Bagian yang tidak dikirim server disembunyikan, bukan diganti teks lokal:
        // menolak seluruh dokumen karena banner hilang jauh lebih merugikan.
        if (document.trustTitle.isNotBlank() || document.trustSubtitle.isNotBlank()) {
            TrustBanner(
                title = document.trustTitle,
                subtitle = document.trustSubtitle,
                modifier = Modifier.padding(horizontal = Spacing.s4),
            )
            Spacer(Modifier.height(Spacing.s4))
        }

        TncContainer(
            sections = document.sections,
            modifier = Modifier.padding(horizontal = Spacing.s4),
        )

        Spacer(Modifier.height(Spacing.s4))

        Column(modifier = Modifier.padding(horizontal = Spacing.s4)) {
            document.notice?.let { notice ->
                PentingNoticeBox(notice = notice)
                Spacer(Modifier.height(Spacing.s4))
            }
            document.consent?.let { consent ->
                AgreementCheckbox(
                    consent = consent,
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                )
                Spacer(Modifier.height(Spacing.s4))
            }
        }
    }
}

// -- Trust Banner -------------------------------------------------------------

@Composable
private fun TrustBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
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
                .size(Spacing.s9)
                .background(AppColor.Primary100, AppShape.R4),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_verified_user),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Spacing.s6),
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// -- Daftar pasal --------------------------------------------------------------

@Composable
private fun TncContainer(
    sections: List<TncSection>,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        shadowElevation = Spacing.s0,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.s4),
        ) {
            // Urutannya sudah benar dari server — jangan disortir ulang di sini.
            sections.forEachIndexed { index, section ->
                TncSectionItem(section = section)
                if (index < sections.lastIndex) {
                    HorizontalDivider(
                        thickness = StrokeWidth.w0,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.padding(vertical = Spacing.s4),
                    )
                }
            }
        }
    }
}

/**
 * Kunci ikon dari server ke drawable lokal.
 *
 * Cabang `else` bukan kerapian: backend bisa menambah pasal keenam dengan `icon_key` baru
 * lewat SQL tanpa rilis aplikasi — itu justru tujuan seluruh perubahan ini. `when` tanpa
 * `else` yang melempar, atau `mapOf(...)[key]!!`, akan mengubah penambahan pasal jadi
 * crash di layar pertama buka rekening. Teks hukum yang tidak tampil jauh lebih buruk
 * daripada ikon yang kurang tepat.
 */
@DrawableRes
private fun tncIconRes(iconKey: String): Int = when (iconKey) {
    "ACCOUNT_BOX" -> R.drawable.ic_account_box
    "VERIFIED_USER" -> R.drawable.ic_verified_user
    "VIDEO_CALL" -> R.drawable.ic_video_call
    "SAVINGS" -> R.drawable.ic_savings
    "LOCK" -> R.drawable.ic_lock
    else -> R.drawable.ic_info
}

@Composable
private fun TncSectionItem(section: TncSection) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(tncIconRes(section.iconKey)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppSize.IconSmall),
            )
            Text(
                text = section.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = section.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.s6),
        )
    }
}

// -- Kotak PENTING -------------------------------------------------------------

@Composable
private fun PentingNoticeBox(
    notice: TncNotice,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .background(AppColor.Primary100, AppShape.R6)
            .padding(Spacing.s3),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s7)
                .background(MaterialTheme.colorScheme.primary, AppShape.R4),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_security),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(AppSize.IconSmall),
            )
        }
        Column {
            Text(
                text = notice.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = notice.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

// -- Checkbox persetujuan ------------------------------------------------------

@Composable
private fun AgreementCheckbox(
    consent: TncConsent,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        shadowElevation = Spacing.s0,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShape.R6)
                .clickable { onCheckedChange(!checked) }
                .padding(Spacing.s3),
        ) {
            val checkboxBg = if (checked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Spacing.s5)
                    .background(checkboxBg, AppShape.R2),
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(Spacing.s4),
                    )
                }
            }

            // Tiga potongan dari server, bukan satu kalimat yang dicari substring-nya:
            // substring itu pecah pada setiap perbaikan kata di sisi server.
            Text(
                text = buildAnnotatedString {
                    append(consent.prefix)
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    ) {
                        append(consent.link)
                    }
                    append(consent.suffix)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// -- Tombol setuju -------------------------------------------------------------

@Composable
private fun AgreeButton(
    label: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = AppShape.R6,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.s9),
    ) {
        Text(
            // Satu-satunya teks S&K yang masih punya cadangan lokal: tombolnya tetap
            // perlu label saat dokumen belum ada, walau keadaannya mati.
            text = label?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.buka_rekening_sk_setuju),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.size(Spacing.s2))
        Icon(
            painter = painterResource(R.drawable.ic_arrow_forward),
            contentDescription = null,
            modifier = Modifier.size(AppSize.IconSmall),
        )
    }
}

// -- Previews ------------------------------------------------------------------

private val previewDocument = TncDocument(
    version = "2026-09-01",
    heading = "Syarat & Ketentuan Pembukaan Rekening",
    subtitle = "Mohon baca dan pahami syarat dan ketentuan pembukaan rekening digital BCA " +
        "sebelum melanjutkan.",
    trustTitle = "Persetujuan Resmi Nasabah",
    trustSubtitle = "Terdaftar dan diawasi oleh Otoritas Jasa Keuangan (OJK)",
    sections = listOf(
        TncSection(
            iconKey = "ACCOUNT_BOX",
            title = "1. Ketentuan Umum Pembukaan Rekening Digital",
            body = "Calon nasabah merupakan Warga Negara Indonesia (WNI) dengan usia " +
                "minimal 17 tahun dan memiliki e-KTP fisik asli yang masih berlaku.",
        ),
        TncSection(
            iconKey = "LOCK",
            title = "2. Penggunaan Fasilitas m-BCA",
            body = "Nasabah bertanggung jawab penuh menjaga kerahasiaan Kode Akses, PIN " +
                "transaksi, dan kode OTP.",
        ),
        // Kunci yang belum dikenal aplikasi: pasalnya tetap tampil dengan ikon cadangan.
        TncSection(
            iconKey = "KUNCI_BARU_DARI_SERVER",
            title = "3. Pasal yang ditambahkan server",
            body = "Ikonnya jatuh ke cadangan, teksnya tetap terbaca.",
        ),
    ),
    notice = TncNotice(
        label = "PENTING",
        body = "Pastikan Anda berada di tempat tenang dan pencahayaan cukup untuk " +
            "verifikasi video call pada tahap berikutnya.",
    ),
    consent = TncConsent(
        prefix = "Saya telah membaca, memahami, dan menyetujui seluruh ",
        link = "Syarat & Ketentuan Pembukaan Rekening BCA",
        suffix = ".",
    ),
    agreeCta = "Setuju & Lanjutkan",
    effectiveFrom = "2026-09-01T00:00:00+07:00",
    isActive = true,
)

@Preview(showBackground = true, name = "Berhasil muat")
@Composable
private fun BukaRekeningSyaratKetentuanPreview() {
    BcaMobileTheme {
        BukaRekeningSyaratKetentuanScreen(
            state = BukaRekeningSyaratKetentuanUiState(document = previewDocument),
            onAgreeClick = {},
            onRetryClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Sedang memuat")
@Composable
private fun BukaRekeningSyaratKetentuanLoadingPreview() {
    BcaMobileTheme {
        BukaRekeningSyaratKetentuanScreen(
            state = BukaRekeningSyaratKetentuanUiState(isLoadingDocument = true),
            onAgreeClick = {},
            onRetryClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Gagal muat")
@Composable
private fun BukaRekeningSyaratKetentuanErrorPreview() {
    BcaMobileTheme {
        BukaRekeningSyaratKetentuanScreen(
            state = BukaRekeningSyaratKetentuanUiState(
                documentError = stringResource(R.string.buka_rekening_error_network),
            ),
            onAgreeClick = {},
            onRetryClick = {},
            onBackClick = {},
        )
    }
}
