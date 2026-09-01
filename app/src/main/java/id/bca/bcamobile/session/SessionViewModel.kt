package id.bca.bcamobile.session

import android.annotation.SuppressLint
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SessionViewModel : ViewModel() {

    val repository = SessionRepository()
    private val lifecycleObserver = AppLifecycleObserver(repository)

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        viewModelScope.launch {
            delay(SPLASH_MIN_DURATION_MS.milliseconds)
            repository.markReady()
        }
    }

    companion object {
        private const val SPLASH_MIN_DURATION_MS = 2000L
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        super.onCleared()
    }
}