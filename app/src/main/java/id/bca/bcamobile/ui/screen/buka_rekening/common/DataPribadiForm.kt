package id.bca.bcamobile.ui.screen.buka_rekening.common

import androidx.annotation.StringRes
import id.bca.bcamobile.R
import id.bca.bcamobile.domain.onboarding.model.AlamatKtp
import id.bca.bcamobile.domain.onboarding.model.KtpData
import id.bca.bcamobile.domain.onboarding.model.PersonalData
import java.time.LocalDate

/**
 * Isian layar Data Pribadi yang boleh disunting nasabah.
 *
 * Dipisah dari [BukaRekeningFlowState] karena bentuknya mengikuti payload
 * `personal_data` (`06-BUKA-REKENING-API-SPEC.md` §3a), bukan mengikuti tampilan:
 * kelurahan dan kecamatan berdiri sendiri walau desain menampilkannya berdampingan,
 * dan tanggal lahir disimpan `yyyy-MM-dd` walau yang terbaca `21 April 1995`.
 *
 * NIK dan nama lengkap sengaja tidak ada di sini — keduanya wajib sama dengan
 * hasil OCR, jadi diambil langsung dari [KtpData] saat payload disusun.
 *
 * Seluruh isinya PII dan hanya hidup di memory (aturan #5 `CLAUDE.md`).
 */
data class DataPribadiForm(
    val tempatLahir: String = "",
    /** Format wire `yyyy-MM-dd`; kosong berarti nasabah belum memilih tanggal. */
    val tanggalLahir: String = "",
    val alamatLengkap: String = "",
    /** Format wire `003/005`, tanpa spasi. */
    val rtRw: String = "",
    val kodePos: String = "",
    val kelurahan: String = "",
    val kecamatan: String = "",
    val kota: String = "",
    val provinsi: String = "",
    val nomorHp: String = "",
    val email: String = "",
    val pekerjaan: Pekerjaan = Pekerjaan.KARYAWAN_SWASTA,
    val penghasilan: Penghasilan = Penghasilan.SEPULUH_SAMPAI_20_JUTA,
    val sumberDana: SumberDana = SumberDana.GAJI,
) {
    companion object {
        /**
         * Prefill dari hasil pindai e-KTP.
         *
         * Kode pos, nomor HP, dan email tidak ikut: ketiganya tidak ada di e-KTP,
         * jadi memang harus diisi nasabah.
         */
        fun from(data: KtpData): DataPribadiForm = DataPribadiForm(
            tempatLahir = data.tempatLahir,
            tanggalLahir = data.tanggalLahir,
            alamatLengkap = data.alamat,
            rtRw = data.rtRw,
            kelurahan = data.kelurahan,
            kecamatan = data.kecamatan,
            kota = data.kota,
            provinsi = data.provinsi,
        )
    }
}

/** Field teks yang bisa diketik; dipakai sebagai alamat event dari layar. */
enum class DataPribadiField {
    TEMPAT_LAHIR,
    ALAMAT_LENGKAP,
    RT_RW,
    KODE_POS,
    KELURAHAN,
    KECAMATAN,
    KOTA,
    PROVINSI,
    NOMOR_HP,
    EMAIL,
}

/**
 * Menyalin form dengan satu field berganti isi.
 *
 * Penyaringan karakter dilakukan di sini, bukan di layar: layar hanya meneruskan
 * apa yang diketik, dan aturan "kode pos itu lima angka" adalah aturan data.
 */
fun DataPribadiForm.withField(field: DataPribadiField, value: String): DataPribadiForm =
    when (field) {
        DataPribadiField.TEMPAT_LAHIR -> copy(tempatLahir = value.take(MAX_TEXT))
        DataPribadiField.ALAMAT_LENGKAP -> copy(alamatLengkap = value.take(MAX_ALAMAT))
        DataPribadiField.RT_RW -> copy(rtRw = value.filter { it.isDigit() || it == '/' }.take(MAX_RT_RW))
        DataPribadiField.KODE_POS -> copy(kodePos = value.filter { it.isDigit() }.take(KODE_POS_LENGTH))
        DataPribadiField.KELURAHAN -> copy(kelurahan = value.take(MAX_TEXT))
        DataPribadiField.KECAMATAN -> copy(kecamatan = value.take(MAX_TEXT))
        DataPribadiField.KOTA -> copy(kota = value.take(MAX_TEXT))
        DataPribadiField.PROVINSI -> copy(provinsi = value.take(MAX_TEXT))
        DataPribadiField.NOMOR_HP ->
            copy(nomorHp = value.filter { it.isDigit() || it == '+' }.take(MAX_NOMOR_HP))
        DataPribadiField.EMAIL -> copy(email = value.filterNot { it.isWhitespace() }.take(MAX_TEXT))
    }

/**
 * Pesan error field, atau `null` bila isinya sah.
 *
 * Validasi dijalankan di client supaya nasabah tidak menunggu perjalanan jaringan
 * untuk tahu nomornya salah ketik — server tetap memeriksa ulang dan membalas
 * `PERSONAL_DATA_INVALID_PHONE` / `PERSONAL_DATA_INVALID_EMAIL`.
 */
@StringRes
fun DataPribadiForm.errorOf(field: DataPribadiField): Int? = when (field) {
    DataPribadiField.TEMPAT_LAHIR -> tempatLahir.blankError()
    DataPribadiField.ALAMAT_LENGKAP -> alamatLengkap.blankError()
    DataPribadiField.RT_RW -> when {
        rtRw.isBlank() -> R.string.buka_rekening_dp_error_wajib
        !RT_RW_PATTERN.matches(rtRw) -> R.string.buka_rekening_dp_error_rt_rw
        else -> null
    }
    DataPribadiField.KODE_POS -> when {
        kodePos.isBlank() -> R.string.buka_rekening_dp_error_wajib
        kodePos.length != KODE_POS_LENGTH -> R.string.buka_rekening_dp_error_kode_pos
        else -> null
    }
    DataPribadiField.KELURAHAN -> kelurahan.blankError()
    DataPribadiField.KECAMATAN -> kecamatan.blankError()
    DataPribadiField.KOTA -> kota.blankError()
    DataPribadiField.PROVINSI -> provinsi.blankError()
    DataPribadiField.NOMOR_HP -> when {
        nomorHp.isBlank() -> R.string.buka_rekening_dp_error_wajib
        nomorHp.toE164Indonesia() == null -> R.string.buka_rekening_dp_error_nomor_hp
        else -> null
    }
    DataPribadiField.EMAIL -> when {
        email.isBlank() -> R.string.buka_rekening_dp_error_wajib
        !EMAIL_PATTERN.matches(email) -> R.string.buka_rekening_dp_error_email
        else -> null
    }
}

/**
 * Tanggal lahir tidak punya field teks, jadi kesalahannya dilaporkan terpisah.
 *
 * Selain kosong, server menolak yang tidak terbaca sebagai ISO `yyyy-MM-dd` dan
 * yang di luar rentang wajar (`400 VALIDATION_ERROR`).
 */
@StringRes
fun DataPribadiForm.tanggalLahirError(): Int? {
    if (tanggalLahir.isBlank()) return R.string.buka_rekening_dp_error_wajib
    val tanggal = runCatching { LocalDate.parse(tanggalLahir) }.getOrNull()
        ?: return R.string.buka_rekening_dp_error_tanggal
    val hariIni = LocalDate.now()
    // Tanggal di masa depan tidak punya arti sebagai tanggal lahir. Batas 120
    // tahun adalah pemeriksaan kewajaran, bukan aturan umur minimum — yang
    // terakhir itu keputusan produk, bukan validasi bentuk data.
    if (tanggal.isAfter(hariIni) || tanggal.isBefore(hariIni.minusYears(MAX_USIA_TAHUN))) {
        return R.string.buka_rekening_dp_error_tanggal
    }
    return null
}

val DataPribadiForm.isValid: Boolean
    get() = DataPribadiField.entries.all { errorOf(it) == null } && tanggalLahirError() == null

@StringRes
private fun String.blankError(): Int? =
    if (isBlank()) R.string.buka_rekening_dp_error_wajib else null

/**
 * Menyusun payload `personal_data`.
 *
 * [nik] dan [namaLengkap] datang dari OCR, bukan dari form: spec mewajibkan
 * keduanya sama persis dengan hasil pindai, dan layar memang menampilkannya
 * sebagai field yang terkunci.
 */
fun DataPribadiForm.toPersonalData(
    nik: String,
    namaLengkap: String,
    jenisKelamin: JenisKelamin?,
    jenisKelaminKtp: String,
    alamatDomisiliSama: Boolean,
): PersonalData = PersonalData(
    nik = nik,
    namaLengkap = namaLengkap,
    tempatLahir = tempatLahir.trim(),
    tanggalLahir = tanggalLahir,
    jenisKelamin = jenisKelamin?.toWire() ?: jenisKelaminKtp,
    alamatKtp = AlamatKtp(
        alamatLengkap = alamatLengkap.trim(),
        rtRw = rtRw,
        kodePos = kodePos,
        kelurahan = kelurahan.trim(),
        kecamatan = kecamatan.trim(),
        kota = kota.trim(),
        provinsi = provinsi.trim(),
    ),
    alamatDomisiliSama = alamatDomisiliSama,
    pekerjaan = pekerjaan.wire,
    penghasilanPerBulan = penghasilan.wire,
    sumberDanaUtama = sumberDana.wire,
    nomorHp = nomorHp,
    email = email.trim(),
)

private const val MAX_TEXT = 100
private const val MAX_ALAMAT = 200
private const val MAX_RT_RW = 7
private const val MAX_NOMOR_HP = 15
private const val KODE_POS_LENGTH = 5

private const val MAX_USIA_TAHUN = 120L

private val RT_RW_PATTERN = Regex("""^\d{1,3}/\d{1,3}$""")

/**
 * Menyalin aturan gateway SMS backend (`internal/pkg/sms.NormalizePhone`):
 * satu-satunya rujukan yang benar, karena gateway itulah yang harus merutekan
 * pesan OTP-nya.
 *
 * Mengembalikan bentuk E.164, atau null kalau tidak ada operator Indonesia yang
 * bisa menerimanya. Dipakai untuk **memvalidasi**, bukan mengubah isi field —
 * yang dikirim tetap apa yang diketik nasabah, dan server menormalkannya sendiri.
 *
 * Menggantikan pola lama `^(+62|62|0)8\d{8,12}$`, yang salah di dua arah: menolak
 * `0812-3456-7890` dan `81234567890` yang sebenarnya sah, sekaligus meloloskan
 * nomor 14 digit, blok `080`, dan `+628012345678` yang pasti dijawab
 * `422 PERSONAL_DATA_INVALID_PHONE`.
 */
internal fun String.toE164Indonesia(): String? {
    val digits = filter { it.isDigit() }
    val national = when {
        digits.startsWith("62") -> "0" + digits.removePrefix("62")
        digits.startsWith("0") -> digits
        digits.startsWith("8") -> "0$digits"
        else -> return null
    }
    // 08 + blok operator + nomor pelanggan. Di luar 10..13 digit berarti telepon
    // rumah, salah ketik, atau tempelan yang terpotong.
    if (national.length !in NOMOR_HP_PANJANG) return null
    // Blok "080" tidak dialokasikan untuk seluler.
    if (!national.startsWith("08") || national[2] == '0') return null
    return "+62" + national.drop(1)
}

/**
 * Potongan nomor HP yang boleh tampil di layar OTP: empat digit depan dan empat
 * digit belakang. Bagian tengahnya tidak dibawa sama sekali, bukan disembunyikan.
 */
internal data class MaskedPhone(val prefix: String, val suffix: String)

/**
 * Menyamarkan nomor HP seperti `otp_sent_to` dari server — `0812****8889`
 * (`06-BUKA-REKENING-API-SPEC.md` §3a).
 *
 * Dipakai hanya sebagai **cadangan** saat server tidak mengirim `otp_sent_to`, yang
 * terjadi pada `personal-data` berjawaban `OTP_DELIVERY_FAILED`: statusnya 503 dan
 * body-nya tanpa `data`, padahal langkahnya sudah maju. Nomornya diambil dari form
 * karena itu memang nomor yang dikirim ke server sebagai tujuan OTP — jadi yang
 * tampil bukan tebakan, dan bukan PII baru: isinya lebih sedikit daripada yang
 * nasabah ketik sendiri satu layar sebelumnya.
 *
 * Dihitung dari bentuk nasional hasil [toE164Indonesia] supaya `+6281…`, `6281…`,
 * `0812-3456-…`, dan `81234…` menghasilkan samaran yang sama dengan milik server.
 */
internal fun String.maskedPhoneOrNull(): MaskedPhone? {
    val national = toE164Indonesia()?.let { "0" + it.removePrefix("+62") } ?: return null
    // Nomor sah selalu 10..13 digit, jadi cabang ini tidak terpakai — ada supaya
    // perubahan di NOMOR_HP_PANJANG tidak pernah membuat kedua potongan bertumpang.
    if (national.length < MASK_PREFIX_LENGTH + MASK_SUFFIX_LENGTH) return null
    return MaskedPhone(
        prefix = national.take(MASK_PREFIX_LENGTH),
        suffix = national.takeLast(MASK_SUFFIX_LENGTH),
    )
}

private const val MASK_PREFIX_LENGTH = 4
private const val MASK_SUFFIX_LENGTH = 4

private val NOMOR_HP_PANJANG = 10..13

private val EMAIL_PATTERN = Regex("""^[^@\s]+@[^@\s.]+(\.[^@\s.]+)+$""")
