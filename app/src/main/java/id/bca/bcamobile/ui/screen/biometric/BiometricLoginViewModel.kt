package id.bca.bcamobile.ui.screen.biometric

import android.util.Base64
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.core.network.messageOrNull
import id.bca.bcamobile.core.security.BiometricAuthResult
import id.bca.bcamobile.core.security.BiometricAvailability
import id.bca.bcamobile.core.security.BiometricKeyManager
import id.bca.bcamobile.core.security.BiometricSigner
import id.bca.bcamobile.core.security.SigningKeyResult
import id.bca.bcamobile.domain.auth.AuthRepository
import id.bca.bcamobile.domain.auth.model.BiometricType
import id.bca.bcamobile.domain.common.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class BiometricLoginStatus { IDLE, SCANNING, SUCCESS, FAILED }

data class BiometricLoginState(
    val status: BiometricLoginStatus = BiometricLoginStatus.IDLE,
    val message: String? = null,
)

/** Teks dialog sistem; diambil dari `strings.xml` oleh pemanggil, bukan di sini. */
data class BiometricPromptText(
    val title: String,
    val subtitle: String,
    val negativeButton: String,
)

sealed interface BiometricLoginEvent {
    /** [displayName] datang dari server, bukan tebakan client. */
    data class LoginSucceeded(val displayName: String) : BiometricLoginEvent

    /**
     * Biometrik tidak bisa dipakai di perangkat ini, atau kuncinya hangus karena
     * sidik jari baru didaftarkan. Nasabah diarahkan ke kode akses.
     */
    data class FallbackToAccessCode(val reason: FallbackReason) : BiometricLoginEvent
}

enum class FallbackReason {
    /** Kunci belum ada atau hangus; perlu didaftarkan ulang setelah login kode akses. */
    NEEDS_REGISTRATION,
    NOT_ENROLLED,
    NO_HARDWARE,
    TEMPORARILY_UNAVAILABLE,
}

/**
 * Login biometrik untuk layar Face ID dan Touch ID.
 *
 * Urutannya: ambil challenge dari server, siapkan kunci, minta verifikasi
 * biometrik, tanda tangani challenge, kirim ke server. Aplikasi tidak pernah
 * menyentuh data biometrik — hanya objek `Signature` yang sudah dibuka sistem.
 */
@HiltViewModel
class BiometricLoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val keyManager: BiometricKeyManager,
    private val signer: BiometricSigner,
) : ViewModel() {

    private val _state = MutableStateFlow(BiometricLoginState())
    val state: StateFlow<BiometricLoginState> = _state.asStateFlow()

    private val _events = Channel<BiometricLoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun authenticate(
        activity: FragmentActivity,
        type: BiometricType,
        promptText: BiometricPromptText,
    ) {
        if (_state.value.status == BiometricLoginStatus.SCANNING) return

        when (signer.availability()) {
            BiometricAvailability.READY -> Unit
            BiometricAvailability.NOT_ENROLLED -> return fallback(FallbackReason.NOT_ENROLLED)
            BiometricAvailability.NO_HARDWARE -> return fallback(FallbackReason.NO_HARDWARE)
            BiometricAvailability.TEMPORARILY_UNAVAILABLE ->
                return fallback(FallbackReason.TEMPORARILY_UNAVAILABLE)
        }

        _state.update { BiometricLoginState(status = BiometricLoginStatus.SCANNING) }

        viewModelScope.launch {
            // AndroidKeyStore membuka berkas dan bicara ke Keymaster lewat binder.
            // Sebelumnya ini berjalan di main thread saat layar Face ID/Touch ID
            // dibuka, jadi frame tertahan tepat di awal layar.
            if (!withContext(Dispatchers.IO) { keyManager.hasKey() }) {
                return@launch fallback(FallbackReason.NEEDS_REGISTRATION)
            }

            val challenge = when (val result = authRepository.biometricChallenge()) {
                is DataResult.Success -> result.value
                is DataResult.Failure -> return@launch fail(result.error.messageOrNull())
            }

            // Kontraknya dibaca dari response, bukan dari konstanta di kode ini.
            // Kalau backend berpindah algoritma, build ini berhenti di sini dan
            // menawarkan kode akses — jauh lebih jelas daripada mengirim tanda
            // tangan EC yang pasti ditolak lalu tampil sebagai "biometrik gagal".
            // Algoritma kosong berarti server belum mengirimkannya; nilai lama
            // tetap dipakai supaya build ini jalan di server versi sebelumnya.
            if (challenge.algorithm.isNotBlank() &&
                !challenge.algorithm.equals(BiometricKeyManager.WIRE_ALGORITHM, ignoreCase = true)
            ) {
                return@launch fallback(FallbackReason.TEMPORARILY_UNAVAILABLE)
            }

            // getKey + initSign juga menyentuh Keystore; alasan yang sama.
            // BiometricPrompt sendiri tetap dipanggil dari Main setelah blok ini.
            val keyResult = withContext(Dispatchers.IO) { keyManager.signatureForSigning() }
            val signature = when (val key = keyResult) {
                is SigningKeyResult.Ready -> key.signature
                // Kunci hangus: sidik jari atau wajah perangkat berubah.
                SigningKeyResult.NeedsRegistration ->
                    return@launch fallback(FallbackReason.NEEDS_REGISTRATION)

                SigningKeyResult.Unavailable ->
                    return@launch fallback(FallbackReason.TEMPORARILY_UNAVAILABLE)
            }

            val authenticated = signer.authenticate(
                activity = activity,
                signature = signature,
                title = promptText.title,
                subtitle = promptText.subtitle,
                negativeButton = promptText.negativeButton,
            )

            val unlocked = when (authenticated) {
                is BiometricAuthResult.Success -> authenticated.signature
                BiometricAuthResult.Cancelled -> {
                    _state.update { BiometricLoginState(status = BiometricLoginStatus.IDLE) }
                    return@launch
                }

                is BiometricAuthResult.Failed -> return@launch fail(null)
            }

            // Challenge datang sebagai base64; yang ditandatangani adalah isi aslinya.
            val payload = runCatching { Base64.decode(challenge.challenge, Base64.NO_WRAP) }
                .getOrNull() ?: return@launch fail(null)

            val signed = runCatching {
                unlocked.update(payload)
                Base64.encodeToString(unlocked.sign(), Base64.NO_WRAP)
            }.getOrNull() ?: return@launch fail(null)

            when (
                val login = authRepository.loginWithBiometric(
                    type = type,
                    challengeId = challenge.challengeId,
                    signedChallenge = signed,
                    keyId = keyManager.keyId(),
                )
            ) {
                is DataResult.Success -> {
                    _state.update { BiometricLoginState(status = BiometricLoginStatus.SUCCESS) }
                    _events.send(BiometricLoginEvent.LoginSucceeded(login.value.displayName))
                }

                is DataResult.Failure -> fail(login.error.messageOrNull())
            }
        }
    }

    fun reset() = _state.update { BiometricLoginState() }

    private fun fail(message: String?) = _state.update {
        BiometricLoginState(status = BiometricLoginStatus.FAILED, message = message)
    }

    private fun fallback(reason: FallbackReason) {
        _state.update { BiometricLoginState(status = BiometricLoginStatus.FAILED) }
        viewModelScope.launch {
            _events.send(BiometricLoginEvent.FallbackToAccessCode(reason))
        }
    }
}
