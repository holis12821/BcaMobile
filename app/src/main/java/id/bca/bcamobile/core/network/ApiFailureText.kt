package id.bca.bcamobile.core.network

/**
 * Pesan siap tampil dari sebuah kegagalan, atau null bila tidak ada pesan yang layak.
 *
 * Hanya error bisnis yang punya kalimat dari server — dan kalimat itu sudah berbahasa
 * Indonesia, jadi dipakai apa adanya. Kegagalan teknis (jaringan, timeout, 5xx)
 * sengaja mengembalikan null supaya layar memakai teks dari `strings.xml`.
 */
fun ApiFailure.messageOrNull(): String? = when (this) {
    is ApiFailure.Business -> message.takeIf { it.isNotBlank() }
    else -> null
}
