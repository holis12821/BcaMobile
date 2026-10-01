package id.bca.bcamobile.core.security

/**
 * Kunci publik RSA yang dipakai mengenkripsi PIN dan kode akses.
 *
 * [keyId] ikut dibawa karena server memakainya untuk membedakan "PIN salah" dari
 * "kunci sudah dirotasi". Bisa null hanya untuk kunci cadangan dari asset, yang
 * tidak membawa penanda versi.
 */
data class PinKey(
    val pem: String,
    val keyId: String?,
)

/**
 * Hasil enkripsi satu rahasia.
 *
 * [keyId] adalah kunci yang **benar-benar dipakai** mengenkripsi [ciphertext] —
 * bukan kunci yang kebetulan aktif saat request dikirim. Mengirim yang kedua
 * justru menciptakan kegagalan yang hendak dicegah `encryption_key_id`.
 */
data class EncryptedPin(
    val ciphertext: String,
    val keyId: String?,
)
