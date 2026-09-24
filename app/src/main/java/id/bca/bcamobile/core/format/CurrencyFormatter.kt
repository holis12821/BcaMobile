package id.bca.bcamobile.core.format

import java.text.NumberFormat
import java.util.Locale

/**
 * Satu-satunya tempat rupiah diformat. Nominal dari server berupa angka bulat;
 * pemformatan tidak boleh tersebar di composable atau ViewModel masing-masing.
 */
object CurrencyFormatter {

    private val LOCALE_ID = Locale.forLanguageTag("id-ID")
    private const val PREFIX = "Rp"

    private val formatter: NumberFormat = NumberFormat.getNumberInstance(LOCALE_ID).apply {
        maximumFractionDigits = 0
    }

    /** `15750000` -> `Rp15.750.000`. */
    fun rupiah(amount: Long): String = PREFIX + formatter.format(amount)

    /** Bentuk bertanda untuk mutasi: `-Rp1.500.000` atau `+Rp5.000.000`. */
    fun signedRupiah(amount: Long, isCredit: Boolean): String =
        (if (isCredit) "+" else "-") + rupiah(amount)

    /** Untuk input pengguna: membuang semua karakter selain digit. */
    fun parseAmount(input: String): Long = input.filter(Char::isDigit).toLongOrNull() ?: 0L
}
