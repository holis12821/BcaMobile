package id.bca.bcamobile.ui.screen.buka_rekening.antrean_video_call

import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState

/**
 * State layar Antrean Video Call.
 *
 * Nilai awalnya sengaja **kosong**, bukan contoh seperti `"A-042"`. Default yang berisi
 * angka masuk akal membuat layar tampak benar sebelum `POST video-call/queue` menjawab, dan
 * itu menyembunyikan kegagalan muat alih-alih menampilkannya.
 */
data class AntreanVideoCallUiState(
    val nomorAntrean: String = "",
    val jumlahAntreanDepan: Int = 0,
    val estimasiMenit: Int = 0,
    val petugasSiap: Boolean = false,
    /**
     * Kamera dan mikrofon sudah diizinkan.
     *
     * Dulu bernama `isKtpReady` dan bernilai `true` tetap, jadi centangnya selalu hijau
     * apa pun keadaannya — termasuk saat izin belum diberikan dan panggilan pasti gagal.
     * Sekarang diisi dari status izin sungguhan.
     */
    val isIzinMediaSiap: Boolean = false,
    /** Tiket antrean sudah terbit; selama belum, tombol tunggu tidak ada gunanya ditekan. */
    val isTiketSiap: Boolean = false,
) {
    /**
     * Penjadwalan ulang belum punya endpoint di `06-BUKA-REKENING-API-SPEC.md`, jadi
     * tombolnya dimatikan. Tombol hidup yang memanggil lambda kosong lebih buruk daripada
     * tombol mati: nasabah menekan dan menyimpulkan aplikasinya rusak.
     */
    val isJadwalkanTersedia: Boolean get() = false
}

fun BukaRekeningFlowState.toAntreanUiState(
    isIzinMediaSiap: Boolean,
): AntreanVideoCallUiState {
    val ticket = queue ?: return AntreanVideoCallUiState(isIzinMediaSiap = isIzinMediaSiap)
    return AntreanVideoCallUiState(
        nomorAntrean = ticket.queueNumber,
        jumlahAntreanDepan = ticket.position,
        estimasiMenit = ticket.estimatedWaitSeconds / SECONDS_PER_MINUTE,
        petugasSiap = ticket.position == 0,
        isIzinMediaSiap = isIzinMediaSiap,
        // `signaling_url` kosong berarti tiketnya tidak bisa dipakai menyambung, jadi
        // layar Video Call hanya akan menampilkan kegagalan.
        isTiketSiap = ticket.signalingUrl.isNotBlank(),
    )
}

private const val SECONDS_PER_MINUTE = 60
