package id.bca.bcamobile.session

import id.bca.bcamobile.domain.auth.AuthRepository
import id.bca.bcamobile.ui.navigation.EWalletBukti
import id.bca.bcamobile.ui.navigation.EWalletNominal
import id.bca.bcamobile.ui.navigation.EWalletPilih
import id.bca.bcamobile.ui.navigation.EWalletPin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keadaan sesi aplikasi, satu untuk seluruh proses.
 *
 * Sengaja lewat Hilt: sebelumnya kelas ini dibuat manual di [SessionViewModel],
 * sehingga token yang tersimpan tidak pernah ikut menentukan keadaan awal.
 */
@Singleton
class SessionRepository @Inject constructor(
    private val authRepository: AuthRepository,
) {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var _pendingReturnRoute: Any? = null

    /**
     * Menentukan keadaan awal setelah splash.
     *
     * Refresh token yang masih tersimpan berarti sesi sebelumnya belum berakhir —
     * tapi aplikasi perbankan tidak boleh langsung masuk. Keadaannya jadi
     * [SessionState.Locked], sehingga nasabah tetap melewati kode akses atau
     * biometrik sementara refresh token tetap dipakai untuk memperbarui akses.
     *
     * Pembacaannya menyentuh EncryptedSharedPreferences dan Keystore, jadi
     * dijalankan di luar main thread.
     */
    suspend fun markReady() {
        val hasSession = withContext(Dispatchers.IO) { authRepository.isLoggedIn }
        _sessionState.value = if (hasSession) SessionState.Locked else SessionState.LoggedOut
    }

    fun authenticate(displayName: String) {
        _sessionState.value = SessionState.Authenticated(displayName)
    }

    fun lock() {
        _sessionState.value = SessionState.Locked
    }

    fun logout() {
        _pendingReturnRoute = null
        _sessionState.value = SessionState.LoggedOut
    }

    fun saveRouteForReturn(route: Any?) {
        _pendingReturnRoute = route?.takeUnless { isTransactionRoute(it) }
    }

    fun consumeReturnRoute(): Any? =
        _pendingReturnRoute.also { _pendingReturnRoute = null }

    private fun isTransactionRoute(route: Any): Boolean =
        route is EWalletPilih || route is EWalletNominal ||
            route is EWalletPin || route is EWalletBukti
}