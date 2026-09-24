package id.bca.bcamobile.core.network

import androidx.annotation.StringRes

/**
 * Teks error yang belum di-resolve ke String.
 *
 * Pesan yang kita tulis sendiri wajib lewat strings.xml ([Res]); pesan yang
 * datang dari server dipakai apa adanya ([Raw]) karena server sudah mengirim
 * bahasa Indonesia yang siap tampil.
 */
sealed interface ErrorText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : ErrorText
    data class Raw(val value: String) : ErrorText
}
