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
import id.bca.bcamobile.session.SessionState
import id.bca.bcamobile.ui.components.LocalSnackbarHostState
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.screen.splash.SplashScreen

// ── Root Composable ─────────────────────────────────────────────────────

@Composable
fun BcaApp(
    sessionState: SessionState,
    onAuthenticated: (displayName: String) -> Unit,
    onLogout: () -> Unit,
    onSaveRouteForReturn: (Any?) -> Unit,
    onConsumeReturnRoute: () -> Any?,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = sessionState is SessionState.Loading,
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
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

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