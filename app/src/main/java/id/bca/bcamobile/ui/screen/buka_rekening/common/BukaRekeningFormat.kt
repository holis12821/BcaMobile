package id.bca.bcamobile.ui.screen.buka_rekening.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.bca.bcamobile.core.network.ErrorText
import java.util.Locale

/** Pembantu tampilan yang dipakai mapper `UiState` di banyak folder layar. */

val INDONESIAN: Locale = Locale.forLanguageTag("id-ID")

/** `500000` menjadi `500.000`, pemisah ribuan sesuai lokal Indonesia. */
fun formatAmount(value: Long): String = String.format(INDONESIAN, "%,d", value)

fun formatAccuracy(value: Double): String = String.format(INDONESIAN, "%.1f", value)

@Composable
fun ErrorText.resolve(): String = when (this) {
    is ErrorText.Res -> stringResource(id, *args.toTypedArray())
    is ErrorText.Raw -> value
}
