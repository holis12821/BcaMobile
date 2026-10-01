package id.bca.bcamobile.ui.screen.buka_rekening.common

import id.bca.bcamobile.R
import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.domain.onboarding.model.AlamatKtp
import id.bca.bcamobile.domain.onboarding.model.KtpOcrResult
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.domain.onboarding.model.PersonalData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// -- Error -> teks siap tampil -------------------------------------------------

/**
 * Pesan yang kita tulis sendiri diambil dari `strings.xml`; pesan bisnis dipakai
 * apa adanya dari server karena sudah berbahasa Indonesia dan spesifik konteks.
 */
fun ApiFailure.toErrorText(): ErrorText = when (this) {
    ApiFailure.Network -> ErrorText.Res(R.string.buka_rekening_error_network)
    ApiFailure.Timeout -> ErrorText.Res(R.string.buka_rekening_error_timeout)
    ApiFailure.Server -> ErrorText.Res(R.string.buka_rekening_error_server)
    ApiFailure.SessionExpired -> ErrorText.Res(R.string.buka_rekening_error_session_expired)
    ApiFailure.SessionNotFound ->
        ErrorText.Res(R.string.buka_rekening_error_session_not_found)
    ApiFailure.InvalidStep -> ErrorText.Res(R.string.buka_rekening_error_invalid_step)
    // Tidak terjadi di onboarding: endpoint /onboarding/* tanpa access token.
    ApiFailure.Unauthorized -> ErrorText.Res(R.string.error_unauthorized)
    is ApiFailure.RateLimited -> when {
        message.isNotBlank() -> ErrorText.Raw(message)
        retryAfterSeconds == null ->
            ErrorText.Res(R.string.buka_rekening_error_rate_limited_no_time)
        // Blokir percobaan OTP dan kuota kirim ulang yang habis bukan hal yang
        // sama bagi nasabah: yang satu menyuruh menunggu, yang lain menjelaskan
        // kenapa tombol kirim ulang mati.
        isOtpBlocked ->
            ErrorText.Res(R.string.buka_rekening_error_otp_blocked, listOf(retryAfterSeconds))
        else -> ErrorText.Res(R.string.buka_rekening_error_rate_limited, listOf(retryAfterSeconds))
    }
    is ApiFailure.Business -> when {
        message.isNotBlank() -> ErrorText.Raw(message)
        code == CODE_OTP_INVALID -> ErrorText.Res(R.string.buka_rekening_error_otp_invalid)
        code == CODE_OTP_EXPIRED -> ErrorText.Res(R.string.buka_rekening_error_otp_expired)
        code == CODE_OTP_DELIVERY_FAILED ->
            ErrorText.Res(R.string.buka_rekening_error_otp_delivery_failed)
        // Bentuk request yang salah adalah bug client, bukan kesalahan nasabah —
        // jangan tampilkan sebagai kode OTP yang keliru.
        code == CODE_VALIDATION -> ErrorText.Res(R.string.buka_rekening_error_validation)
        code == CODE_ENCRYPTION_FAILED ->
            ErrorText.Res(R.string.buka_rekening_error_credential_encryption)
        else -> ErrorText.Res(R.string.buka_rekening_error_unknown)
    }
    ApiFailure.Unknown -> ErrorText.Res(R.string.buka_rekening_error_unknown)
}

private const val CODE_OTP_INVALID = "OTP_INVALID"
private const val CODE_OTP_EXPIRED = "OTP_EXPIRED"
private const val CODE_OTP_DELIVERY_FAILED = "OTP_DELIVERY_FAILED"
private const val CODE_VALIDATION = "VALIDATION_ERROR"
private const val CODE_ENCRYPTION_FAILED = "CRED_ENCRYPTION_FAILED"

// -- OCR -> data pribadi -------------------------------------------------------

fun String.toJenisKelamin(): JenisKelamin? = when (uppercase(Locale.ROOT)) {
    WIRE_LAKI_LAKI -> JenisKelamin.LAKI_LAKI
    WIRE_PEREMPUAN -> JenisKelamin.PEREMPUAN
    else -> null
}

fun JenisKelamin.toWire(): String = when (this) {
    JenisKelamin.LAKI_LAKI -> WIRE_LAKI_LAKI
    JenisKelamin.PEREMPUAN -> WIRE_PEREMPUAN
}

private const val WIRE_LAKI_LAKI = "LAKI_LAKI"
private const val WIRE_PEREMPUAN = "PEREMPUAN"

/**
 * Menyusun payload `personal_data` dari hasil OCR.
 *
 * Pekerjaan, penghasilan, dan sumber dana masih memakai nilai default karena
 * layar Data Pribadi belum punya callback untuk tiga dropdown itu — begitu
 * callback-nya ada, ambil dari state alih-alih konstanta di sini.
 */
fun KtpOcrResult.toPersonalData(
    jenisKelamin: JenisKelamin?,
    alamatDomisiliSama: Boolean,
    nomorHp: String = "",
    email: String = "",
    pekerjaan: String = DEFAULT_PEKERJAAN,
    penghasilanPerBulan: String = DEFAULT_PENGHASILAN,
    sumberDanaUtama: String = DEFAULT_SUMBER_DANA,
): PersonalData = PersonalData(
    nik = extracted.nik,
    namaLengkap = extracted.namaLengkap,
    tempatLahir = extracted.tempatLahir,
    tanggalLahir = extracted.tanggalLahir,
    jenisKelamin = jenisKelamin?.toWire() ?: extracted.jenisKelamin,
    alamatKtp = AlamatKtp(
        alamatLengkap = extracted.alamat,
        rtRw = extracted.rtRw,
        kodePos = "",
        kelurahan = extracted.kelurahan,
        kecamatan = extracted.kecamatan,
        kota = extracted.kota,
        provinsi = extracted.provinsi,
    ),
    alamatDomisiliSama = alamatDomisiliSama,
    pekerjaan = pekerjaan,
    penghasilanPerBulan = penghasilanPerBulan,
    sumberDanaUtama = sumberDanaUtama,
    nomorHp = nomorHp,
    email = email,
)

const val DEFAULT_PEKERJAAN = "KARYAWAN_SWASTA"
const val DEFAULT_PENGHASILAN = "10_20_JUTA"
const val DEFAULT_SUMBER_DANA = "GAJI"

// -- Validasi kredensial -------------------------------------------------------

/** Aturan sisi client sebelum kredensial dienkripsi dan dikirim. */
fun BukaRekeningFlowState.isCredentialValid(): Boolean =
    accessCode.isValidAccessCode() &&
        accessCode == confirmAccessCode &&
        pin.isValidPin() &&
        pin == confirmPin &&
        pin != accessCode

fun String.isValidAccessCode(): Boolean =
    length == CREDENTIAL_LENGTH &&
        all { it.isLetterOrDigit() } &&
        !isAllSameChar() &&
        !isSequential()

fun String.isValidPin(): Boolean =
    length == CREDENTIAL_LENGTH &&
        all { it.isDigit() } &&
        !isAllSameChar() &&
        !isSequential()

fun String.isAllSameChar(): Boolean = isNotEmpty() && all { it == first() }

/** Menolak deret naik maupun turun, mis. `123456` dan `654321`. */
fun String.isSequential(): Boolean {
    if (length < 2) return false
    val ascending = windowed(2).all { it[1].code - it[0].code == 1 }
    val descending = windowed(2).all { it[0].code - it[1].code == 1 }
    return ascending || descending
}

const val CREDENTIAL_LENGTH = 6

// -- Format tampilan -----------------------------------------------------------

private val DISPLAY_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", INDONESIAN)
private val SHORT_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy", INDONESIAN)

/** `1995-04-21` menjadi `21 April 1995`; nilai yang tidak terbaca dikembalikan apa adanya. */
fun String.toDisplayDate(): String = runCatching {
    LocalDate.parse(this).format(DISPLAY_DATE)
}.getOrDefault(this)

/** `1995-04-21` menjadi `21-04-1995`, format yang dipakai pratinjau e-KTP. */
fun String.toKtpDate(): String = runCatching {
    LocalDate.parse(this).format(SHORT_DATE)
}.getOrDefault(this)

/** `3174082104950001` menjadi `3174 0821 0495 0001`. */
fun String.groupNik(): String = chunked(NIK_GROUP_SIZE).joinToString(" ")

private const val NIK_GROUP_SIZE = 4

/** Merangkai potongan alamat, melewati bagian yang kosong. */
fun joinNonBlank(vararg parts: String, separator: String = ", "): String =
    parts.filter { it.isNotBlank() }.joinToString(separator)
