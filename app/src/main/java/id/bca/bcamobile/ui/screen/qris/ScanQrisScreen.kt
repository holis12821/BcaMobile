package id.bca.bcamobile.ui.screen.qris

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

data class ScanQrisUiState(
    val isFlashOn: Boolean = false,
    /** True selama `POST /qris/decode` berjalan; pemindaian ditahan agar tidak dobel. */
    val isDecoding: Boolean = false,
    val error: String? = null,
)

/**
 * Pemindai QRIS.
 *
 * Payload QR tidak diurai di sini — teks mentahnya dikirim ke server. Layar ini
 * hanya mengatur kamera, bingkai bidik, lampu kilat, dan pilihan gambar galeri.
 */
@Composable
fun ScanQrisScreen(
    state: ScanQrisUiState,
    onFlashToggle: () -> Unit,
    onGalleryClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Preview kamera langsung; kosong di @Preview dan saat izin kamera belum ada. */
    cameraPreview: (@Composable () -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.qris_scan_title),
                onBackClick = onBackClick,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.scrim),
        ) {
            cameraPreview?.invoke()

            // Peredup di atas preview supaya bingkai bidik dan teks tetap terbaca.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.scrim.copy(alpha = AppAlpha.A50),
                    ),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.s6),
            ) {
                ScannerFrame(isDecoding = state.isDecoding)

                Spacer(Modifier.height(Spacing.s8))

                Text(
                    text = state.error ?: stringResource(R.string.qris_scan_instruksi),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.error != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.scrim.copy(alpha = AppAlpha.A50),
                            AppShape.Full,
                        )
                        .padding(horizontal = Spacing.s4, vertical = Spacing.s2),
                )

                Spacer(Modifier.height(Spacing.s8))

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                    ScannerAction(
                        iconRes = R.drawable.ic_wb_incandescent,
                        label = stringResource(R.string.qris_scan_flash),
                        isActive = state.isFlashOn,
                        onClick = onFlashToggle,
                    )
                    ScannerAction(
                        iconRes = R.drawable.ic_photo_library,
                        label = stringResource(R.string.qris_scan_galeri),
                        isActive = false,
                        onClick = onGalleryClick,
                    )
                }
            }
        }
    }
}

/** Bingkai bidik dengan empat sudut kurung; ukurannya token `AppSize.ScannerFrame`. */
@Composable
private fun ScannerFrame(
    isDecoding: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(AppSize.ScannerFrame),
    ) {
        FrameCorner(alignment = Alignment.TopStart)
        FrameCorner(alignment = Alignment.TopEnd)
        FrameCorner(alignment = Alignment.BottomStart)
        FrameCorner(alignment = Alignment.BottomEnd)

        if (isDecoding) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun FrameCorner(
    alignment: Alignment,
    modifier: Modifier = Modifier,
) {
    val isTop = alignment == Alignment.TopStart || alignment == Alignment.TopEnd
    val isStart = alignment == Alignment.TopStart || alignment == Alignment.BottomStart
    val color = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.align(alignment).size(Spacing.s7)) {
            // Dua bilah membentuk sudut kurung: satu mendatar, satu menurun.
            Box(
                modifier = Modifier
                    .align(if (isTop) Alignment.TopStart else Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(StrokeWidth.w2)
                    .background(color),
            )
            Box(
                modifier = Modifier
                    .align(if (isStart) Alignment.TopStart else Alignment.TopEnd)
                    .width(StrokeWidth.w2)
                    .fillMaxHeight()
                    .background(color),
            )
        }
    }
}

@Composable
private fun ScannerAction(
    iconRes: Int,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier,
    ) {
        Surface(
            onClick = onClick,
            shape = AppShape.Full,
            color = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = AppAlpha.A30)
            },
            modifier = Modifier.size(Spacing.s10),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(AppSize.IconLarge),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun ScanQrisScreenPreview() {
    BcaMobileTheme {
        ScanQrisScreen(
            state = ScanQrisUiState(),
            onFlashToggle = {},
            onGalleryClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark")
@Composable
private fun ScanQrisScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        ScanQrisScreen(
            state = ScanQrisUiState(isFlashOn = true, isDecoding = true),
            onFlashToggle = {},
            onGalleryClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Gagal baca")
@Composable
private fun ScanQrisScreenErrorPreview() {
    BcaMobileTheme {
        ScanQrisScreen(
            state = ScanQrisUiState(error = stringResource(R.string.qris_error_qr_tidak_valid)),
            onFlashToggle = {},
            onGalleryClick = {},
            onBackClick = {},
        )
    }
}
