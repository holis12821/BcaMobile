package id.bca.bcamobile.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.tween
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.first
import id.bca.bcamobile.R
import id.bca.bcamobile.core.push.NotificationPermissionEffect
import id.bca.bcamobile.session.AppGate
import id.bca.bcamobile.session.SessionState
import id.bca.bcamobile.ui.components.LocalSnackbarHostState
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.screen.splash.AppBlockedScreen
import id.bca.bcamobile.ui.screen.splash.SplashScreen

// ── Root Composable ─────────────────────────────────────────────────────

@Composable
fun BcaApp(
    sessionState: SessionState,
    appGate: AppGate,
    onAuthenticated: (displayName: String) -> Unit,
    onLogout: () -> Unit,
    onSaveRouteForReturn: (Any?) -> Unit,
    onConsumeReturnRoute: () -> Any?,
    onRetryConfig: () -> Unit,
    onUpdateApp: () -> Unit,
    pendingPushRoute: Any?,
    onPushRouteConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Pemeliharaan dan wajib perbarui menutup aplikasi sebelum NavHost dipasang,
    // supaya tidak ada route yang bisa dicapai lewat deep link saat server menutup layanan.
    when (appGate) {
        is AppGate.Maintenance -> {
            AppBlockedScreen(
                title = stringResource(R.string.gate_maintenance_title),
                message = appGate.message
                    ?: stringResource(R.string.gate_maintenance_default_message),
                actionLabel = stringResource(R.string.gate_coba_lagi),
                onAction = onRetryConfig,
                modifier = modifier,
            )
            return
        }

        is AppGate.UpdateRequired -> {
            AppBlockedScreen(
                title = stringResource(R.string.gate_update_title),
                message = stringResource(R.string.gate_update_message, appGate.minimumVersion),
                actionLabel = stringResource(R.string.gate_perbarui),
                onAction = onUpdateApp,
                modifier = modifier,
            )
            return
        }

        AppGate.Checking, AppGate.Open -> Unit
    }

    Crossfade(
        targetState = sessionState is SessionState.Loading || appGate is AppGate.Checking,
        animationSpec = tween(durationMillis = 200),
        label = "splash",
    ) { isLoading ->
        if (isLoading) {
            SplashScreen(modifier = modifier)
        } else {
            AppNavHost(
                sessionState = sessionState,
                onAuthenticated = onAuthenticated,
                onLogout = onLogout,
                onSaveRouteForReturn = onSaveRouteForReturn,
                onConsumeReturnRoute = onConsumeReturnRoute,
                pendingPushRoute = pendingPushRoute,
                onPushRouteConsumed = onPushRouteConsumed,
                modifier = modifier,
            )
        }
    }
}

// ── Nav Host (mounted after splash) ────────────────────────────────────

@Composable
private fun AppNavHost(
    sessionState: SessionState,
    onAuthenticated: (displayName: String) -> Unit,
    onLogout: () -> Unit,
    onSaveRouteForReturn: (Any?) -> Unit,
    onConsumeReturnRoute: () -> Any?,
    pendingPushRoute: Any?,
    onPushRouteConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    // Izin notifikasi diminta setelah nasabah masuk, bukan saat aplikasi pertama
    // dibuka: penolakan kedua bersifat permanen tanpa lewat Setelan sistem.
    NotificationPermissionEffect(enabled = sessionState is SessionState.Authenticated)

    val startGraph = remember {
        if (sessionState is SessionState.Authenticated) GraphMain else GraphAuth
    }

    LaunchedEffect(sessionState) {
        when (sessionState) {
            is SessionState.Authenticated -> {
                val alreadyInMain = navController.currentDestination
                    ?.hierarchy?.any { it.hasRoute<GraphMain>() } == true
                if (!alreadyInMain) {
                    val returnRoute = onConsumeReturnRoute()
                    navController.navigate(GraphMain) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                    returnRoute?.let { route ->
                        navController.navigate(route) { launchSingleTop = true }
                    }
                }
            }

            SessionState.LoggedOut -> {
                val alreadyInAuth = navController.currentDestination
                    ?.hierarchy?.any { it.hasRoute<GraphAuth>() } == true
                if (!alreadyInAuth) {
                    navController.navigate(GraphAuth) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            SessionState.Locked -> {
                val alreadyInAuth = navController.currentDestination
                    ?.hierarchy?.any { it.hasRoute<GraphAuth>() } == true
                if (!alreadyInAuth) {
                    saveCurrentRouteForReturn(navController, onSaveRouteForReturn)
                    navController.navigate(GraphAuth) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            else -> Unit
        }
    }

    /*
     * Tujuan dari notifikasi yang ditekan.
     *
     * Dua hal yang harus ditunggu, dan keduanya alasan efek ini terpisah dari efek
     * sesi di atas: nasabah harus sudah terautentikasi, dan `GraphMain` harus benar
     * benar terpasang. Menavigasi lebih dulu berarti tujuannya ikut terbuang oleh
     * `popUpTo` yang mengosongkan back stack saat berpindah graph.
     *
     * Tap yang datang saat masih terkunci tidak dibuang — nilainya tetap tertahan
     * di SessionViewModel sampai sesi terbuka, lalu efek ini jalan.
     */
    LaunchedEffect(pendingPushRoute, sessionState) {
        val route = pendingPushRoute ?: return@LaunchedEffect
        if (sessionState !is SessionState.Authenticated) return@LaunchedEffect

        navController.currentBackStackEntryFlow.first { entry ->
            entry.destination.hierarchy.any { it.hasRoute<GraphMain>() }
        }
        navController.navigate(route) { launchSingleTop = true }
        onPushRouteConsumed()
    }

    // Host pesan disediakan sekali di sini supaya layar tetap stateless dan tidak
    // perlu Scaffold sendiri-sendiri. Lihat `ui/components/AppMessageHost.kt`.
    val snackbarHostState = remember { SnackbarHostState() }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Box(modifier = modifier) {
            NavHost(
                navController = navController,
                startDestination = startGraph,
            ) {
                authGraph(navController, onAuthenticated)
                mainGraph(navController, onLogout)
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // enableEdgeToEdge() aktif di MainActivity: tanpa inset ini
                    // snackbar tertutup navigation bar.
                    .navigationBarsPadding()
                    .padding(Spacing.s4),
            )
        }
    }
}

private fun saveCurrentRouteForReturn(
    navController: NavHostController,
    onSaveRouteForReturn: (Any?) -> Unit,
) {
    val dest = navController.currentDestination ?: return
    val route: Any? = when {
        dest.hasRoute<Home>() -> Home
        dest.hasRoute<Mutasi>() -> Mutasi
        dest.hasRoute<Riwayat>() -> Riwayat
        dest.hasRoute<Akun>() -> Akun
        dest.hasRoute<Transfer>() -> Transfer
        dest.hasRoute<RentangWaktu>() -> RentangWaktu
        dest.hasRoute<EWalletPilih>() -> EWalletPilih
        dest.hasRoute<EWalletNominal>() -> EWalletNominal
        dest.hasRoute<EWalletPin>() -> EWalletPin
        dest.hasRoute<EWalletBukti>() -> EWalletBukti
        else -> null
    }
    onSaveRouteForReturn(route)
}