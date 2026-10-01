package id.bca.bcamobile.ui.screen.akun

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.domain.card.model.BlockedReason
import id.bca.bcamobile.domain.card.model.DeliveryMethod
import id.bca.bcamobile.domain.card.model.ReplacementReason
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

/**
 * Pemilih alasan blokir dan alasan + metode kirim untuk penggantian kartu.
 *
 * **Tidak ada di artefak Stitch.** Desain hanya menggambar tombol "Blokir
 * Kartu" dan "Ganti Kartu"; kedua endpoint mewajibkan alasan, dan penggantian
 * juga mewajibkan metode pengiriman. Dialog ini dirakit dari komponen dan token
 * yang sudah ada — tidak ada nilai visual baru — mengikuti preseden layar
 * Konfirmasi QRIS. **Perlu direview pemilik desain.**
 *
 * Biaya penggantian sengaja tidak dicetak di sini: `fee` baru diketahui dari
 * respons `201`, dan menuliskan angka sebelum server menyebutkannya berarti
 * menjanjikan tarif yang bisa berbeda.
 */
@Composable
fun AksiKartuDialog(
    state: AksiKartuUiState,
    onBlockReasonSelected: (BlockedReason) -> Unit,
    onReplacementReasonSelected: (ReplacementReason) -> Unit,
    onDeliveryMethodSelected: (DeliveryMethod) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val aksi = state.aksi ?: return

    Surface(
        shape = AppShape.R7,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = StrokeWidth.w0,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppAlpha.A30),
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.s4),
    ) {
        Column(modifier = Modifier.padding(Spacing.s4)) {
            Text(
                text = stringResource(
                    when (aksi) {
                        AksiKartu.BLOKIR -> R.string.profil_kartu_blokir_judul
                        AksiKartu.GANTI -> R.string.profil_kartu_ganti_judul
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(Spacing.s2))

            Text(
                text = stringResource(
                    when (aksi) {
                        AksiKartu.BLOKIR -> R.string.profil_kartu_blokir_deskripsi
                        AksiKartu.GANTI -> R.string.profil_kartu_ganti_deskripsi
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.s4))

            when (aksi) {
                AksiKartu.BLOKIR -> {
                    PilihanLabel(textRes = R.string.profil_kartu_label_alasan)
                    BlockedReason.entries.forEach { reason ->
                        PilihanRow(
                            label = stringResource(reason.labelRes()),
                            isSelected = state.blockReason == reason,
                            onClick = { onBlockReasonSelected(reason) },
                        )
                    }
                }

                AksiKartu.GANTI -> {
                    PilihanLabel(textRes = R.string.profil_kartu_label_alasan)
                    ReplacementReason.entries.forEach { reason ->
                        PilihanRow(
                            label = stringResource(reason.labelRes()),
                            isSelected = state.replacementReason == reason,
                            onClick = { onReplacementReasonSelected(reason) },
                        )
                    }

                    Spacer(Modifier.height(Spacing.s3))
                    PilihanLabel(textRes = R.string.profil_kartu_label_pengiriman)
                    DeliveryMethod.entries
                        // Ambil di cabang disembunyikan setelah server menjawab
                        // CARD_DELIVERY_UNAVAILABLE untuk kartu ini.
                        .filter {
                            it != DeliveryMethod.BRANCH_PICKUP || state.isBranchPickupAvailable
                        }
                        .forEach { method ->
                            PilihanRow(
                                label = stringResource(method.labelRes()),
                                isSelected = state.deliveryMethod == method,
                                onClick = { onDeliveryMethodSelected(method) },
                            )
                        }
                }
            }

            if (state.message != null) {
                Spacer(Modifier.height(Spacing.s3))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(Spacing.s4))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.profil_kartu_batal))
                }
                Spacer(Modifier.height(Spacing.s2))
                TextButton(
                    onClick = onConfirm,
                    enabled = state.isConfirmEnabled,
                ) {
                    Text(text = stringResource(R.string.profil_kartu_lanjut))
                }
            }
        }
    }
}

@Composable
private fun PilihanLabel(
    textRes: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = Spacing.s1),
    )
}

@Composable
private fun PilihanRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppSize.MinTouchTarget)
            // selectable pada barisnya, bukan hanya pada RadioButton: target
            // sentuh 20dp terlalu kecil untuk pilihan sepenting ini.
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AksiKartuDialogBlokirPreview() {
    BcaMobileTheme {
        AksiKartuDialog(
            state = AksiKartuUiState(
                aksi = AksiKartu.BLOKIR,
                blockReason = BlockedReason.LOST,
            ),
            onBlockReasonSelected = {},
            onReplacementReasonSelected = {},
            onDeliveryMethodSelected = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AksiKartuDialogGantiPreview() {
    BcaMobileTheme {
        AksiKartuDialog(
            state = AksiKartuUiState(
                aksi = AksiKartu.GANTI,
                replacementReason = ReplacementReason.DAMAGED,
                deliveryMethod = DeliveryMethod.COURIER,
            ),
            onBlockReasonSelected = {},
            onReplacementReasonSelected = {},
            onDeliveryMethodSelected = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}
