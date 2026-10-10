package id.bca.bcamobile.ui.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.core.network.NetworkStatus
import id.bca.bcamobile.core.network.NetworkStatusMonitor
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Menjembatani [NetworkStatusMonitor] ke lapisan Compose.
 *
 * Sengaja tipis dan tanpa logika: monitornya `@Singleton` yang hidup selama
 * proses, jadi yang dibutuhkan hanya satu titik injeksi yang boleh dipanggil
 * dari composable. Tidak ada state yang dipegang di sini — mengkopi status ke
 * ViewModel akan membuat dua sumber kebenaran untuk satu keadaan.
 */
@HiltViewModel
class NetworkStatusViewModel @Inject constructor(
    monitor: NetworkStatusMonitor,
) : ViewModel() {

    val status: StateFlow<NetworkStatus> = monitor.status
}
