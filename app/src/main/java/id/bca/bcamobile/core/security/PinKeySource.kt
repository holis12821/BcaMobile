package id.bca.bcamobile.core.security

/**
 * Sumber jaringan untuk kunci publik PIN.
 *
 * Antarmukanya tinggal di `core` dan implementasinya di `data/auth` supaya
 * [PinKeyProvider] tidak perlu mengenal Retrofit — arah ketergantungannya tetap
 * data → core, bukan sebaliknya.
 */
fun interface PinKeySource {

    /** @return kunci aktif dari server, atau null bila permintaan gagal. */
    suspend fun fetch(): PinKey?
}
