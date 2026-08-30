package id.bca.bcamobile.session

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel

class SessionViewModel : ViewModel() {

    val repository = SessionRepository()
    private val lifecycleObserver = AppLifecycleObserver(repository)

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        repository.markReady()
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        super.onCleared()
    }
}