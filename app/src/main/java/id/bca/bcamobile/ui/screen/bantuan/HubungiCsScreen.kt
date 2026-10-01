package id.bca.bcamobile.ui.screen.bantuan

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Elevation
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// ── State Models ─────────────────────────────────────────────────────────

data class HubungiCsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val phone: String = "",
    /** Nomor bebas pulsa; dipakai saat nasabah sedang di luar negeri. */
    val phoneFree: String = "",
    val whatsapp: String = "",
    val email: String = "",
    val chatUrl: String = "",
    val hours: String = "",
)

// ── Main Screen ──────────────────────────────────────────────────────────

/**
 * Kontak Halo BCA — dari artefak Stitch "Hubungi CS".
 *
 * Tiga penyimpangan artefak yang **sengaja tidak diikuti**, semuanya dicatat di
 * `screen-inventory.md` #23:
 *
 * 1. Kartu **Twitter / X** di artefak tidak dibuat: akunnya tidak dikirim
 *    `GET /content/contact-cs`, jadi satu-satunya cara menampilkannya adalah
 *    menulis handle di client. Slotnya diisi **nomor dari luar negeri**, yang
 *    ada di kontrak tapi justru hilang dari artefak.
 * 2. Nomor, email, dan jam layanan di artefak ditulis mati. Di sini semuanya
 *    dari state — server yang memformat, supaya mengubah nomor tidak butuh rilis.
 * 3. Jarak kosong 80dp di kaki artefak adalah siasat web untuk bottom bar;
 *    di sini `Scaffold` yang mengurusnya.
 *
 * Nilai kosong berarti server tidak mengirim kanal itu — kartunya disembunyikan,
 * bukan diisi nomor contoh. Endpoint-nya **tanpa Authorization** dan layar ini
 * tidak boleh diletakkan di belakang gerbang login: nasabah yang terkunci di
 * luar aplikasi justru yang paling butuh nomor ini.
 */
@Composable
fun HubungiCsScreen(
    state: HubungiCsUiState,
    onPhoneClick: (String) -> Unit,
    onWhatsAppClick: (String) -> Unit,
    onEmailClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.hubungi_cs_title),
                onBackClick = onBackClick,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            state.error != null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(Spacing.s6),
            ) {
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.s4))
                TextButton(onClick = onRetry) {
                    Text(
                        text = stringResource(R.string.mutasi_coba_lagi),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            ) {
                HeaderHaloBca()
                KanalSection(
                    state = state,
                    onPhoneClick = onPhoneClick,
                    onWhatsAppClick = onWhatsAppClick,
                    onEmailClick = onEmailClick,
                    onChatClick = onChatClick,
                )
            }
        }
    }
}

// ── Section Composables ──────────────────────────────────────────────────

/** Kepala layar biru: lingkaran ikon, nama layanan, dan satu baris penegasan. */
@Composable
private fun HeaderHaloBca(modifier: Modifier = Modifier) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(onPrimary.copy(alpha = AppAlpha.A10), Color.Transparent),
                    ),
                ),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Spacing.s4,
                    end = Spacing.s4,
                    top = Spacing.s6,
                    // Ruang untuk kartu pertama yang naik menimpa kepala layar.
                    bottom = Spacing.s10,
                ),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(AppSize.ContactAvatar)
                    .clip(AppShape.Full)
                    .background(onPrimary.copy(alpha = AppAlpha.A10)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_support_agent),
                    contentDescription = null,
                    tint = onPrimary,
                    modifier = Modifier.size(AppSize.Icon40),
                )
            }
            Spacer(Modifier.height(Spacing.s4))
            Text(
                text = stringResource(R.string.hubungi_cs_header_judul),
                style = MaterialTheme.typography.titleLarge,
                color = onPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.s1))
            Text(
                text = stringResource(R.string.hubungi_cs_header_subjudul),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primaryFixed,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Daftar kanal, naik [Spacing.s4] menimpa kepala layar.
 *
 * Kanal yang nilainya kosong tidak ikut dibentuk, lalu sisanya dipasangkan dua
 * per baris. Jadi hilangnya satu kanal menggeser susunan, bukan meninggalkan
 * kartu kosong.
 */
@Composable
private fun KanalSection(
    state: HubungiCsUiState,
    onPhoneClick: (String) -> Unit,
    onWhatsAppClick: (String) -> Unit,
    onEmailClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val kanal = listOfNotNull(
        kanalOrNull(
            iconRes = R.drawable.ic_chat,
            labelRes = R.string.hubungi_cs_whatsapp,
            descRes = R.string.hubungi_cs_whatsapp_desc,
            value = state.whatsapp,
            iconTint = colorScheme.tertiary,
            onClick = { onWhatsAppClick(state.whatsapp) },
        ),
        kanalOrNull(
            iconRes = R.drawable.ic_forum,
            labelRes = R.string.hubungi_cs_chat,
            descRes = R.string.hubungi_cs_chat_desc,
            value = state.chatUrl,
            iconTint = colorScheme.primary,
            onClick = { onChatClick(state.chatUrl) },
        ),
        kanalOrNull(
            iconRes = R.drawable.ic_mail,
            labelRes = R.string.hubungi_cs_email,
            descRes = null,
            value = state.email,
            iconTint = colorScheme.secondary,
            onClick = { onEmailClick(state.email) },
        ),
        // Slot yang di artefak diisi Twitter/X. Nomor luar negeri ada di
        // kontrak; akun sosial media tidak.
        kanalOrNull(
            iconRes = R.drawable.ic_phone_in_talk,
            labelRes = R.string.hubungi_cs_telepon_bebas_pulsa,
            descRes = null,
            value = state.phoneFree,
            iconTint = colorScheme.secondary,
            onClick = { onPhoneClick(state.phoneFree) },
        ),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .offset(y = -Spacing.s4)
            .padding(horizontal = Spacing.s4)
            .padding(bottom = Spacing.s6),
    ) {
        if (state.phone.isNotBlank()) {
            CallCenterCard(
                phone = state.phone,
                onClick = { onPhoneClick(state.phone) },
            )
        }

        kanal.chunked(2).forEach { baris ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                baris.forEach { item ->
                    KanalKartu(
                        item = item,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                // Baris ganjil: sisakan ruangnya supaya kartu terakhir tidak
                // melebar jadi dua kali kartu lain.
                if (baris.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (state.hours.isNotBlank()) {
            JamOperasionalCard(hours = state.hours)
        }
    }
}

/** Kartu utama: nomor Halo BCA dan tombol untuk langsung menelepon. */
@Composable
private fun CallCenterCard(
    phone: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R7,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(StrokeWidth.w0, MaterialTheme.colorScheme.surfaceVariant),
        shadowElevation = Elevation.Card,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
            modifier = Modifier.padding(Spacing.s4),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(AppSize.IconCircle)
                    .clip(AppShape.Full)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = AppAlpha.A10)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_phone_in_talk),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(AppSize.Icon24),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.hubungi_cs_telepon),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onClick,
                shape = AppShape.R6,
                contentPadding = PaddingValues(horizontal = Spacing.s4),
                modifier = Modifier.height(AppSize.ButtonCompact),
            ) {
                Text(
                    text = stringResource(R.string.hubungi_cs_telepon_aksi),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Satu kanal di grid. Seluruh kartunya area sentuh. */
@Composable
private fun KanalKartu(
    item: KanalKartuItem,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = item.onClick,
        shape = AppShape.R7,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(StrokeWidth.w0, MaterialTheme.colorScheme.surfaceVariant),
        shadowElevation = Elevation.Card,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier.padding(Spacing.s4),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(AppSize.IconBox)
                    .clip(AppShape.R6)
                    .background(item.iconTint.copy(alpha = AppAlpha.A10)),
            ) {
                Icon(
                    painter = painterResource(item.iconRes),
                    contentDescription = null,
                    tint = item.iconTint,
                    modifier = Modifier.size(AppSize.Icon20),
                )
            }
            Column {
                Text(
                    text = stringResource(item.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.s0))
                Text(
                    // Tanpa keterangan tetap, nilainya sendiri yang dicetak —
                    // alamat email dan nomor telepon sudah menjelaskan diri.
                    text = item.descRes?.let { stringResource(it) } ?: item.value,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Jam layanan apa adanya dari server — client tidak menyimpulkan "24 jam". */
@Composable
private fun JamOperasionalCard(
    hours: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R7,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.s1),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier.padding(Spacing.s4),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_schedule),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = Spacing.s0)
                    .size(AppSize.Icon20),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.hubungi_cs_jam_judul),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.s1))
                Text(
                    text = hours,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────

/** Satu kartu kanal yang sudah pasti punya nilai. */
private data class KanalKartuItem(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
    @StringRes val descRes: Int?,
    val value: String,
    val iconTint: Color,
    val onClick: () -> Unit,
)

private fun kanalOrNull(
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int,
    @StringRes descRes: Int?,
    value: String,
    iconTint: Color,
    onClick: () -> Unit,
): KanalKartuItem? = value.takeIf { it.isNotBlank() }?.let {
    KanalKartuItem(
        iconRes = iconRes,
        labelRes = labelRes,
        descRes = descRes,
        value = it,
        iconTint = iconTint,
        onClick = onClick,
    )
}

// ── Previews ─────────────────────────────────────────────────────────────

private val previewState = HubungiCsUiState(
    phone = "1500888",
    phoneFree = "+62 21 23588000",
    whatsapp = "+62 811 1500 998",
    email = "halobca@bca.co.id",
    chatUrl = "https://www.bca.co.id/halobca",
    hours = "Layanan Halo BCA tersedia 24 jam sehari, 7 hari seminggu, termasuk hari libur nasional.",
)

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun HubungiCsScreenPreview() {
    BcaMobileTheme {
        HubungiCsScreen(
            state = previewState,
            onPhoneClick = {},
            onWhatsAppClick = {},
            onEmailClick = {},
            onChatClick = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}

/**
 * Skema gelap belum didefinisikan desain, jadi [BcaMobileTheme] mengunci ke
 * terang — preview ini sengaja tetap ada supaya ketimpangannya terlihat saat
 * skema gelap akhirnya dibuat.
 */
@Preview(showBackground = true, widthDp = 360, name = "Dark")
@Composable
private fun HubungiCsScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        HubungiCsScreen(
            state = previewState,
            onPhoneClick = {},
            onWhatsAppClick = {},
            onEmailClick = {},
            onChatClick = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}

/** Server hanya mengirim sebagian kanal: susunan menggeser, tidak mengosong. */
@Preview(showBackground = true, widthDp = 360, name = "Sebagian kanal")
@Composable
private fun HubungiCsScreenSebagianPreview() {
    BcaMobileTheme {
        HubungiCsScreen(
            state = HubungiCsUiState(
                phone = "1500888",
                email = "halobca@bca.co.id",
                hours = "Setiap hari, 24 jam",
            ),
            onPhoneClick = {},
            onWhatsAppClick = {},
            onEmailClick = {},
            onChatClick = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}
