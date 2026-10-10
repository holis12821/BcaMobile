package id.bca.bcamobile.ui.screen.buka_rekening.common

import id.bca.bcamobile.R
import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.core.network.ApiFailure
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
    // Pesan server lebih spesifik daripada teks cadangan kita: ia menyebut S&K sudah
    // diperbarui dan meminta versi baru dibaca, bukan menyuruh mencoba lagi.
    is ApiFailure.TncOutdated -> when {
        message.isNotBlank() -> ErrorText.Raw(message)
        else -> ErrorText.Res(R.string.buka_rekening_error_tnc_outdated)
    }
    is ApiFailure.Business -> when {
        message.isNotBlank() -> ErrorText.Raw(message)
        code == OnboardingErrorCode.OTP_INVALID ->
            ErrorText.Res(R.string.buka_rekening_error_otp_invalid)
        code == OnboardingErrorCode.OTP_EXPIRED ->
            ErrorText.Res(R.string.buka_rekening_error_otp_expired)
        code == OnboardingErrorCode.OTP_DELIVERY_FAILED ->
            ErrorText.Res(R.string.buka_rekening_error_otp_delivery_failed)
        // Nonce yang sudah dipakai bukan kesalahan nasabah dan bukan hal yang bisa
        // diperbaiki dengan mengirim payload yang sama: verifikasi harus dimulai ulang.
        code == OnboardingErrorCode.LIVENESS_CHALLENGE_INVALID ->
            ErrorText.Res(R.string.buka_rekening_error_liveness_replay)
        code == OnboardingErrorCode.LIVENESS_INTEGRITY_FAILED ->
            ErrorText.Res(R.string.buka_rekening_error_liveness_integrity)
        code == OnboardingErrorCode.LIVENESS_BLOCKED ||
            code == OnboardingErrorCode.LIVENESS_ESCALATED ->
            ErrorText.Res(R.string.buka_rekening_biometrik_blocked)
        // Bentuk request yang salah adalah bug client, bukan kesalahan nasabah —
        // jangan tampilkan sebagai kode OTP yang keliru.
        code == CODE_VALIDATION -> ErrorText.Res(R.string.buka_rekening_error_validation)
        code == CODE_ENCRYPTION_FAILED ->
            ErrorText.Res(R.string.buka_rekening_error_credential_encryption)
        else -> ErrorText.Res(R.string.buka_rekening_error_unknown)
    }
    ApiFailure.Unknown -> ErrorText.Res(R.string.buka_rekening_error_unknown)
}

private const val CODE_VALIDATION = "VALIDATION_ERROR"
private const val CODE_ENCRYPTION_FAILED = "CRED_ENCRYPTION_FAILED"

/**
 * Kode bisnis yang dibutuhkan lebih dari satu ViewModel langkah, jadi tidak boleh
 * hidup sebagai literal privat di masing-masing — dua salinan cepat berbeda ejaan.
 */
object OnboardingErrorCode {
    const val OTP_INVALID = "OTP_INVALID"
    const val OTP_EXPIRED = "OTP_EXPIRED"

    /**
     * HTTP 503, tapi bukan kegagalan transport: SMS-nya yang gagal berangkat, sementara
     * kode OTP tetap terbit dan sah dan **server sudah maju ke langkah berikutnya**.
     * Lihat `ApiCaller.FINAL_5XX_CODES`.
     */
    const val OTP_DELIVERY_FAILED = "OTP_DELIVERY_FAILED"

    /** Nonce sudah dipakai, kedaluwarsa, atau bukan milik perangkat ini. */
    const val LIVENESS_CHALLENGE_INVALID = "LIVENESS_CHALLENGE_INVALID"

    /** Verdict Play Integrity tidak memenuhi kebijakan produksi. */
    const val LIVENESS_INTEGRITY_FAILED = "LIVENESS_INTEGRITY_FAILED"

    /**
     * Liveness mandiri dihentikan untuk sesi ini (6 kegagalan dalam 24 jam).
     * Pemulihannya bukan mencoba lagi, jadi tombol coba lagi harus mati.
     */
    const val LIVENESS_BLOCKED = "LIVENESS_BLOCKED"

    /** Sama seperti [LIVENESS_BLOCKED], tapi server sudah mengarahkan ke video call. */
    const val LIVENESS_ESCALATED = "LIVENESS_ESCALATED_TO_VIDEO_CALL"
}

/** Benar hanya untuk kode bisnis [code] — kegagalan transport tidak ikut cocok. */
fun ApiFailure.isBusinessCode(code: String): Boolean =
    this is ApiFailure.Business && this.code == code

// -- Jenis kelamin <-> wire -------------------------------------------------------

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
