package id.bca.bcamobile.core.network

/**
 * Kegagalan panggilan API yang sudah diklasifikasi dari HTTP status + error code backend.
 *
 * Dipakai seluruh domain, bukan hanya onboarding. Tabel kode:
 * `bca-mobile-api/docs/01-API-SPECIFICATION.md` dan `bca-mobile-api/docs/06-BUKA-REKENING-API-SPEC.md`.
 */
sealed interface ApiFailure {

    /** Tidak ada koneksi, DNS gagal, atau socket putus. Boleh retry. */
    data object Network : ApiFailure

    /** Request melewati batas waktu. Boleh retry. */
    data object Timeout : ApiFailure

    /** HTTP 5xx. Boleh retry terbatas. */
    data object Server : ApiFailure

    /**
     * HTTP 401 yang tidak tertolong refresh token — sesi login habis.
     * Penanganan: bersihkan token, kembali ke layar masuk.
     *
     * Tidak pernah muncul di flow onboarding: endpoint onboarding tidak memakai access token.
     */
    data object Unauthorized : ApiFailure

    /** `ONBOARDING_SESSION_EXPIRED` — sesi 24 jam habis, flow harus mulai ulang. */
    data object SessionExpired : ApiFailure

    /** `ONBOARDING_NOT_FOUND` — session_id tersimpan sudah tidak dikenal server. */
    data object SessionNotFound : ApiFailure

    /** `ONBOARDING_INVALID_STEP` — client mencoba melompati step. */
    data object InvalidStep : ApiFailure

    /**
     * `RATE_LIMIT_EXCEEDED` / `OTP_BLOCKED` — tunggu [retryAfterSeconds] sebelum coba lagi.
     *
     * [isOtpBlocked] membedakan dua hal yang perilaku UI-nya berbeda: blokir percobaan
     * OTP mematikan input **dan** tombol kirim ulang, sedangkan kuota kirim ulang yang
     * habis hanya mematikan tombolnya — kode terakhir masih sah.
     *
     * [message] ikut dibawa karena server sudah mengirim kalimat berbahasa Indonesia
     * yang lebih spesifik daripada teks cadangan kita.
     */
    data class RateLimited(
        val retryAfterSeconds: Int?,
        val isOtpBlocked: Boolean = false,
        val message: String = "",
    ) : ApiFailure

    /**
     * `TNC_VERSION_OUTDATED` — S&K sudah diperbarui sejak layar itu dibuka.
     *
     * [currentVersion] dari `details.current_version`, dan bisa kosong kalau server tidak
     * mengirimkannya. Sengaja **bukan** [Business]: pemulihannya bukan mengulang request,
     * tapi memuat ulang teks S&K dan meminta nasabah menyetujui versi baru. Sebagai
     * `Business`, layar akan menawarkan "coba lagi" yang mengirim versi lama yang sama
     * dan dijamin gagal — nasabah terkurung di lingkaran itu.
     *
     * [message] dari server sudah berbahasa Indonesia dan lebih spesifik daripada teks
     * cadangan kita, sama seperti [RateLimited].
     */
    data class TncOutdated(
        val currentVersion: String,
        val message: String = "",
    ) : ApiFailure

    /**
     * Error bisnis spesifik endpoint (OCR_PHOTO_BLURRY, AUTH_INVALID_PIN, dst).
     * [message] berasal dari server dan sudah berbahasa Indonesia.
     */
    data class Business(val code: String, val message: String) : ApiFailure

    /** Response tidak sesuai kontrak, atau kegagalan yang tidak terklasifikasi. */
    data object Unknown : ApiFailure

    /** true jika masuk akal untuk menampilkan tombol "Coba Lagi". */
    val isRetryable: Boolean
        get() = this is Network || this is Timeout || this is Server

    /** true jika flow onboarding tidak bisa dilanjutkan dan harus mulai dari awal. */
    val isFatalForSession: Boolean
        get() = this is SessionExpired || this is SessionNotFound
}
