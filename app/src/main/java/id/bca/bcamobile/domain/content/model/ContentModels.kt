package id.bca.bcamobile.domain.content.model

/**
 * Satu kategori FAQ.
 *
 * [key] dipetakan client ke ikon; [title] ikut dikirim server **supaya key baru
 * yang belum dikenal build ini tetap punya teks untuk dicetak.** Jatuhkan ke
 * [title] — jangan mencetak key mentah dan jangan menyembunyikan kategorinya.
 */
data class HelpCategory(
    val key: String,
    val title: String,
    val items: List<HelpItem>,
)

data class HelpItem(
    val question: String,
    val answer: String,
)

/**
 * Kontak customer service. Semua sudah berbentuk teks siap tampil — server
 * yang memformat, supaya mengubah aturannya tidak butuh rilis aplikasi.
 */
data class ContactCs(
    val phone: String,
    /** Nomor bebas pulsa untuk dari luar negeri. */
    val phoneFree: String,
    val whatsapp: String,
    val email: String,
    val chatUrl: String,
    val hours: String,
)
