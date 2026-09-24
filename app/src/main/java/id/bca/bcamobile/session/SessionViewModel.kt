package id.bca.bcamobile.session

import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    val repository: SessionRepository,
) : ViewModel() {

    private val lifecycleObserver = AppLifecycleObserver(repository)

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        viewModelScope.launch {
            repository.markReady()
        }
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        super.onCleared()
    }
}