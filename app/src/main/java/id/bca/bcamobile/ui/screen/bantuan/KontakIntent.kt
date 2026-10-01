package id.bca.bcamobile.ui.screen.bantuan

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * Menyerahkan kanal kontak ke aplikasi sistem.
 *
 * Semua nilainya datang apa adanya dari `GET /content/contact-cs` — tidak ada
 * nomor atau tautan yang dirakit di sini, supaya mengubahnya tidak butuh rilis.
 *
 * Setiap niat dibungkus `runCatching`: perangkat tanpa aplikasi telepon, surel,
 * atau WhatsApp melempar `ActivityNotFoundException`, dan itu tidak boleh
 * menjatuhkan layar yang sedang dibutuhkan nasabah.
 */
internal fun Context.dial(phone: String) =
    launch(Intent(Intent.ACTION_DIAL, "tel:${phone.digitsAndPlus()}".toUri()))

/** `wa.me` memakai nomor tanpa spasi maupun tanda plus. */
internal fun Context.openWhatsApp(phone: String) =
    launch(Intent(Intent.ACTION_VIEW, "https://wa.me/${phone.digitsOnly()}".toUri()))

internal fun Context.sendEmail(email: String) =
    launch(Intent(Intent.ACTION_SENDTO, "mailto:$email".toUri()))

internal fun Context.openLink(url: String) =
    launch(Intent(Intent.ACTION_VIEW, url.toUri()))

private fun Context.launch(intent: Intent) {
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun String.digitsAndPlus(): String = filter { it.isDigit() || it == '+' }

private fun String.digitsOnly(): String = filter(Char::isDigit)
