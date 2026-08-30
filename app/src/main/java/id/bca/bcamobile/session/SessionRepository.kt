package id.bca.bcamobile.session

import id.bca.bcamobile.ui.navigation.EWalletBukti
import id.bca.bcamobile.ui.navigation.EWalletNominal
import id.bca.bcamobile.ui.navigation.EWalletPilih
import id.bca.bcamobile.ui.navigation.EWalletPin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionRepository {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var _pendingReturnRoute: Any? = null

    fun markReady() {
        _sessionState.value = SessionState.LoggedOut
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