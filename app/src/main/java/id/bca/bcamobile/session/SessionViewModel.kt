package id.bca.bcamobile.session

import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bca.bcamobile.core.push.PushDeepLink
import id.bca.bcamobile.core.push.PushEventBus
import id.bca.bcamobile.core.push.PushTokenEvent
import id.bca.bcamobile.core.push.PushTokenRegistrar
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.config.AppConfigRepository
import id.bca.bcamobile.domain.config.model.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    val repository: SessionRepository,
    private val appConfigRepository: AppConfigRepository,
    private val pushTokenRegistrar: PushTokenRegistrar,
    private val pushEventBus: PushEventBus,
) : ViewModel() {

    private val lifecycleObserver = AppLifecycleObserver(repository)

    private val _appGate = MutableStateFlow<AppGate>(AppGate.Checking)

    /** Gerbang `GET /health`: pemeliharaan dan wajib perbarui memblokir seluruh aplikasi. */
    val appGate: StateFlow<AppGate> = _appGate.asStateFlow()

    /**
     * Tujuan yang harus dibuka karena nasabah menekan notifikasi; null kalau tidak
     * ada. Nilainya tetap tersedia sampai [consumePushRoute] dipanggil — tap yang
     * datang sebelum grafik navigasi siap tidak boleh hilang.
     */
    val pendingPushRoute: StateFlow<Any?> = pushEventBus.pendingOpen
        .map { open -> open?.let { PushDeepLink.routeFor(it.deepLink) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        viewModelScope.launch {
            repository.markReady()
        }
        refreshConfig()

        // Token push didaftarkan setiap sesi jadi terautentikasi. Satu tempat ini
        // menutup tiga kasus sekaligus: login kode akses, login biometrik, dan
        // aplikasi yang dibuka dalam keadaan sudah login — sesi tersimpan selalu
        // melewati Locked lebih dulu, jadi ia pasti lewat sini.
        viewModelScope.launch {
            repository.sessionState.collect { state ->
                if (state is SessionState.Authenticated) {
                    pushTokenRegistrar.onSessionAuthenticated()
                }
            }
        }

        viewModelScope.launch {
            pushTokenRegistrar.events.collect { event ->
                when (event) {
                    // Perangkat sudah dicabut di server. Artinya bukan "notifikasi
                    // gagal", jadi tidak ada pesan soal notifikasi — sesi
                    // dibersihkan dan nasabah kembali ke layar masuk.
                    PushTokenEvent.DeviceRevoked -> repository.forceLogout()
                }
            }
        }
    }

    /** Dipanggil `MainActivity` saat Intent membawa muatan notifikasi. */
    fun onPushNotificationOpened(deepLink: String?) {
        pushEventBus.onNotificationOpened(deepLink)
    }

    fun consumePushRoute() {
        pushEventBus.consumePendingOpen()
    }

    fun refreshConfig() {
        _appGate.value = AppGate.Checking
        viewModelScope.launch {
            // Config gagal dibaca bukan alasan mengunci nasabah: gerbang dibuka,
            // dan endpoint sebenarnya tetap menolak kalau fiturnya memang mati.
            val config = when (val result = appConfigRepository.refresh()) {
                is DataResult.Success -> result.value
                is DataResult.Failure -> AppConfig.Permissive
            }
            _appGate.value = when {
                config.maintenanceMode -> AppGate.Maintenance(config.maintenanceMessage)
                config.forceUpdate -> AppGate.UpdateRequired(config.minimumAppVersion)
                else -> AppGate.Open
            }
        }
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        super.onCleared()
    }
}