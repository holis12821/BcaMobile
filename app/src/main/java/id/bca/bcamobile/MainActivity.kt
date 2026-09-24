package id.bca.bcamobile

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import id.bca.bcamobile.session.SessionState
import id.bca.bcamobile.session.SessionViewModel
import id.bca.bcamobile.ui.navigation.BcaApp
import id.bca.bcamobile.ui.theme.BcaMobileTheme

@AndroidEntryPoint
// FragmentActivity, bukan ComponentActivity: BiometricPrompt mensyaratkannya.
class MainActivity : FragmentActivity() {

    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            val sessionState by sessionViewModel.repository.sessionState.collectAsState()
            val showSplash = sessionState is SessionState.Loading
            LaunchedEffect(showSplash) {
                if (!showSplash) {
                    window.setBackgroundDrawableResource(R.color.white)
                }
            }
            BcaMobileTheme {
                BcaApp(
                    sessionState = sessionState,
                    onLogout = { sessionViewModel.repository.logout() },
                    onAuthenticated = { displayName ->
                        sessionViewModel.repository.authenticate(displayName)
                    },
                    onSaveRouteForReturn = { route ->
                        sessionViewModel.repository.saveRouteForReturn(route)
                    },
                    onConsumeReturnRoute = {
                        sessionViewModel.repository.consumeReturnRoute()
                    },
                )
            }
        }
    }
}