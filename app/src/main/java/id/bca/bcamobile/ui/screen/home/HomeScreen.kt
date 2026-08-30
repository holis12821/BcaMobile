package id.bca.bcamobile.ui.screen.home

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// ── State ────────────────────────────────────────────────────────────────

data class PromoItem(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String = "",
)

data class BerandaUiState(
    val userName: String = "",
    val balance: String = "Rp 12.500.000",
    val isBalanceVisible: Boolean = false,
    val promoItems: List<PromoItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

enum class QuickAction(@param:StringRes val labelRes: Int) {
    M_INFO(R.string.menu_m_info),
    TRANSFER(R.string.menu_transfer),
    E_WALLET(R.string.menu_e_wallet),
    PULSA(R.string.menu_pulsa),
    CARDLESS(R.string.menu_cardless),
    M_ADMIN(R.string.menu_m_admin),
    M_COMMERCE(R.string.menu_m_commerce),
    LAINNYA(R.string.menu_lainnya),
}

// ── Main Screen ──────────────────────────────────────────────────────────

@Composable
fun BerandaScreen(
    state: BerandaUiState,
    onToggleBalance: () -> Unit,
    onIsiSaldo: () -> Unit,
    onMutasi: () -> Unit,
    onQuickAction: (QuickAction) -> Unit,
    onPromoClick: (PromoItem) -> Unit,
    onLihatSemuaPromo: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            BerandaTopBar(
                onNotificationClick = onNotificationClick,
                onProfileClick = onProfileClick,
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues),
        ) {
            if (state.errorMessage != null) {
                ErrorBanner(
                    message = state.errorMessage,
                    onRetry = onRetry,
                )
            }

            GreetingSection(userName = state.userName)

            SaldoCard(
                balance = state.balance,
                isBalanceVisible = state.isBalanceVisible,
                onToggleBalance = onToggleBalance,
                onIsiSaldo = onIsiSaldo,
                onMutasi = onMutasi,
            )

            QuickActionsGrid(onQuickAction = onQuickAction)

            PromoSection(
                promoItems = state.promoItems,
                onPromoClick = onPromoClick,
                onLihatSemua = onLihatSemuaPromo,
            )
        }
    }
}

// ── Top Bar ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BerandaTopBar(
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
                    text = stringResource(R.string.nav_beranda),
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

// ── Greeting Section ─────────────────────────────────────────────────────

@Composable
private fun GreetingSection(
    userName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.s4)
            .padding(top = Spacing.s6, bottom = Spacing.s4),
    ) {
        Text(
            text = stringResource(R.string.beranda_selamat_datang),
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(Modifier.height(Spacing.s1))

        Text(
            text = userName,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Saldo Card ───────────────────────────────────────────────────────────

@Composable
private fun SaldoCard(
    balance: String,
    isBalanceVisible: Boolean,
    onToggleBalance: () -> Unit,
    onIsiSaldo: () -> Unit,
    onMutasi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = Spacing.s2,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.s4)
            .padding(bottom = Spacing.s8),
    ) {
        Box {
            // Decorative gradient overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                AppColor.Neutral100.copy(alpha = AppAlpha.A10),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            Column(modifier = Modifier.padding(Spacing.s5)) {
                // Top row: saldo info + wallet icon
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.s6),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.beranda_total_saldo),
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColor.Neutral100.copy(alpha = AppAlpha.A80),
                        )

                        Spacer(Modifier.height(Spacing.s1))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                        ) {
                            Text(
                                text = if (isBalanceVisible) balance
                                else stringResource(R.string.beranda_balance_hidden),
                                style = MaterialTheme.typography.titleLarge,
                                color = AppColor.Neutral100,
                            )

                            IconButton(
                                onClick = onToggleBalance,
                                modifier = Modifier.size(Spacing.s7),
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isBalanceVisible) R.drawable.ic_visibility
                                        else R.drawable.ic_visibility_off,
                                    ),
                                    contentDescription = stringResource(R.string.cd_toggle_balance),
                                    tint = AppColor.Neutral100,
                                    modifier = Modifier.size(Spacing.s5),
                                )
                            }
                        }
                    }

                    // Wallet icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(
                                color = AppColor.Neutral100.copy(alpha = AppAlpha.A20),
                                shape = AppShape.R4,
                            )
                            .padding(Spacing.s2),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_wallet),
                            contentDescription = null,
                            tint = AppColor.Neutral100,
                            modifier = Modifier.size(Spacing.s6),
                        )
                    }
                }

                // Bottom row: action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SaldoActionButton(
                        text = stringResource(R.string.beranda_isi_saldo),
                        isPrimary = true,
                        onClick = onIsiSaldo,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(AppSize.IconSmall),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                    SaldoActionButton(
                        text = stringResource(R.string.beranda_mutasi),
                        isPrimary = false,
                        onClick = onMutasi,
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_receipt),
                                contentDescription = null,
                                tint = AppColor.Neutral100,
                                modifier = Modifier.size(AppSize.IconSmall),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SaldoActionButton(
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = AppShape.R4,
        color = if (isPrimary) AppColor.Neutral100
        else AppColor.Neutral100.copy(alpha = AppAlpha.A20),
        modifier = modifier.height(Spacing.s8),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
        ) {
            icon()
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (isPrimary) MaterialTheme.colorScheme.primary
                else AppColor.Neutral100,
            )
        }
    }
}

// ── Quick Actions Grid ───────────────────────────────────────────────────

@Composable
private fun QuickActionsGrid(
    onQuickAction: (QuickAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s6),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.s4)
            .padding(bottom = Spacing.s8),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            modifier = Modifier.fillMaxWidth(),
        ) {
            QuickAction.entries.take(4).forEach { action ->
                MenuGridItem(
                    action = action,
                    onClick = { onQuickAction(action) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            modifier = Modifier.fillMaxWidth(),
        ) {
            QuickAction.entries.drop(4).forEach { action ->
                MenuGridItem(
                    action = action,
                    onClick = { onQuickAction(action) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MenuGridItem(
    action: QuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Spacing.s10)
                    .background(
                        color = AppColor.Neutral200,
                        shape = AppShape.R7,
                    ),
            ) {
                QuickActionIcon(action)
            }
            Text(
                text = stringResource(action.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QuickActionIcon(action: QuickAction) {
    val tint = MaterialTheme.colorScheme.primary
    val iconModifier = Modifier.size(AppSize.IconLarge)

    when (action) {
        QuickAction.M_INFO -> Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = tint,
            modifier = iconModifier,
        )
        QuickAction.TRANSFER -> Icon(
            painter = painterResource(R.drawable.ic_transfer),
            contentDescription = null,
            tint = tint,
            modifier = iconModifier,
        )
        QuickAction.E_WALLET -> Icon(
            painter = painterResource(R.drawable.ic_wallet),
            contentDescription = null,
            tint = tint,
            modifier = iconModifier,
        )
        QuickAction.PULSA -> Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = null,
            tint = tint,
            modifier = iconModifier,
        )
        QuickAction.CARDLESS -> Icon(
            painter = painterResource(R.drawable.ic_contactless),
            contentDescription = null,
            tint = tint,
            modifier = iconModifier,
        )
        QuickAction.M_ADMIN -> Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            tint = tint,
            modifier = iconModifier,
        )
        QuickAction.M_COMMERCE -> Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            tint = tint,
            modifier = iconModifier,
        )
        QuickAction.LAINNYA -> Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = null,
            tint = tint,
            modifier = iconModifier,
        )
    }
}

// ── Promo Section ────────────────────────────────────────────────────────

@Composable
private fun PromoSection(
    promoItems: List<PromoItem>,
    onPromoClick: (PromoItem) -> Unit,
    onLihatSemua: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = Spacing.s6)) {
        // Header
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.s4)
                .padding(bottom = Spacing.s4),
        ) {
            Text(
                text = stringResource(R.string.beranda_promo_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            TextButton(onClick = onLihatSemua) {
                Text(
                    text = stringResource(R.string.beranda_lihat_semua),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (promoItems.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.s4)
                    .height(AppSize.PromoImageHeight),
            ) {
                Text(
                    text = stringResource(R.string.beranda_no_promo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.s4)
                    .padding(bottom = Spacing.s4),
            ) {
                promoItems.forEach { promo ->
                    PromoCard(
                        promo = promo,
                        onClick = { onPromoClick(promo) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PromoCard(
    promo: PromoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = AppShape.R6,
        color = AppColor.Neutral200,
        shadowElevation = Spacing.s1,
        modifier = modifier.width(AppSize.PromoCardWidth),
    ) {
        Column {
            // Image placeholder
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSize.PromoImageHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Spacing.s8),
                )
            }

            Column(modifier = Modifier.padding(Spacing.s4)) {
                Text(
                    text = promo.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(Spacing.s1))

                Text(
                    text = promo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── Error Banner ─────────────────────────────────────────────────────────

@Composable
private fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R4,
        color = AppColor.Danger100,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.s4)
            .padding(top = Spacing.s4),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier.padding(Spacing.s4),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = AppColor.Danger1000,
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = onRetry,
                shape = AppShape.R4,
                color = AppColor.Danger600,
            ) {
                Text(
                    text = stringResource(R.string.beranda_coba_lagi),
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColor.Neutral100,
                    modifier = Modifier.padding(
                        horizontal = Spacing.s3,
                        vertical = Spacing.s1,
                    ),
                )
            }
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────

private val samplePromos = listOf(
    PromoItem("1", "Diskon 50% di Merchant Pilihan", "Gunakan QRIS BCA untuk mendapatkan potongan setengah harga."),
    PromoItem("2", "Cashback 20% di Restoran", "Nikmati makan siang lebih hemat dengan Kartu Kredit BCA."),
    PromoItem("3", "Promo Tiket Pesawat", "Beli tiket liburanmu sekarang, cicilan 0% hingga 12 bulan."),
)

@Preview(showBackground = true, name = "Beranda - Default")
@Composable
private fun BerandaScreenPreview() {
    BcaMobileTheme {
        BerandaScreen(
            state = BerandaUiState(
                userName = "Muhamad Ardan Prayogi",
                promoItems = samplePromos,
            ),
            onToggleBalance = {},
            onIsiSaldo = {},
            onMutasi = {},
            onQuickAction = {},
            onPromoClick = {},
            onLihatSemuaPromo = {},
            onNotificationClick = {},
            onProfileClick = {},

            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Beranda - Balance Visible")
@Composable
private fun BerandaBalanceVisiblePreview() {
    BcaMobileTheme {
        BerandaScreen(
            state = BerandaUiState(
                userName = "Muhamad Ardan Prayogi",
                isBalanceVisible = true,
                promoItems = samplePromos,
            ),
            onToggleBalance = {},
            onIsiSaldo = {},
            onMutasi = {},
            onQuickAction = {},
            onPromoClick = {},
            onLihatSemuaPromo = {},
            onNotificationClick = {},
            onProfileClick = {},

            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Beranda - Dark")
@Composable
private fun BerandaScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        BerandaScreen(
            state = BerandaUiState(
                userName = "Muhamad Ardan Prayogi",
                promoItems = samplePromos,
            ),
            onToggleBalance = {},
            onIsiSaldo = {},
            onMutasi = {},
            onQuickAction = {},
            onPromoClick = {},
            onLihatSemuaPromo = {},
            onNotificationClick = {},
            onProfileClick = {},

            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Beranda - Error")
@Composable
private fun BerandaScreenErrorPreview() {
    BcaMobileTheme {
        BerandaScreen(
            state = BerandaUiState(
                userName = "Muhamad Ardan Prayogi",
                errorMessage = "Koneksi gagal. Periksa jaringan Anda.",
                promoItems = samplePromos,
            ),
            onToggleBalance = {},
            onIsiSaldo = {},
            onMutasi = {},
            onQuickAction = {},
            onPromoClick = {},
            onLihatSemuaPromo = {},
            onNotificationClick = {},
            onProfileClick = {},

            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Beranda - Empty Promo")
@Composable
private fun BerandaEmptyPromoPreview() {
    BcaMobileTheme {
        BerandaScreen(
            state = BerandaUiState(
                userName = "Muhamad Ardan Prayogi",
            ),
            onToggleBalance = {},
            onIsiSaldo = {},
            onMutasi = {},
            onQuickAction = {},
            onPromoClick = {},
            onLihatSemuaPromo = {},
            onNotificationClick = {},
            onProfileClick = {},

            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Beranda - Narrow", widthDp = 360)
@Composable
private fun BerandaScreenNarrowPreview() {
    BcaMobileTheme {
        BerandaScreen(
            state = BerandaUiState(
                userName = "Muhammad Alexander Jonathan Doe bin Abdullah",
                promoItems = samplePromos,
            ),
            onToggleBalance = {},
            onIsiSaldo = {},
            onMutasi = {},
            onQuickAction = {},
            onPromoClick = {},
            onLihatSemuaPromo = {},
            onNotificationClick = {},
            onProfileClick = {},

            onRetry = {},
        )
    }
}