package id.bca.bcamobile

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import id.bca.bcamobile.core.push.PushDeepLink
import id.bca.bcamobile.session.AppGate
import id.bca.bcamobile.session.SessionState
import id.bca.bcamobile.session.SessionViewModel
import id.bca.bcamobile.ui.navigation.BcaApp
import id.bca.bcamobile.ui.theme.BcaMobileTheme

@AndroidEntryPoint
// FragmentActivity, bukan ComponentActivity: BiometricPrompt mensyaratkannya.
class MainActivity : FragmentActivity() {

    /**
     * Membuka halaman aplikasi di Play Store saat server mewajibkan pembaruan.
     * Perangkat tanpa Play Store jatuh ke tautan web.
     */
    private fun openPlayStore() {
        val market = Intent(Intent.ACTION_VIEW, PLAY_STORE_APP_URI.toUri())
        val web = Intent(Intent.ACTION_VIEW, PLAY_STORE_WEB_URL.toUri())
        runCatching { startActivity(market) }.onFailure { runCatching { startActivity(web) } }
    }

    private val sessionViewModel: SessionViewModel by viewModels()

    /**
     * Notifikasi yang ditekan nasabah sampai ke sini, **bukan** ke
     * `BcaFirebaseMessagingService`: muatan ber-`notification` ditampilkan SDK saat
     * aplikasi di background, dan `onMessageReceived` tidak pernah dipanggil untuk
     * kasus itu. `data` ikut sebagai extras Intent.
     */
    private fun handlePushIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        val fromPush = extras.containsKey(PushDeepLink.EXTRA_TYPE) ||
            extras.containsKey(PushDeepLink.EXTRA_DEEP_LINK)
        if (!fromPush) return

        sessionViewModel.onPushNotificationOpened(extras.getString(PushDeepLink.EXTRA_DEEP_LINK))

        // Dibuang setelah dibaca supaya Intent yang sama tidak membuka tujuannya
        // lagi ketika activity dibuat ulang — mis. saat rotasi layar.
        intent.removeExtra(PushDeepLink.EXTRA_TYPE)
        intent.removeExtra(PushDeepLink.EXTRA_DEEP_LINK)
    }

    /**
     * Aplikasi sudah hidup di belakang dan tidak dibuat ulang, jadi [onCreate] tidak
     * dipanggil. Melewatkan ini membuat tap "tidak berfungsi" hanya pada sebagian
     * kasus — yang paling sulit dilacak.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePushIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        handlePushIntent(intent)
        setContent {
            val sessionState by sessionViewModel.repository.sessionState.collectAsState()
            val appGate by sessionViewModel.appGate.collectAsState()
            val pendingPushRoute by sessionViewModel.pendingPushRoute.collectAsState()
            val showSplash = sessionState is SessionState.Loading || appGate is AppGate.Checking
            LaunchedEffect(showSplash) {
                if (!showSplash) {
                    window.setBackgroundDrawableResource(R.color.white)
                }
            }
            BcaMobileTheme {
                BcaApp(
                    sessionState = sessionState,
                    appGate = appGate,
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
                    onRetryConfig = sessionViewModel::refreshConfig,
                    onUpdateApp = ::openPlayStore,
                    pendingPushRoute = pendingPushRoute,
                    onPushRouteConsumed = sessionViewModel::consumePushRoute,
                )
            }
        }
    }

    private companion object {
        const val PLAY_STORE_APP_URI = "market://details?id=" + BuildConfig.APPLICATION_ID
        const val PLAY_STORE_WEB_URL =
            "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID
    }
}