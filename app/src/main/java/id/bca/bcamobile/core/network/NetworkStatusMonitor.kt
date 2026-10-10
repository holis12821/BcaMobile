package id.bca.bcamobile.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import id.bca.bcamobile.data.config.remote.HealthApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Sumber tunggal keadaan jaringan untuk lampu indikator.
 *
 * Menggabungkan tiga sinyal yang sudah tersedia, **tanpa endpoint baru di
 * backend**:
 *
 *  1. **Sistem** — [ConnectivityManager] melaporkan apakah ada jaringan yang
 *     ber-`INTERNET` **dan** `VALIDATED`. Syarat kedua yang membedakan "WiFi
 *     tersambung" dari "WiFi yang benar-benar sampai ke internet": captive
 *     portal hotel punya yang pertama, tidak punya yang kedua.
 *  2. **Hasil panggilan API nyata** — dilaporkan [NetworkCallWatcher] dari
 *     setiap request OkHttp. Ini sinyal paling berharga karena mengukur jalur
 *     yang sebenarnya dipakai aplikasi, termasuk saat servernya yang bermasalah
 *     sementara jaringannya sempurna.
 *  3. **Perkiraan bandwidth** — hanya dipakai saat belum ada panggilan terbaru.
 *
 * Yang **tidak** dilakukan: menyimpulkan dari ada-tidaknya jaringan saja. Nasabah
 * dengan empat bar sinyal dan API yang mati akan melihat lampu hijau, dan lampu
 * yang berbohong lebih buruk daripada tidak ada lampu.
 */
@Singleton
class NetworkStatusMonitor @Inject constructor(
    private val context: Context,
    // Provider, bukan HealthApi langsung — dan itu wajib, bukan preferensi.
    // HealthApi dibangun dari Retrofit → OkHttp → NetworkCallWatcher → monitor
    // ini. Menyuntik HealthApi apa adanya menutup lingkaran itu dan Dagger
    // menolak merakit graph-nya. Provider memutus lingkarannya karena HealthApi
    // baru dibuat saat probe pertama berjalan.
    private val healthApi: Provider<HealthApi>,
    private val config: NetworkStatusConfig = NetworkStatusConfig(),
) {

    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(ConnectivityManager::class.java)

    private val scope = CoroutineScope(SupervisorJob())

    private val signals = MutableStateFlow(readSystemSignals())

    /**
     * Status mentah, berubah setiap kali ada sinyal baru. Tidak diekspos: nilai
     * inilah yang perlu ditahan sebelum sampai ke lampu.
     */
    private val rawStatus = MutableStateFlow(evaluate(signals.value))

    private val _status = MutableStateFlow(rawStatus.value)

    /** Keadaan yang ditampilkan lampu. Sudah ditahan sesuai `stabilizeMillis`. */
    val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    private var probeJob: Job? = null
    private var started = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refreshSystemSignals()
        override fun onLost(network: Network) = refreshSystemSignals()
        override fun onUnavailable() = refreshSystemSignals()

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) = refreshSystemSignals()
    }

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            // Sinyal sistem dibaca ulang saat kembali ke depan: callback tidak
            // menjamin terkirim saat proses di-freeze, jadi keadaan yang
            // tersimpan bisa sudah ketinggalan.
            refreshSystemSignals()
            startProbing()
        }

        override fun onStop(owner: LifecycleOwner) {
            stopProbing()
        }
    }

    /**
     * Mulai mengamati. Dipanggil sekali dari `BcaMobileApplication.onCreate`.
     *
     * Observer lifecycle dipasang di sana, bukan di `init` kelas ini, karena
     * [ProcessLifecycleOwner] mensyaratkan main thread sementara monitor ini
     * bisa dirakit Dagger di thread mana pun.
     */
    fun start() {
        if (started) return
        started = true

        runCatching {
            connectivityManager?.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build(),
                networkCallback,
            )
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)

        scope.launch {
            // collectLatest, bukan collect: kandidat baru membatalkan penundaan
            // yang sedang berjalan, dan itu tepat yang dibutuhkan penahan ini.
            rawStatus.collectLatest { candidate ->
                if (candidate == _status.value) return@collectLatest

                // Jaringan sistem yang hilang adalah fakta, bukan perkiraan —
                // tidak ada gunanya ditahan. Nasabah yang masuk lift harus
                // langsung melihat merah.
                val immediate = candidate == NetworkStatus.OFFLINE && !signals.value.systemOnline
                if (!immediate) delay(config.stabilizeMillis)

                _status.value = candidate
            }
        }
    }

    /** Dipakai test untuk melepas observer dan menghentikan coroutine. */
    internal fun stop() {
        if (!started) return
        started = false
        stopProbing()
        runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
        scope.cancel()
    }

    /**
     * Mencatat hasil satu panggilan API nyata.
     *
     * Dipanggil [NetworkCallWatcher] untuk **setiap** request di kedua jaringan.
     * Gagal di sini berarti request tidak pernah menghasilkan response —
     * timeout, DNS, TLS, socket putus — atau server menjawab 5xx.
     */
    fun recordCall(durationMillis: Long, failed: Boolean) {
        signals.update {
            it.copy(
                lastCallAtMillis = System.currentTimeMillis(),
                lastCallFailed = failed,
                lastCallDurationMillis = durationMillis,
            )
        }
        rawStatus.value = evaluate(signals.value)
    }

    // -- Sinyal sistem ---------------------------------------------------------

    private fun refreshSystemSignals() {
        val fresh = readSystemSignals()
        signals.update {
            it.copy(
                systemOnline = fresh.systemOnline,
                downstreamKbps = fresh.downstreamKbps,
            )
        }
        rawStatus.value = evaluate(signals.value)
    }

    private fun readSystemSignals(): NetworkSignals {
        val capabilities = runCatching {
            connectivityManager?.let { it.getNetworkCapabilities(it.activeNetwork) }
        }.getOrNull()

        val online = capabilities != null &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        return NetworkSignals(
            systemOnline = online,
            downstreamKbps = capabilities?.linkDownstreamBandwidthKbps ?: 0,
        )
    }

    // -- Probe -----------------------------------------------------------------

    private fun startProbing() {
        if (probeJob?.isActive == true) return
        probeJob = scope.launch {
            while (true) {
                delay(config.probeIntervalMillis)
                probeIfSignalStale()
            }
        }
    }

    private fun stopProbing() {
        probeJob?.cancel()
        probeJob = null
    }

    /**
     * Memanggil `GET /health` hanya kalau memang perlu.
     *
     * Dua gerbang sebelum satu byte dikirim:
     *
     *  - **Sudah ada panggilan nyata yang segar** → tidak perlu. Di layar yang
     *    aktif memanggil API, probe ini tidak pernah menambah traffic apa pun.
     *  - **Sistem melaporkan tidak ada jaringan** → sudah pasti merah, dan
     *    request yang pasti gagal hanya membuang baterai.
     *
     * Hasilnya tidak dibaca di sini: request-nya melewati OkHttp yang sama, jadi
     * [NetworkCallWatcher] yang mencatat durasi dan hasilnya. Satu jalur
     * penilaian untuk probe dan panggilan nyata — tidak ada dua logika yang
     * bisa menyimpang.
     */
    private suspend fun probeIfSignalStale() {
        val current = signals.value
        if (!current.systemOnline) return

        val age = System.currentTimeMillis() - current.lastCallAtMillis
        if (current.lastCallAtMillis > 0 && age < config.probeIntervalMillis) return

        // Dipanggil langsung, bukan lewat ApiCaller: ApiCaller mengulang tiga
        // kali dengan backoff, dan lampu yang menunggu 9 detik untuk memerah
        // sudah kehilangan gunanya.
        runCatching { healthApi.get().health() }
    }

    // -- Penilaian -------------------------------------------------------------

    /** Aturannya ada di [evaluateNetworkStatus] — murni, dan diuji tanpa Android. */
    private fun evaluate(s: NetworkSignals): NetworkStatus =
        evaluateNetworkStatus(s, config, System.currentTimeMillis())
}
