package id.bca.bcamobile.ui.screen.akun

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.CardArt
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// ── State ────────────────────────────────────────────────────────────────

enum class AkunMenuItem {
    UBAH_PIN,
    ATUR_LIMIT,
    NOTIFIKASI_PUSH,
    EMAIL_STATEMENT,
    PUSAT_BANTUAN,
    HUBUNGI_CS,
    TENTANG_APLIKASI,

    // Manajemen kartu Paspor.
    //
    // BLOKIR_KARTU, GANTI_KARTU, dan PENGGANTIAN_KARTU dilayani
    // `/account/cards/{id}/…`. KONTROL_AKSES belum: desain memberinya aksi
    // tersendiri, tapi yang dilayani server hanya dua sakelar kanal yang sudah
    // tampil sebagai baris di bawah kartu.
    KONTROL_AKSES,
    BLOKIR_KARTU,
    GANTI_KARTU,
    PENGGANTIAN_KARTU,
}

/** Dua sakelar kanal kartu; `PUT /account/cards/{id}/settings`. */
enum class KartuKanal {
    DEBIT_ONLINE,
    LUAR_NEGERI,
}

/**
 * Status kartu seperti yang digambar desain. `EXPIRED` dihitung server di zona
 * WIB — client hanya menampilkan apa yang dikirim, tidak menghitung ulang.
 */
enum class KartuStatusUi {
    AKTIF,
    DIBLOKIR,
    KEDALUWARSA,
    PENGGANTIAN,

    /** Status yang belum dikenal build ini: kartu tetap tampil, aksinya ditutup. */
    TIDAK_DIKENAL,
}

/** Bahan visual kartu. Server mengirim nama gaya, bukan warna. */
enum class KartuStyleUi {
    BLUE,
    GOLD,
    PLATINUM,
}

/**
 * Satu kartu milik nasabah, siap ditampilkan.
 *
 * Tidak ada nomor kartu utuh di sini dan tidak akan ada: backend hanya
 * menyimpan bentuk tersamar.
 */
data class KartuItem(
    val cardId: String,
    val maskedNumber: String,
    val holderName: String,
    val productName: String,
    /** `MM/YY` dari server. */
    val validThru: String,
    val status: KartuStatusUi,
    val style: KartuStyleUi,
    val isDebitOnlineEnabled: Boolean,
    val isLuarNegeriEnabled: Boolean,
    /** Terisi hanya saat [status] [KartuStatusUi.DIBLOKIR]. */
    val blockedReasonLabel: String?,
) {
    /** Kartu yang diblokir atau kedaluwarsa tidak menerima perubahan apa pun. */
    val isActionable: Boolean get() = status == KartuStatusUi.AKTIF
}

data class AkunUiState(
    val userName: String = "",
    val phoneNumber: String = "",
    /** Nomor rekening utama, sudah dikelompokkan empat digit. */
    val accountNumber: String = "",
    val email: String = "",
    /**
     * Tier nasabah, mis. "Prioritas", dari `tier` di `GET /account/profile`.
     *
     * Nasabah reguler **tidak** dikirimi field ini, jadi kosong di sini berarti
     * badge disembunyikan — bukan nilai contoh dari desain.
     */
    val accountTier: String = "",
    val appVersion: String = "",
    val isBiometricEnabled: Boolean = false,
    val isNotificationEnabled: Boolean = true,
    /** `PUT /account/settings` field `email_statement_enabled`. */
    val isEmailStatementEnabled: Boolean = false,
    /**
     * Kartu utama nasabah dari `GET /account/cards`.
     *
     * Null selama belum dimuat **atau** saat nasabah memang belum punya kartu;
     * [hasNoCards] yang membedakan keduanya. Server menjawab daftar kosong
     * dengan `200`, bukan `404`, jadi "belum punya kartu" bukan kegagalan.
     */
    val card: KartuItem? = null,
    /** True hanya setelah server menjawab daftar kosong. */
    val hasNoCards: Boolean = false,
    /** Ada perubahan kartu yang sedang dikirim; sakelar dikunci sementara. */
    val isCardBusy: Boolean = false,
    /** Pesan hasil aksi kartu (gagal maupun berhasil), sekali tampil. */
    val cardMessage: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

// ── Main Screen ──────────────────────────────────────────────────────────

@Composable
fun AkunScreen(
    state: AkunUiState,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLihatProfilClick: () -> Unit,
    onMenuItemClick: (AkunMenuItem) -> Unit,
    onCardSettingToggle: (KartuKanal, Boolean) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onNotificationToggle: (Boolean) -> Unit,
    onEmailStatementToggle: (Boolean) -> Unit,
    onKeluarClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AkunTopBar(
                onNotificationClick = onNotificationClick,
                onProfileClick = onProfileClick,
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            state.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(Spacing.s4))
                    Button(onClick = onRetry) {
                        Text(text = stringResource(R.string.mutasi_coba_lagi))
                    }
                }
            }

            else -> {
                AkunContent(
                    state = state,
                    onLihatProfilClick = onLihatProfilClick,
                    onMenuItemClick = onMenuItemClick,
                    onCardSettingToggle = onCardSettingToggle,
                    onBiometricToggle = onBiometricToggle,
                    onNotificationToggle = onNotificationToggle,
                    onEmailStatementToggle = onEmailStatementToggle,
                    onKeluarClick = onKeluarClick,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

// ── Content ──────────────────────────────────────────────────────────────

@Composable
private fun AkunContent(
    state: AkunUiState,
    onLihatProfilClick: () -> Unit,
    onMenuItemClick: (AkunMenuItem) -> Unit,
    onCardSettingToggle: (KartuKanal, Boolean) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onNotificationToggle: (Boolean) -> Unit,
    onEmailStatementToggle: (Boolean) -> Unit,
    onKeluarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.s4)
            .padding(top = Spacing.s4),
    ) {
        ProfileHeaderCard(
            userName = state.userName,
            phoneNumber = state.phoneNumber,
            accountNumber = state.accountNumber,
            email = state.email,
            accountTier = state.accountTier,
            onClick = onLihatProfilClick,
        )

        Spacer(Modifier.height(Spacing.s6))

        KartuPasporSection(
            card = state.card,
            hasNoCards = state.hasNoCards,
            isBusy = state.isCardBusy,
            onMenuItemClick = onMenuItemClick,
            onCardSettingToggle = onCardSettingToggle,
        )

        Spacer(Modifier.height(Spacing.s6))

        // ── KEAMANAN & AKUN ──
        // Baris "Atur Limit Transaksi" sengaja tidak ada di sini: desain hanya
        // menaruhnya sebagai aksi kartu, dan itu sudah terhubung di atas.
        SettingsSection(label = stringResource(R.string.akun_section_keamanan_akun)) {
            SettingsRow(
                icon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                label = stringResource(R.string.akun_ubah_pin_password),
                onClick = { onMenuItemClick(AkunMenuItem.UBAH_PIN) },
                iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer
                    .copy(alpha = AppAlpha.A20),
            )
            SettingsRowDivider()
            SettingsRow(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_fingerprint),
                        contentDescription = null,
                    )
                },
                label = stringResource(R.string.akun_login_biometrik),
                subtitle = stringResource(R.string.akun_login_biometrik_sub),
                onClick = { onBiometricToggle(!state.isBiometricEnabled) },
                iconBackgroundColor = MaterialTheme.colorScheme.tertiaryContainer
                    .copy(alpha = AppAlpha.A20),
                iconTintColor = MaterialTheme.colorScheme.tertiary,
                trailing = {
                    Switch(
                        checked = state.isBiometricEnabled,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedThumbColor = AppColor.Neutral100,
                        ),
                    )
                },
            )
        }

        Spacer(Modifier.height(Spacing.s6))

        // ── NOTIFIKASI & LAPORAN ──
        SettingsSection(label = stringResource(R.string.akun_section_notifikasi_laporan)) {
            SettingsRow(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                    )
                },
                label = stringResource(R.string.akun_notifikasi_transaksi),
                subtitle = stringResource(R.string.akun_notifikasi_transaksi_sub),
                // Sakelar, bukan baris navigasi seperti di artefak Stitch:
                // `PUT /account/settings` melayani `notification_enabled`, dan
                // `screen-inventory.md` menyebutnya sakelar secara eksplisit.
                onClick = { onNotificationToggle(!state.isNotificationEnabled) },
                iconBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                iconTintColor = MaterialTheme.colorScheme.onSurfaceVariant,
                trailing = {
                    Switch(
                        checked = state.isNotificationEnabled,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedThumbColor = AppColor.Neutral100,
                        ),
                    )
                },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
                label = stringResource(R.string.akun_email_estatement),
                subtitle = stringResource(R.string.akun_email_estatement_sub),
                // Sakelar, bukan baris navigasi: `PUT /account/settings` sudah
                // melayani `email_statement_enabled` sejak migrasi 000018.
                onClick = { onEmailStatementToggle(!state.isEmailStatementEnabled) },
                iconBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                iconTintColor = MaterialTheme.colorScheme.onSurfaceVariant,
                trailing = {
                    Switch(
                        checked = state.isEmailStatementEnabled,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedThumbColor = AppColor.Neutral100,
                        ),
                    )
                },
            )
        }

        Spacer(Modifier.height(Spacing.s6))

        // ── BANTUAN & INFORMASI ──
        SettingsSection(label = stringResource(R.string.akun_section_bantuan_informasi)) {
            SettingsRow(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_help_outline),
                        contentDescription = null,
                    )
                },
                label = stringResource(R.string.akun_pusat_bantuan_faq),
                onClick = { onMenuItemClick(AkunMenuItem.PUSAT_BANTUAN) },
                iconBackgroundColor = MaterialTheme.colorScheme.inversePrimary
                    .copy(alpha = AppAlpha.A20),
            )
            SettingsRowDivider()
            SettingsRow(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_phone_in_talk),
                        contentDescription = null,
                    )
                },
                label = stringResource(R.string.akun_halo_bca_cs),
                subtitle = stringResource(R.string.akun_halo_bca_cs_sub),
                onClick = { onMenuItemClick(AkunMenuItem.HUBUNGI_CS) },
                iconBackgroundColor = MaterialTheme.colorScheme.inversePrimary
                    .copy(alpha = AppAlpha.A20),
            )
            SettingsRowDivider()
            SettingsRow(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_info),
                        contentDescription = null,
                    )
                },
                label = stringResource(R.string.akun_tentang_aplikasi),
                onClick = { onMenuItemClick(AkunMenuItem.TENTANG_APLIKASI) },
                iconBackgroundColor = MaterialTheme.colorScheme.inversePrimary
                    .copy(alpha = AppAlpha.A20),
                trailing = {
                    Text(
                        text = state.appVersion,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }

        Spacer(Modifier.height(Spacing.s6))

        // ── Logout ──
        Button(
            onClick = onKeluarClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            shape = AppShape.R6,
            contentPadding = PaddingValues(horizontal = Spacing.s6, vertical = Spacing.s4),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(Spacing.s5),
            )
            Spacer(Modifier.width(Spacing.s2))
            Text(
                text = stringResource(R.string.akun_keluar),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(Modifier.height(Spacing.s7))
    }
}

// ── Top Bar ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AkunTopBar(
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
            ) {
                Image(
                    painter = painterResource(R.drawable.bca_logo_white),
                    contentDescription = stringResource(R.string.cd_bca_logo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(Spacing.s6),
                )
                Text(
                    text = stringResource(R.string.nav_akun),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        },
        actions = {
            IconButton(onClick = onNotificationClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = stringResource(R.string.cd_notifikasi),
                )
            }
            IconButton(onClick = onProfileClick) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(Spacing.s8)
                        .border(
                            width = StrokeWidth.w1,
                            color = AppColor.Neutral100.copy(alpha = AppAlpha.A20),
                            shape = AppShape.Full,
                        )
                        .clip(AppShape.Full)
                        .background(AppColor.Neutral100.copy(alpha = AppAlpha.A10)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.cd_profile),
                        modifier = Modifier.size(Spacing.s6),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = AppColor.Neutral100,
            actionIconContentColor = AppColor.Neutral100,
        ),
        modifier = modifier,
    )
}

// ── Profile Header Card ─────────────────────────────────────────────────

@Composable
private fun ProfileHeaderCard(
    userName: String,
    phoneNumber: String,
    accountNumber: String,
    email: String,
    accountTier: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        // Desain memisahkan kartu dari latar lewat `surface-container`.
        // `surface` tidak bisa dipakai di sini: nilainya sama dengan `background`.
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            width = StrokeWidth.w0,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppAlpha.A30),
        ),
        shadowElevation = Spacing.s0,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(Spacing.s4),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Spacing.s10)
                    .clip(AppShape.Full)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Spacing.s7),
                )
            }

            Spacer(Modifier.width(Spacing.s4))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    // Tier hanya tampil kalau server mengirimkannya. Tidak ada
                    // endpoint yang membalasnya saat ini, jadi normalnya kosong.
                    if (accountTier.isNotBlank()) {
                        Spacer(Modifier.width(Spacing.s2))
                        Text(
                            text = accountTier,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary
                                        .copy(alpha = AppAlpha.A10),
                                    shape = AppShape.Full,
                                )
                                .border(
                                    width = StrokeWidth.w0,
                                    color = MaterialTheme.colorScheme.primary
                                        .copy(alpha = AppAlpha.A20),
                                    shape = AppShape.Full,
                                )
                                .padding(horizontal = Spacing.s2, vertical = Spacing.s0),
                        )
                    }
                }
                Text(
                    // Nomor handphone dan rekening utama, seperti di desain.
                    text = if (accountNumber.isBlank()) {
                        phoneNumber
                    } else {
                        stringResource(
                            R.string.akun_hp_rekening_format,
                            phoneNumber,
                            accountNumber,
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (email.isNotBlank()) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(Spacing.s1))

                // Penanda bahwa seluruh kartu dapat ditekan; aksinya milik Surface
                // di atas, jadi baris ini tidak menambah target sentuh bersarang.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.akun_lihat_profil),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppSize.Icon16),
                    )
                }
            }
        }
    }
}

// ── Manajemen Kartu Paspor ──────────────────────────────────────────────

/**
 * Bagian manajemen kartu Paspor sesuai desain, tersambung ke
 * `GET /account/cards` beserta tiga endpoint turunannya.
 *
 * Nomor kartu yang tampil adalah **satu-satunya** bentuk yang ada: backend
 * tidak pernah menyimpan PAN utuh, jadi tidak ada "lihat nomor lengkap".
 *
 * Kartu yang tidak aktif mengunci seluruh aksinya. Menawarkan "Blokir Kartu"
 * pada kartu yang sudah diblokir hanya menghasilkan `409 CARD_BLOCKED`.
 */
@Composable
private fun KartuPasporSection(
    card: KartuItem?,
    hasNoCards: Boolean,
    isBusy: Boolean,
    onMenuItemClick: (AkunMenuItem) -> Unit,
    onCardSettingToggle: (KartuKanal, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_credit_card),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Spacing.s5),
                )
                Spacer(Modifier.width(Spacing.s2))
                Text(
                    text = stringResource(R.string.profil_manajemen_kartu),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (card != null) {
                Spacer(Modifier.width(Spacing.s2))
                KartuStatusChip(status = card.status)
            }
        }

        Spacer(Modifier.height(Spacing.s3))

        if (card == null) {
            // Daftar kosong dijawab 200, bukan 404 — ini bukan kegagalan, jadi
            // yang tampil keterangan, bukan tombol coba lagi.
            Text(
                text = stringResource(
                    if (hasNoCards) {
                        R.string.profil_kartu_kosong
                    } else {
                        R.string.profil_kartu_belum_tersedia
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        KartuPasporArt(card = card)

        if (card.blockedReasonLabel != null) {
            Spacer(Modifier.height(Spacing.s2))
            Text(
                text = stringResource(
                    R.string.profil_kartu_diblokir_alasan,
                    card.blockedReasonLabel,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(Spacing.s3))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Empat aksi memakai empat peran warna berbeda, seperti di desain —
            // bukan satu warna primary untuk semuanya.
            KartuAksi(
                iconRes = R.drawable.ic_payments,
                label = stringResource(R.string.profil_aksi_atur_limit),
                onClick = { onMenuItemClick(AkunMenuItem.ATUR_LIMIT) },
                modifier = Modifier.weight(1f),
            )
            KartuAksi(
                iconRes = R.drawable.ic_security,
                label = stringResource(R.string.profil_aksi_kontrol_akses),
                onClick = { onMenuItemClick(AkunMenuItem.KONTROL_AKSES) },
                iconBackgroundColor = MaterialTheme.colorScheme.tertiaryContainer
                    .copy(alpha = AppAlpha.A10),
                iconTintColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
            )
            KartuAksi(
                iconRes = R.drawable.ic_lock,
                label = stringResource(R.string.profil_aksi_blokir_kartu),
                onClick = { onMenuItemClick(AkunMenuItem.BLOKIR_KARTU) },
                // Kartu yang sudah diblokir atau kedaluwarsa tidak bisa
                // diblokir lagi; server menjawab 409 CARD_BLOCKED.
                enabled = card.isActionable && !isBusy,
                iconBackgroundColor = MaterialTheme.colorScheme.errorContainer,
                iconTintColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            KartuAksi(
                iconRes = R.drawable.ic_sync,
                label = stringResource(R.string.profil_aksi_ganti_kartu),
                onClick = { onMenuItemClick(AkunMenuItem.GANTI_KARTU) },
                // Penggantian yang sedang berjalan tidak boleh dikirim dua kali.
                enabled = card.status != KartuStatusUi.PENGGANTIAN && !isBusy,
                iconBackgroundColor = MaterialTheme.colorScheme.secondaryContainer
                    .copy(alpha = AppAlpha.A20),
                iconTintColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(Spacing.s3))

        Surface(
            shape = AppShape.R6,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = StrokeWidth.w0,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppAlpha.A30),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                // Dua sakelar kanal, `PUT /account/cards/{id}/settings`. Yang
                // dikirim hanya sakelar yang diubah; yang satunya dibiarkan.
                KartuSakelarRow(
                    iconRes = R.drawable.ic_credit_card,
                    label = stringResource(R.string.profil_debit_online),
                    subtitle = stringResource(R.string.profil_debit_online_sub),
                    checked = card.isDebitOnlineEnabled,
                    enabled = card.isActionable && !isBusy,
                    onCheckedChange = { onCardSettingToggle(KartuKanal.DEBIT_ONLINE, it) },
                    iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer
                        .copy(alpha = AppAlpha.A20),
                )
                SettingsRowDivider()
                KartuSakelarRow(
                    iconRes = R.drawable.ic_language,
                    label = stringResource(R.string.profil_luar_negeri),
                    subtitle = stringResource(R.string.profil_luar_negeri_sub),
                    checked = card.isLuarNegeriEnabled,
                    enabled = card.isActionable && !isBusy,
                    onCheckedChange = { onCardSettingToggle(KartuKanal.LUAR_NEGERI, it) },
                    iconBackgroundColor = MaterialTheme.colorScheme.secondaryContainer
                        .copy(alpha = AppAlpha.A20),
                    iconTintColor = MaterialTheme.colorScheme.secondary,
                )
                SettingsRowDivider()
                SettingsRow(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_local_shipping),
                            contentDescription = null,
                        )
                    },
                    label = stringResource(R.string.profil_penggantian_kartu),
                    subtitle = stringResource(R.string.profil_penggantian_kartu_sub),
                    onClick = { onMenuItemClick(AkunMenuItem.PENGGANTIAN_KARTU) },
                    iconBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    iconTintColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconShape = AppShape.R4,
                )
            }
        }
    }
}

/**
 * Lencana status kartu.
 *
 * Empat status server dipetakan ke empat peran warna; tidak ada hex di sini.
 */
@Composable
private fun KartuStatusChip(
    status: KartuStatusUi,
    modifier: Modifier = Modifier,
) {
    val color = when (status) {
        KartuStatusUi.AKTIF -> MaterialTheme.colorScheme.tertiary
        KartuStatusUi.DIBLOKIR -> MaterialTheme.colorScheme.error
        KartuStatusUi.PENGGANTIAN -> MaterialTheme.colorScheme.secondary
        KartuStatusUi.KEDALUWARSA,
        KartuStatusUi.TIDAK_DIKENAL,
        -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(color = color.copy(alpha = AppAlpha.A10), shape = AppShape.Full)
            .padding(horizontal = Spacing.s2, vertical = Spacing.s0),
    ) {
        Box(
            modifier = Modifier
                .size(Spacing.s2)
                .background(color = color, shape = AppShape.Full),
        )
        Spacer(Modifier.width(Spacing.s1))
        Text(
            text = stringResource(status.labelRes()),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
        )
    }
}

private fun KartuStatusUi.labelRes(): Int = when (this) {
    KartuStatusUi.AKTIF -> R.string.profil_kartu_status_aktif
    KartuStatusUi.DIBLOKIR -> R.string.profil_kartu_status_diblokir
    KartuStatusUi.KEDALUWARSA -> R.string.profil_kartu_status_kedaluwarsa
    KartuStatusUi.PENGGANTIAN -> R.string.profil_kartu_status_penggantian
    KartuStatusUi.TIDAK_DIKENAL -> R.string.profil_kartu_status_tidak_dikenal
}

/**
 * Baris sakelar kartu.
 *
 * Berbeda dari [SettingsRow] biasa: sakelarnya benar-benar menerima perubahan,
 * jadi `onCheckedChange` dipasang pada [Switch] dan barisnya tidak ikut
 * menangkap klik — dua jalur ke aksi yang sama membuat sakelar terpicu ganda.
 */
@Composable
private fun KartuSakelarRow(
    @DrawableRes iconRes: Int,
    label: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    iconBackgroundColor: Color = MaterialTheme.colorScheme.primaryContainer
        .copy(alpha = AppAlpha.A20),
    iconTintColor: Color = MaterialTheme.colorScheme.primary,
) {
    SettingsRow(
        icon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
            )
        },
        label = label,
        subtitle = subtitle,
        onClick = null,
        iconBackgroundColor = iconBackgroundColor,
        iconTintColor = iconTintColor,
        iconShape = AppShape.R4,
        modifier = modifier,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = AppColor.Neutral100,
                ),
            )
        },
    )
}

/**
 * Gambar kartu dengan data nyata dari `GET /account/cards`.
 *
 * Gaya kartu datang sebagai nama (`BLUE`/`GOLD`/`PLATINUM`) dan dipetakan ke
 * gradien token [CardArt] — server tidak mengirim hex maupun URL gambar, dan
 * memang tidak akan.
 */
@Composable
private fun KartuPasporArt(
    card: KartuItem,
    modifier: Modifier = Modifier,
) {
    val brush = Brush.linearGradient(card.style.gradientColors())
    val onCard = card.style.onCardColor()

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .height(AppSize.DebitCardHeight)
            .clip(AppShape.R7)
            .background(brush)
            .padding(Spacing.s4),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.buka_rekening_kartu_brand),
                    style = MaterialTheme.typography.titleMedium,
                    color = onCard,
                )
                Spacer(Modifier.width(Spacing.s2))
                Text(
                    text = stringResource(R.string.buka_rekening_kartu_tier_debit),
                    style = MaterialTheme.typography.labelSmall,
                    color = onCard,
                    modifier = Modifier
                        .clip(AppShape.R2)
                        .background(onCard.copy(alpha = AppAlpha.A20))
                        .padding(horizontal = Spacing.s2, vertical = Spacing.s0),
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_contactless),
                contentDescription = null,
                tint = onCard,
                modifier = Modifier.size(Spacing.s5),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(AppSize.CardChipWidth)
                    .height(AppSize.CardChipHeight)
                    .clip(AppShape.R2)
                    .background(onCard.copy(alpha = AppAlpha.A30)),
            )
            Spacer(Modifier.width(Spacing.s3))
            Text(
                // Nama produk dari katalog server, mis. "Paspor BCA Gold".
                text = card.productName.ifBlank {
                    stringResource(R.string.buka_rekening_kartu_paspor)
                },
                style = MaterialTheme.typography.labelSmall,
                color = onCard,
            )
        }

        Text(
            // Satu-satunya bentuk nomor yang ada; PAN utuh tidak pernah dikirim.
            text = card.maskedNumber.ifBlank {
                stringResource(R.string.profil_kartu_nomor_kosong)
            },
            style = MaterialTheme.typography.titleMedium,
            color = onCard,
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profil_kartu_pemegang),
                    style = MaterialTheme.typography.labelSmall,
                    color = onCard,
                )
                Text(
                    text = card.holderName,
                    style = MaterialTheme.typography.labelLarge,
                    color = onCard,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.profil_kartu_valid_thru),
                        style = MaterialTheme.typography.labelSmall,
                        color = onCard,
                    )
                    Text(
                        // `MM/YY` sudah dirakit server dari bulan dan tahunnya.
                        text = card.validThru.ifBlank {
                            stringResource(R.string.profil_kartu_valid_thru_kosong)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = onCard,
                    )
                }
                Spacer(Modifier.width(Spacing.s3))
                Image(
                    painter = painterResource(R.drawable.ic_mastercard),
                    contentDescription = stringResource(R.string.cd_mastercard),
                    modifier = Modifier.size(
                        width = AppSize.CardChipWidth,
                        height = AppSize.CardChipHeight,
                    ),
                )
            }
        }
    }
}

/**
 * Gradien per gaya kartu, memakai grup token `CardArt` yang sama dengan layar
 * pilih kartu — bukan lima ramp design system, karena ini material kartu.
 */
private fun KartuStyleUi.gradientColors(): List<Color> = when (this) {
    KartuStyleUi.BLUE -> listOf(CardArt.BlueStart, CardArt.BlueEnd)
    KartuStyleUi.GOLD -> listOf(CardArt.GoldStart, CardArt.GoldMid, CardArt.GoldEnd)
    KartuStyleUi.PLATINUM ->
        listOf(CardArt.PlatinumStart, CardArt.PlatinumMid, CardArt.PlatinumEnd)
}

/** Platinum memakai putih teredam supaya tidak menyilaukan di atas hitam. */
private fun KartuStyleUi.onCardColor(): Color = when (this) {
    KartuStyleUi.PLATINUM -> CardArt.OnCardPlatinum
    else -> CardArt.OnCard
}

@Composable
private fun KartuAksi(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Aksi yang ditolak server untuk status kartu ini dimatikan, bukan disembunyikan. */
    enabled: Boolean = true,
    iconBackgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.A10),
    iconTintColor: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = AppShape.R6,
        // `surface` sama dengan `background`, jadi tombol jadi tak terlihat.
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.heightIn(min = AppSize.MinTouchTarget),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = Spacing.s3, horizontal = Spacing.s1),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Spacing.s8)
                    .clip(AppShape.Full)
                    .background(iconBackgroundColor),
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(Spacing.s5),
                )
            }
            Spacer(Modifier.height(Spacing.s1))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Settings Components ─────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.s2),
        )
        Spacer(Modifier.height(Spacing.s3))
        Surface(
            shape = AppShape.R6,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = StrokeWidth.w0,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppAlpha.A30),
            ),
            shadowElevation = Spacing.s0,
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: @Composable () -> Unit,
    label: String,
    /** Null saat barisnya bukan tujuan klik — mis. baris yang hanya memuat sakelar. */
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    iconBackgroundColor: Color = MaterialTheme.colorScheme.primaryContainer
        .copy(alpha = AppAlpha.A20),
    iconTintColor: Color = MaterialTheme.colorScheme.primary,
    iconShape: Shape = AppShape.Full,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    },
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(Spacing.s4),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s8)
                .clip(iconShape)
                .background(iconBackgroundColor),
        ) {
            CompositionLocalProvider(
                LocalContentColor provides iconTintColor,
            ) {
                icon()
            }
        }

        Spacer(Modifier.width(Spacing.s3))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        trailing()
    }
}

@Composable
private fun SettingsRowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = modifier.padding(start = Spacing.s4 + Spacing.s8),
    )
}

// ── Previews ─────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun AkunScreenPreview() {
    BcaMobileTheme {
        AkunScreen(
            state = AkunUiState(
                userName = "Budi Santoso",
                phoneNumber = "0812 3456 7890",
                accountNumber = "5264 4500 89",
                accountTier = "Prioritas",
                appVersion = "v2.4.1",
                isBiometricEnabled = true,
                card = previewCard,
            ),
            onNotificationClick = {},
            onProfileClick = {},
            onLihatProfilClick = {},
            onMenuItemClick = {},
            onCardSettingToggle = { _, _ -> },
            onBiometricToggle = {},
            onNotificationToggle = {},
            onEmailStatementToggle = {},
            onKeluarClick = {},
            onRetry = {},
        )
    }
}

private val previewCard = KartuItem(
    cardId = "preview",
    maskedNumber = "5221 •••• •••• 1234",
    holderName = "BUDI SANTOSO",
    productName = "Paspor BCA Gold",
    validThru = "12/28",
    status = KartuStatusUi.AKTIF,
    style = KartuStyleUi.GOLD,
    isDebitOnlineEnabled = true,
    isLuarNegeriEnabled = false,
    blockedReasonLabel = null,
)

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AkunScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        AkunScreen(
            state = AkunUiState(
                userName = "Budi Santoso",
                phoneNumber = "0812 3456 7890",
                accountNumber = "5264 4500 89",
                accountTier = "Prioritas",
                appVersion = "v2.4.1",
                isBiometricEnabled = true,
                card = previewCard,
            ),
            onNotificationClick = {},
            onProfileClick = {},
            onLihatProfilClick = {},
            onMenuItemClick = {},
            onCardSettingToggle = { _, _ -> },
            onBiometricToggle = {},
            onNotificationToggle = {},
            onEmailStatementToggle = {},
            onKeluarClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AkunScreenLoadingPreview() {
    BcaMobileTheme {
        AkunScreen(
            state = AkunUiState(isLoading = true),
            onNotificationClick = {},
            onProfileClick = {},
            onLihatProfilClick = {},
            onMenuItemClick = {},
            onCardSettingToggle = { _, _ -> },
            onBiometricToggle = {},
            onNotificationToggle = {},
            onEmailStatementToggle = {},
            onKeluarClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AkunScreenErrorPreview() {
    BcaMobileTheme {
        AkunScreen(
            state = AkunUiState(
                error = stringResource(R.string.error_general_retry),
            ),
            onNotificationClick = {},
            onProfileClick = {},
            onLihatProfilClick = {},
            onMenuItemClick = {},
            onCardSettingToggle = { _, _ -> },
            onBiometricToggle = {},
            onNotificationToggle = {},
            onEmailStatementToggle = {},
            onKeluarClick = {},
            onRetry = {},
        )
    }
}