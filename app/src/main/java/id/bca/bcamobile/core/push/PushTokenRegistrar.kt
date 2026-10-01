package id.bca.bcamobile.core.push

import com.google.firebase.messaging.FirebaseMessaging
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.domain.account.AccountRepository
import id.bca.bcamobile.domain.auth.AuthRepository
import id.bca.bcamobile.domain.common.DataResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Mendaftarkan token FCM perangkat ke `POST /v1/account/device/push-token`.
 *
 * Endpoint itu butuh access token, dan backend mengambil identitas perangkat dari
 * klaim `did` di dalamnya — jadi pendaftaran **tidak mungkin** sebelum login, dan
 * `device_id` tidak pernah dikirim di body.
 *
 * Tiga titik pemicunya, ketiganya perlu:
 *
 * 1. [onSessionAuthenticated] — sesi jadi terautentikasi. Ini sekaligus menutup
 *    kasus "aplikasi dibuka dalam keadaan sudah login", karena sesi tersimpan
 *    selalu melewati kunci kode akses atau biometrik lebih dulu. Itu penting:
 *    backend menghapus `devices.push_token` begitu FCM menjawab `UNREGISTERED`,
 *    dan hanya pendaftaran ulang dari aplikasi yang memulihkannya.
 * 2. [onNewToken] — FCM merotasi token kapan saja. Tanpa pengiriman ulang, push
 *    berhenti sampai login berikutnya tanpa satu pun gejala di aplikasi.
 * 3. [onLogout] — token lama dimatikan supaya server berhenti mengirim ke ponsel
 *    yang sudah logout.
 *
 * **Semua kegagalan best-effort.** Login tetap berhasil, layar tetap terbuka:
 * token yang gagal terdaftar berarti push tidak sampai, bukan aplikasi rusak.
 * Satu-satunya kegagalan yang berarti bagi nasabah adalah [PushTokenEvent.DeviceRevoked].
 *
 * Token push **tidak pernah dicatat ke log** — ia identitas perangkat di sisi FCM,
 * dan log client ikut terkirim saat nasabah melaporkan keluhan.
 */
@Singleton
class PushTokenRegistrar @Inject constructor(
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
) {

    // Scope milik proses: pendaftaran tidak boleh mati bersama layar yang memicunya.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Menjaga dua pemicu yang datang berdekatan (login + onNewToken) tidak saling
    // menimpa nilai pendingToken.
    private val mutex = Mutex()

    /**
     * Token yang sudah didapat tapi belum berhasil didaftarkan — karena belum login,
     * atau karena jaringan sedang gagal. Hanya di memori: ia bukan rahasia yang perlu
     * bertahan, dan FCM selalu bisa memberikannya lagi.
     */
    private var pendingToken: String? = null

    private val _events = MutableSharedFlow<PushTokenEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PushTokenEvent> = _events.asSharedFlow()

    fun onSessionAuthenticated() {
        scope.launch {
            val token = mutex.withLock { pendingToken } ?: currentToken() ?: return@launch
            register(token)
        }
    }

    fun onNewToken(token: String) {
        scope.launch {
            if (!authRepository.isLoggedIn) {
                mutex.withLock { pendingToken = token }
                return@launch
            }
            register(token)
        }
    }

    /**
     * `POST /v1/auth/logout` mencabut **sesi**, bukan baris `devices` —
     * `push_token` tetap terisi dan tidak ada endpoint untuk menghapusnya. Token
     * yang dimatikan di sini membuat push berikutnya dijawab `UNREGISTERED`, dan
     * backend membersihkan barisnya sendiri. Jadi celahnya tertutup lewat jalur
     * yang sudah ada, tanpa endpoint baru.
     *
     * Konsekuensinya login berikutnya mendapat token **baru**, jadi
     * [onSessionAuthenticated] wajib benar-benar jalan.
     */
    fun onLogout() {
        scope.launch {
            mutex.withLock { pendingToken = null }
            deleteToken()
        }
    }

    private suspend fun register(token: String) {
        if (token.isBlank()) return

        // Idempoten di server: menulis nilai yang sama dua kali tidak berbiaya,
        // sedangkan token yang tidak terkirim membuat nasabah berhenti menerima
        // notifikasi tanpa satu pun tanda. Karena itu tidak ada penjagaan
        // "sudah pernah dikirim" di sini.
        when (val result = accountRepository.registerPushToken(token)) {
            is DataResult.Success -> mutex.withLock { pendingToken = null }

            is DataResult.Failure -> {
                val error = result.error
                // ApiCaller memeriksa `error.code` sebelum status HTTP, dan
                // cabang else-nya menangkap kode apa pun jadi Business — jadi
                // 403 berkode tidak pernah muncul sebagai ApiFailure lain.
                // Mencocokkan tipe tanpa kode di sini tidak akan pernah kena.
                if (error is ApiFailure.Business && error.code == DEVICE_NOT_RECOGNIZED) {
                    mutex.withLock { pendingToken = null }
                    _events.tryEmit(PushTokenEvent.DeviceRevoked)
                } else {
                    // Jaringan, 5xx, atau sesi habis: simpan, coba lagi pada
                    // pembukaan aplikasi berikutnya. Tidak ada retry berulang di
                    // latar belakang.
                    mutex.withLock { pendingToken = token }
                }
            }
        }
    }

    /**
     * Token perangkat dari FCM. `null` kalau Firebase belum terkonfigurasi
     * (`app/google-services.json` tidak ada) atau layanannya tidak tersedia —
     * keduanya bukan alasan menggagalkan apa pun yang memanggil.
     */
    private suspend fun currentToken(): String? = runCatching {
        suspendCancellableCoroutine<String?> { continuation ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> continuation.resume(token) }
                .addOnFailureListener { continuation.resume(null) }
        }
    }.getOrNull()

    private suspend fun deleteToken() {
        runCatching {
            suspendCancellableCoroutine<Unit> { continuation ->
                FirebaseMessaging.getInstance().deleteToken()
                    .addOnCompleteListener { continuation.resume(Unit) }
            }
        }
    }

    private companion object {
        const val DEVICE_NOT_RECOGNIZED = "AUTH_DEVICE_NOT_RECOGNIZED"
    }
}

/** Kejadian pendaftaran token yang perlu ditindaklanjuti di luar kelas ini. */
sealed interface PushTokenEvent {

    /**
     * `403 AUTH_DEVICE_NOT_RECOGNIZED` — perangkat ini sudah dicabut di server.
     * Mengulang panggilan tidak akan pernah berhasil; sesi harus dibersihkan dan
     * nasabah kembali ke layar masuk. Artinya bukan "notifikasi gagal", jadi
     * jangan menampilkan pesan tentang notifikasi.
     */
    data object DeviceRevoked : PushTokenEvent
}
