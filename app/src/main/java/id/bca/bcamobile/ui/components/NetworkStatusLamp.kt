package id.bca.bcamobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.core.network.NetworkStatus
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

/**
 * Warna lampu untuk satu [NetworkStatus].
 *
 * Dipisah dari composable-nya supaya indikator hijau yang sudah ada di tombol
 * mBCA bisa memakai sumber warna yang **sama**. Dua indikator di satu layar yang
 * mengambil warna dari dua tempat akan menyimpang, dan yang terlihat nasabah
 * adalah dua lampu berbeda warna untuk satu keadaan.
 */
@Composable
fun networkStatusColor(status: NetworkStatus): Color = when (status) {
    NetworkStatus.ONLINE -> AppColor.Success500
    NetworkStatus.DEGRADED -> AppColor.Warning600
    NetworkStatus.OFFLINE -> AppColor.Danger600
}

/**
 * Lampu indikator konektivitas — satu titik berwarna, seperti BCA mobile lama.
 *
 * Sengaja tanpa teks. Yang membuatnya tetap bisa diakses adalah
 * [contentDescription]: TalkBack mengucapkan keadaannya ("Jaringan lemah"),
 * jadi nasabah yang memakai pembaca layar mendapat keterangan penuh sementara
 * tampilannya tetap satu titik.
 *
 * [liveRegion] membuat perubahan warna ikut diucapkan tanpa perlu fokus
 * berpindah ke titiknya — tanpa itu, satu-satunya cara mengetahui jaringan
 * terputus adalah menyapu layar sampai menemukan titik ini.
 */
@Composable
fun NetworkStatusLamp(
    status: NetworkStatus,
    modifier: Modifier = Modifier,
) {
    // Transisi warna, bukan pergantian mendadak: titik yang berkedip keras
    // menarik mata ke sudut layar di tengah nasabah mengisi formulir.
    val color by animateColorAsState(
        targetValue = networkStatusColor(status),
        animationSpec = tween(durationMillis = COLOR_TRANSITION_MILLIS),
        label = "networkLampColor",
    )

    val description = stringResource(
        when (status) {
            NetworkStatus.ONLINE -> R.string.cd_jaringan_online
            NetworkStatus.DEGRADED -> R.string.cd_jaringan_lemah
            NetworkStatus.OFFLINE -> R.string.cd_jaringan_terputus
        },
    )

    Box(
        modifier = modifier
            .size(Spacing.s3)
            // Cincin gelap tipis: titik hijau di atas header biru primary punya
            // kontras yang cukup, tapi di atas layar putih nyaris hilang.
            // Cincinnya yang membuat satu komponen ini bekerja di kedua latar.
            .border(
                width = StrokeWidth.w0,
                color = AppColor.Neutral1000.copy(alpha = AppAlpha.A20),
                shape = AppShape.Full,
            )
            .background(color = color, shape = AppShape.Full)
            // clearAndSetSemantics: isinya hanya warna, jadi tidak ada anak yang
            // perlu diumumkan sendiri — satu pengumuman untuk satu titik.
            .clearAndSetSemantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
    )
}

private const val COLOR_TRANSITION_MILLIS = 400

// ── Previews ────────────────────────────────────────────────────────────

@Preview(name = "Lampu - Online")
@Composable
private fun NetworkStatusLampOnlinePreview() {
    BcaMobileTheme {
        NetworkStatusLamp(status = NetworkStatus.ONLINE)
    }
}

@Preview(name = "Lampu - Lemah")
@Composable
private fun NetworkStatusLampDegradedPreview() {
    BcaMobileTheme {
        NetworkStatusLamp(status = NetworkStatus.DEGRADED)
    }
}

@Preview(name = "Lampu - Terputus")
@Composable
private fun NetworkStatusLampOfflinePreview() {
    BcaMobileTheme {
        NetworkStatusLamp(status = NetworkStatus.OFFLINE)
    }
}
