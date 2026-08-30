package id.bca.bcamobile.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class AppLifecycleObserver(
    private val sessionRepository: SessionRepository,
) : DefaultLifecycleObserver {

    private var backgroundTimestamp: Long = 0L

    override fun onStop(owner: LifecycleOwner) {
        if (sessionRepository.sessionState.value is SessionState.Authenticated) {
            backgroundTimestamp = System.currentTimeMillis()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (backgroundTimestamp > 0L &&
            sessionRepository.sessionState.value is SessionState.Authenticated
        ) {
            val elapsed = System.currentTimeMillis() - backgroundTimestamp
            if (elapsed >= LOCK_THRESHOLD_MS) {
                sessionRepository.lock()
            }
        }
        backgroundTimestamp = 0L
    }

    companion object {
        private const val LOCK_THRESHOLD_MS = 3L * 60L * 1000L // 3 menit
    }
}