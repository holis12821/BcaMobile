package id.bca.bcamobile.session

sealed interface SessionState {
    /** Aplikasi belum selesai membaca penyimpanan. Tampilkan splash sistem. */
    data object Loading : SessionState

    /** Belum pernah login, atau sudah logout. */
    data object LoggedOut : SessionState

    /** Sesi ada tapi perlu autentikasi ulang (timeout / kembali dari background). */
    data object Locked : SessionState

    /** Terautentikasi penuh. */
    data class Authenticated(val displayName: String) : SessionState
}