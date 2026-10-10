package id.bca.bcamobile.core.liveness

/**
 * Satu gerakan yang diminta server dalam satu tantangan liveness.
 *
 * Daftarnya tetap (Q5 keputusan Phase 2), tapi **isi dan urutan tantangan bukan
 * milik client**: server memilih `BLINK` + dua gerakan kepala berbeda lalu
 * mengacak posisi ketiganya. Client hanya merender dan mendeteksi apa yang
 * dikirim. Urutan yang bisa ditebak — dulu selalu kedip lalu menoleh — bisa
 * dilewati dengan satu video rekaman.
 */
enum class LivenessAction {
    TURN_LEFT,
    TURN_RIGHT,
    LOOK_UP,
    LOOK_DOWN,
    BLINK,
    ;

    /** Gerakan kepala wajib kembali ke posisi netral sebelum langkah berikutnya. */
    val isHeadPose: Boolean get() = this != BLINK
}

/**
 * Tantangan yang diterbitkan server, sekali pakai.
 *
 * [nonce] terikat ke `device_id` di sisi server dan dikonsumsi secara atomik saat
 * verifikasi, jadi payload yang sama tidak bisa dikirim dua kali. [expiresAtMillis]
 * dihitung client dari `expires_at` response; mesin status berhenti sendiri saat
 * terlewat supaya tidak ada frame yang dikumpulkan untuk nonce yang sudah mati.
 *
 * Tidak ada konstruktor yang membuat tantangan dari sisi client — satu-satunya
 * jalan masuk adalah response `POST /onboarding/liveness/challenge`.
 */
data class LivenessChallenge(
    val challengeId: String,
    val nonce: String,
    val actions: List<LivenessAction>,
    val expiresAtMillis: Long,
) {
    val stepCount: Int get() = actions.size
}
