package id.bca.bcamobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.Spacing

/**
 * Pengganti preview kamera saat izin belum diberikan.
 *
 * Mengisi area bingkai yang sama dengan preview, jadi tata letak layar tidak
 * bergeser antara keadaan berizin dan tidak.
 *
 * @param isPermanentlyDenied true setelah pengguna menolak — tombolnya mengarah
 *   ke Pengaturan karena dialog izin tidak akan muncul lagi.
 */
@Composable
fun CameraPermissionNotice(
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    isPermanentlyDenied: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s3, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.scrim.copy(alpha = AppAlpha.A70),
            )
            .padding(Spacing.s4),
    ) {
        Text(
            text = stringResource(R.string.buka_rekening_izin_kamera_judul),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                if (isPermanentlyDenied) {
                    R.string.buka_rekening_izin_kamera_pesan_pengaturan
                } else {
                    R.string.buka_rekening_izin_kamera_pesan
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = AppAlpha.A80),
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAction) {
            Text(
                text = stringResource(
                    if (isPermanentlyDenied) {
                        R.string.buka_rekening_izin_kamera_pengaturan
                    } else {
                        R.string.buka_rekening_izin_kamera_beri
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
