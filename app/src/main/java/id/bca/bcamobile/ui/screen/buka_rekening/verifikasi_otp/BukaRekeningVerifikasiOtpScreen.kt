package id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_otp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.focus.FocusRequester
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningLangkah
import id.bca.bcamobile.ui.screen.buka_rekening.common.OtpDigitBoxes
import id.bca.bcamobile.ui.screen.buka_rekening.common.StepProgressIndicator

/** Panjang kode OTP sesuai kontrak `POST /onboarding/verify-otp`. */
const val OTP_LENGTH = 6


@Composable
fun BukaRekeningVerifikasiOtpScreen(
    state: BukaRekeningOtpUiState,
    onKodeChange: (String) -> Unit,
    onVerifikasiClick: () -> Unit,
    onKirimUlangClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        // enableEdgeToEdge() di MainActivity membuat window tidak lagi menyusut saat
        // papan ketik muncul, jadi windowSoftInputMode="adjustResize" di manifest tidak
        // berpengaruh dan IME harus diambil dari sini. safeDrawing = gabungan systemBars,
        // ime, dan displayCutout per sisi dengan nilai terbesar: tanpa bottomBar, Scaffold
        // meneruskannya sebagai innerPadding.bottom, dan karena padding itu dipasang pada
        // container gulir, area gulir menyusut dan seluruh isi layar tetap terjangkau.
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.buka_rekening_otp_title),
                onBackClick = onBackClick,
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
            StepProgressIndicator(
                langkah = BukaRekeningLangkah.VERIFIKASI_OTP,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.s4, vertical = Spacing.s3),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.s6),
                modifier = Modifier.padding(
                    start = Spacing.s4,
                    end = Spacing.s4,
                    top = Spacing.s5,
                    bottom = Spacing.s6,
                ),
            ) {
                OtpInfoCard(nomorTersamar = state.nomorTersamar)

                OtpInputSection(
                    state = state,
                    onKodeChange = onKodeChange,
                    onVerifikasiClick = onVerifikasiClick,
                    onKirimUlangClick = onKirimUlangClick,
                )

                OtpSecurityNotice()

                OtpActionSection(
                    isEnabled = state.isVerifikasiAktif,
                    isLoading = state.isLoading,
                    onVerifikasiClick = onVerifikasiClick,
                )
            }
        }
    }
}

/** Kartu pembuka: ikon SMS, judul, penjelasan, dan nomor tujuan yang tersamar. */
@Composable
private fun OtpInfoCard(
    nomorTersamar: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, AppShape.R6)
            .padding(Spacing.s5),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s10)
                .background(MaterialTheme.colorScheme.secondaryFixed, AppShape.Full),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sms),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppSize.Icon32),
            )
        }

        Spacer(Modifier.size(Spacing.s3))

        Text(
            text = stringResource(R.string.buka_rekening_otp_heading),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.size(Spacing.s2))

        Text(
            text = stringResource(R.string.buka_rekening_otp_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.size(Spacing.s2))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer, AppShape.Full)
                .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_smartphone),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppSize.Icon16),
            )
            Text(
                text = nomorTersamar,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Kotak digit, hitung mundur, dan tombol kirim ulang.
 *
 * Input sesungguhnya adalah satu [BasicTextField] tanpa tampilan; kotak digit
 * dipasang sebagai `decorationBox`. Dengan begitu keyboard angka, tempel, dan
 * autofill SMS tetap bekerja, tanpa mengelola fokus enam field terpisah.
 */
@Composable
private fun OtpInputSection(
    state: BukaRekeningOtpUiState,
    onKodeChange: (String) -> Unit,
    onVerifikasiClick: () -> Unit,
    onKirimUlangClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(state.isInputAktif) {
        if (state.isInputAktif) focusRequester.requestFocus()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        BasicTextField(
            value = state.kode,
            onValueChange = { input ->
                val bersih = input.filter(Char::isDigit).take(OTP_LENGTH)
                if (bersih != state.kode) onKodeChange(bersih)
            },
            enabled = state.isInputAktif,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (state.isVerifikasiAktif) onVerifikasiClick()
                },
            ),
            // Teks aslinya tidak digambar — yang tampil adalah kotak digit.
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            decorationBox = {
                OtpDigitBoxes(
                    code = state.kode,
                    length = OTP_LENGTH,
                    isFocused = isFocused,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
        )

        if (state.error != null) {
            Spacer(Modifier.size(Spacing.s3))
            Text(
                text = state.error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.size(Spacing.s5))

        if (!state.isKirimUlangAktif) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_schedule),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(AppSize.IconSmall),
                )
                Text(
                    text = stringResource(R.string.buka_rekening_otp_kirim_ulang_dalam),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.buka_rekening_otp_hitung_mundur_format,
                        state.detikTersisa / DETIK_PER_MENIT,
                        state.detikTersisa % DETIK_PER_MENIT,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        TextButton(
            onClick = onKirimUlangClick,
            enabled = state.isKirimUlangAktif,
            modifier = Modifier.heightIn(min = AppSize.MinTouchTarget),
        ) {
            Text(
                text = stringResource(R.string.buka_rekening_otp_kirim_ulang),
                style = MaterialTheme.typography.labelMedium,
                color = if (state.isKirimUlangAktif) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
        }
    }
}

/** Peringatan anti-penipuan; wajib tampil, bukan hiasan. */
@Composable
private fun OtpSecurityNotice(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer, AppShape.R6)
            .padding(Spacing.s4),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s7)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, AppShape.Full),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_security),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppSize.Icon20),
            )
        }
        Column {
            Text(
                text = stringResource(R.string.buka_rekening_otp_keamanan_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.size(Spacing.s0))
            Text(
                text = stringResource(R.string.buka_rekening_otp_keamanan_pesan),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

/** Tombol utama dan catatan enkripsi di bawahnya. */
@Composable
private fun OtpActionSection(
    isEnabled: Boolean,
    isLoading: Boolean,
    onVerifikasiClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        Button(
            onClick = onVerifikasiClick,
            enabled = isEnabled,
            shape = AppShape.R6,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AppSize.MinTouchTarget),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(AppSize.Icon20),
                )
            } else {
                Text(
                    text = stringResource(R.string.buka_rekening_otp_verifikasi),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(Spacing.s2))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
                    modifier = Modifier.size(AppSize.Icon20),
                )
            }
        }

        Spacer(Modifier.size(Spacing.s3))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(AppSize.Icon16),
            )
            Text(
                text = stringResource(R.string.buka_rekening_otp_enkripsi),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val DETIK_PER_MENIT = 60

private val previewState = BukaRekeningOtpUiState(
    nomorTersamar = "0812 •••• 8891",
    kode = "481",
    detikTersisa = 48,
)

@Preview(showBackground = true, name = "Light")
@Composable
private fun BukaRekeningVerifikasiOtpScreenPreview() {
    BcaMobileTheme {
        BukaRekeningVerifikasiOtpScreen(
            state = previewState,
            onKodeChange = {},
            onVerifikasiClick = {},
            onKirimUlangClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark")
@Composable
private fun BukaRekeningVerifikasiOtpScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        BukaRekeningVerifikasiOtpScreen(
            state = previewState,
            onKodeChange = {},
            onVerifikasiClick = {},
            onKirimUlangClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Terblokir")
@Composable
private fun BukaRekeningVerifikasiOtpScreenBlockedPreview() {
    BcaMobileTheme {
        BukaRekeningVerifikasiOtpScreen(
            state = previewState.copy(
                kode = "",
                detikTersisa = 1_800,
                isInputDiblokir = true,
                error = stringResource(R.string.buka_rekening_error_otp_blocked, 1_800),
            ),
            onKodeChange = {},
            onVerifikasiClick = {},
            onKirimUlangClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Error + kirim ulang aktif")
@Composable
private fun BukaRekeningVerifikasiOtpScreenErrorPreview() {
    BcaMobileTheme {
        BukaRekeningVerifikasiOtpScreen(
            state = previewState.copy(
                kode = "481920",
                detikTersisa = 0,
                error = stringResource(R.string.buka_rekening_error_otp_invalid),
            ),
            onKodeChange = {},
            onVerifikasiClick = {},
            onKirimUlangClick = {},
            onBackClick = {},
        )
    }
}
