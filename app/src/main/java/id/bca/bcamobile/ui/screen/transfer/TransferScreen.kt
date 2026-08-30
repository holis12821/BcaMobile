package id.bca.bcamobile.ui.screen.transfer

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.Spacing

// ── State ───────────────────────────────────────────────────────────────

data class TransferUiState(
    val balance: String = "Rp 12.500.000",
    val isBalanceVisible: Boolean = true,
    val accountNumber: String = "1234 5678 90",
    val searchQuery: String = "",
    val recentTransfers: List<RecentTransferItem> = emptyList(),
)

data class RecentTransferItem(
    val id: String,
    val name: String,
    val bankAccount: String,
    val initial: Char,
)

enum class TransferType(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    ANTAR_REKENING(R.string.transfer_antar_rekening, R.drawable.ic_wallet),
    ANTAR_BANK(R.string.transfer_antar_bank, R.drawable.ic_account_balance),
    VIRTUAL_ACCOUNT(R.string.transfer_virtual_account, R.drawable.ic_qr_scan),
    SAKUKU(R.string.transfer_sakuku, R.drawable.ic_credit_card),
}

// ── Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    state: TransferUiState,
    onBackClick: () -> Unit,
    onToggleBalance: () -> Unit,
    onCopyAccount: () -> Unit,
    onSearchChange: (String) -> Unit,
    onTransferTypeClick: (TransferType) -> Unit,
    onRecentTransferClick: (RecentTransferItem) -> Unit,
    onLihatSemua: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.transfer_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_transfer_back),
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
                        Icon(
                            painter = painterResource(R.drawable.ic_contactless),
                            contentDescription = stringResource(R.string.cd_profile),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s4),
                verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            ) {
                BalanceCard(
                    balance = state.balance,
                    isBalanceVisible = state.isBalanceVisible,
                    accountNumber = state.accountNumber,
                    onToggleBalance = onToggleBalance,
                    onCopyAccount = onCopyAccount,
                )

                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = onSearchChange,
                )

                TransferTypeGrid(
                    onTypeClick = onTransferTypeClick,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            RecentTransferSection(
                transfers = state.recentTransfers,
                onTransferClick = onRecentTransferClick,
                onLihatSemua = onLihatSemua,
                modifier = Modifier.padding(horizontal = Spacing.s4),
            )
        }
    }
}

// ── Balance Card ────────────────────────────────────────────────────────

@Composable
private fun BalanceCard(
    balance: String,
    isBalanceVisible: Boolean,
    accountNumber: String,
    onToggleBalance: () -> Unit,
    onCopyAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShape.R6)
            .background(MaterialTheme.colorScheme.primary)
            .padding(Spacing.s5),
    ) {
        // Decorative circles
        Box(
            modifier = Modifier
                .size(Spacing.s10)
                .offset(x = Spacing.s10, y = -Spacing.s10)
                .clip(AppShape.Full)
                .background(AppColor.Neutral100.copy(alpha = AppAlpha.A10))
                .blur(Spacing.s5)
                .align(Alignment.TopEnd),
        )

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Text(
                text = stringResource(R.string.transfer_saldo_efektif),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColor.Primary200,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                Text(
                    text = if (isBalanceVisible) balance
                    else stringResource(R.string.beranda_balance_hidden),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                IconButton(onClick = onToggleBalance) {
                    Icon(
                        painter = painterResource(
                            if (isBalanceVisible) R.drawable.ic_visibility_off
                            else R.drawable.ic_visibility,
                        ),
                        contentDescription = stringResource(R.string.cd_toggle_balance),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            HorizontalDivider(
                color = AppColor.Neutral100.copy(alpha = AppAlpha.A20),
                modifier = Modifier.padding(top = Spacing.s2),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Spacing.s3),
            ) {
                Text(
                    text = stringResource(R.string.transfer_no_rekening),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColor.Primary200,
                )
                Text(
                    text = accountNumber,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(start = Spacing.s2),
                )
                Box(modifier = Modifier.weight(1f))
                IconButton(onClick = onCopyAccount) {
                    Icon(
                        painter = painterResource(R.drawable.ic_content_copy),
                        contentDescription = stringResource(R.string.cd_copy_rekening),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

// ── Search Bar ──────────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = stringResource(R.string.transfer_search_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        singleLine = true,
        shape = AppShape.R6,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = modifier.fillMaxWidth(),
    )
}

// ── Quick Actions Grid ──────────────────────────────────────────────────

@Composable
private fun TransferTypeGrid(
    onTypeClick: (TransferType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        TransferType.entries.forEach { type ->
            TransferTypeButton(
                type = type,
                onClick = { onTypeClick(type) },
            )
        }
    }
}

@Composable
private fun TransferTypeButton(
    type: TransferType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier
            .clip(AppShape.R4)
            .clickable(onClick = onClick)
            .padding(Spacing.s2),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AppSize.MinTouchTarget)
                .clip(AppShape.R7)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Icon(
                painter = painterResource(type.iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(type.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Recent Transfers ────────────────────────────────────────────────────

@Composable
private fun RecentTransferSection(
    transfers: List<RecentTransferItem>,
    onTransferClick: (RecentTransferItem) -> Unit,
    onLihatSemua: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.transfer_terakhir),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.transfer_lihat_semua),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(AppShape.R2)
                    .clickable(onClick = onLihatSemua)
                    .padding(Spacing.s1),
            )
        }

        transfers.forEach { item ->
            RecentTransferItemRow(
                item = item,
                onClick = { onTransferClick(item) },
            )
        }
    }
}

@Composable
private fun RecentTransferItemRow(
    item: RecentTransferItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShape.R6)
            .border(
                width = Spacing.s0,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = AppShape.R6,
            )
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.s3),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
    ) {
        // Avatar
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AppSize.MinTouchTarget)
                .clip(AppShape.Full)
                .background(MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Text(
                text = item.initial.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }

        // Name + bank
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.s0),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = item.bankAccount,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AppAlpha.A50),
        )
    }
}