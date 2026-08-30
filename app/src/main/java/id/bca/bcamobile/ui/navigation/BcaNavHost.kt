package id.bca.bcamobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import id.bca.bcamobile.session.SessionState

// ── Root Composable ─────────────────────────────────────────────────────

@Composable
fun BcaApp(
    sessionState: SessionState,
    onAuthenticated: () -> Unit,
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

            SessionState.Loading -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = startGraph,
        modifier = modifier,
    ) {
        authGraph(navController, onAuthenticated)
        mainGraph(navController)
    }
}

private fun saveCurrentRouteForReturn(
    navController: NavHostController,
    onSaveRouteForReturn: (Any?) -> Unit,
) {
    val dest = navController.currentDestination ?: return
    val route: Any? = when {
        dest.hasRoute<Beranda>() -> Beranda
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