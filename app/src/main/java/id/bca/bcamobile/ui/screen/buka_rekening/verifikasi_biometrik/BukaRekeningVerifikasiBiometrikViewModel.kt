package id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_biometrik

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.R
import id.bca.bcamobile.core.liveness.CaptureSlot
import id.bca.bcamobile.core.liveness.LivenessConfig
import id.bca.bcamobile.core.liveness.LivenessFailure
import id.bca.bcamobile.core.liveness.LivenessFrame
import id.bca.bcamobile.core.liveness.LivenessPayload
import id.bca.bcamobile.core.liveness.LivenessState
import id.bca.bcamobile.core.liveness.LivenessStateMachine
import id.bca.bcamobile.core.liveness.LivenessStepFrame
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.core.security.LivenessAttestor
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.domain.onboarding.model.LivenessSubmission
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.common.OnboardingErrorCode
import id.bca.bcamobile.ui.screen.buka_rekening.common.isBusinessCode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel layar Verifikasi Biometrik Wajah.
 *
 * Yang berubah paling mendasar dibanding versi sebelumnya: **tidak ada satu pun jalur
 * di sini yang menyatakan liveness lulus**. Dulu `isComplete` dari detektor di perangkat
 * langsung memicu unggahan dan langkah maju ke VIDEO_CALL, dan `liveness_meta` yang
 * dikirim berisi konstanta `completed_actions = 3` apa pun yang sebenarnya terjadi.
 * Sekarang perangkat hanya mengumpulkan bahan bukti; `livenessVerified` dari response
 * server yang menentukan, dan langkah berikutnya mengikuti itu.
 */
@HiltViewModel
class BukaRekeningVerifikasiBiometrikViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
    private val attestor: LivenessAttestor,
) : BukaRekeningStepViewModel(repository, store) {

    /** Konfigurasi ambang deteksi; satu sumber untuk mesin status dan analyzer. */
    val config = LivenessConfig()

    /**
     * Mesin status dipegang ViewModel, bukan layar.
     *
     * Analyzer dibuat ulang setiap kali `DisposableEffect` dijalankan — rotasi layar,
     * izin kamera berubah, recomposition. Kalau mesin statusnya ikut dibuat ulang di
     * sana, tantangan yang sedang berjalan hilang bersama nonce-nya.
     */
    val machine = LivenessStateMachine(config)

    private var challengeJob: Job? = null
    private var cooldownJob: Job? = null

    fun onEvent(event: VerifikasiBiometrikEvent) {
        when (event) {
            VerifikasiBiometrikEvent.CameraReady -> publish(machine.onCameraReady().state)

            is VerifikasiBiometrikEvent.LivenessStateChanged -> publish(event.state)

            VerifikasiBiometrikEvent.ChallengeNeeded -> requestChallenge()

            is VerifikasiBiometrikEvent.FramesReady -> submit(event.frames)

            VerifikasiBiometrikEvent.RetryRequested -> {
                val update = machine.retry()
                publish(update.state)
                if (update.needsNewChallenge) requestChallenge()
            }
        }
    }

    private fun publish(state: LivenessState) {
        store.update { it.copy(livenessState = state) }
    }

    // -- Tantangan -------------------------------------------------------------

    private fun requestChallenge() {
        // Satu permintaan pada satu waktu: setiap permintaan menerbitkan nonce baru
        // di server dan ikut menghitung kuota percobaan nasabah.
        if (challengeJob?.isActive == true) return
        if (store.current.isLivenessBlocked) return

        challengeJob = viewModelScope.launch {
            when (val result = repository.requestLivenessChallenge()) {
                is DataResult.Success -> {
                    val challenge = result.value
                    if (challenge.actions.isEmpty()) {
                        // Server mengirim aksi yang seluruhnya tidak dikenal client.
                        // Melanjutkan berarti meminta nasabah melakukan gerakan yang
                        // tidak akan pernah terdeteksi.
                        machine.onChallengeRequestFailed()
                        store.update { it.copy(error = ErrorText.Res(R_ERROR_CHALLENGE)) }
                        return@launch
                    }
                    publish(machine.onChallengeIssued(challenge).state)
                }

                is DataResult.Failure -> {
                    machine.onChallengeRequestFailed()
                    handleLivenessFailure(result.error)
                }
            }
        }
    }

    // -- Pengiriman bukti ------------------------------------------------------

    private fun submit(frames: List<LivenessFrame>) {
        // Tantangan bisa hilang di antara CAPTURE dan titik ini kalau tantangannya
        // dibatalkan. Keluar diam-diam akan membuat layar menggantung di CAPTURE
        // tanpa pesan dan tanpa tombol coba lagi.
        val challenge = machine.current.challenge ?: run {
            publish(machine.failLocally(LivenessFailure.EVIDENCE_INCOMPLETE).state)
            wipe(frames)
            return
        }
        val neutral = frames.firstOrNull { it.slot is CaptureSlot.Neutral }
        // Diurutkan indeks, bukan urutan di buffer.
        //
        // Server memasangkan `liveness_frames[i]` dengan `step_meta[i]` lalu menuntut
        // `step.Index == i`. Payload yang ditandatangani sendiri sudah diurutkan
        // indeks, jadi urutan yang berbeda di sini akan lolos verifikasi tanda tangan
        // tapi ditolak sebagai `step_index_mismatch` — kegagalan yang jauh lebih sulit
        // dilacak daripada dicegah di satu baris ini.
        val steps = frames.mapNotNull { frame ->
            val slot = frame.slot as? CaptureSlot.Step ?: return@mapNotNull null
            LivenessStepFrame(
                index = slot.index,
                action = slot.action,
                capturedAtMillis = frame.capturedAtMillis,
                jpeg = frame.jpeg,
            )
        }.sortedBy { it.index }

        // Frame netral yang hilang berarti tidak ada pembanding face match. Mengirimnya
        // tanpa itu hanya membuang satu percobaan nasabah untuk penolakan yang pasti.
        //
        // Dilaporkan sebagai bukti yang tidak lengkap, BUKAN sebagai penolakan server:
        // server belum pernah melihat payload ini, dan menyebutnya "ditolak server"
        // membuat jejak masalahnya menunjuk ke tempat yang salah.
        if (neutral == null || steps.size != challenge.actions.size) {
            publish(machine.failLocally(LivenessFailure.EVIDENCE_INCOMPLETE).state)
            wipe(frames)
            return
        }

        publish(machine.onSubmitted().state)

        launchWithLoading {
            val payload = LivenessPayload.build(
                challengeId = challenge.challengeId,
                nonce = challenge.nonce,
                deviceId = attestor.deviceId(),
                neutralFrame = neutral.jpeg,
                stepFrames = steps,
            )
            val publicKey = attestor.publicKey()
            val signature = publicKey?.let { attestor.sign(payload) }

            if (signature == null || publicKey == null) {
                // Tanpa tanda tangan, server tidak bisa memastikan payload datang dari
                // perangkat ini. Mengirim apa adanya bukan jalan pintas yang tersedia.
                wipe(frames)
                publish(machine.onServerVerdict(passed = false).state)
                store.update { it.copy(error = ErrorText.Res(R_ERROR_DEVICE_KEY)) }
                return@launchWithLoading
            }

            // Token integritas diminta dengan nonce tantangan, lalu dikirim **tanpa
            // dibaca**: isinya ditandatangani Google dan hanya berarti setelah
            // diverifikasi server.
            val integrityToken = attestor.integrityToken(challenge.nonce)

            val result = repository.submitLiveness(
                LivenessSubmission(
                    challengeId = challenge.challengeId,
                    nonce = challenge.nonce,
                    neutralFrame = neutral.jpeg,
                    stepFrames = steps,
                    signedPayload = payload,
                    signature = signature,
                    signatureAlgorithm = attestor.signatureAlgorithm,
                    deviceKeyId = publicKey.keyId,
                    devicePublicKey = publicKey.base64,
                    integrityToken = integrityToken,
                    riskSignals = attestor.riskSignals(),
                ),
            )

            // Piksel wajah ditimpa nol apa pun hasilnya, sebelum cabang mana pun di
            // bawah bisa keluar lebih awal.
            wipe(frames)

            when (result) {
                is DataResult.Success -> {
                    val verdict = result.value
                    store.update { it.copy(biometric = verdict) }
                    publish(machine.onServerVerdict(verdict.livenessVerified).state)
                    if (verdict.livenessVerified) {
                        store.send(BukaRekeningSideEffect.AdvanceTo(OnboardingStep.VIDEO_CALL))
                    }
                }

                is DataResult.Failure -> {
                    publish(machine.onServerVerdict(passed = false).state)
                    handleLivenessFailure(result.error)
                }
            }
        }
    }

    /** Menimpa isi setiap frame sebelum acuannya dilepas. */
    private fun wipe(frames: List<LivenessFrame>) = frames.forEach { it.jpeg.fill(0) }

    // -- Kegagalan, masa tunggu, dan eskalasi ----------------------------------

    /**
     * Masa tunggu dan hitungan percobaan **tidak pernah dihitung di sini**.
     *
     * Keduanya milik server (keputusan Q6); yang dilakukan client hanya menampilkan
     * sisa detik yang dibalas API. Hitungan kedua di perangkat akan menyimpang dari
     * yang sebenarnya berlaku, dan nasabah akan melihat tombol hidup saat server masih
     * menolak.
     */
    private suspend fun handleLivenessFailure(error: ApiFailure) {
        when {
            error is ApiFailure.RateLimited -> {
                store.update { it.copy(error = error.toLivenessErrorText()) }
                startCooldown(error.retryAfterSeconds ?: 0)
            }

            error.isBusinessCode(OnboardingErrorCode.LIVENESS_BLOCKED) ||
                error.isBusinessCode(OnboardingErrorCode.LIVENESS_ESCALATED) -> {
                store.update {
                    it.copy(isLivenessBlocked = true, livenessCooldownSeconds = 0)
                }
                handleFailure(error)
            }

            else -> handleFailure(error)
        }
    }

    private fun ApiFailure.RateLimited.toLivenessErrorText(): ErrorText = when {
        message.isNotBlank() -> ErrorText.Raw(message)
        retryAfterSeconds != null ->
            ErrorText.Res(R_COOLDOWN, listOf(retryAfterSeconds))
        else -> ErrorText.Res(R_ERROR_CHALLENGE)
    }

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        if (seconds <= 0) {
            store.update { it.copy(livenessCooldownSeconds = 0) }
            return
        }
        cooldownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                store.update { it.copy(livenessCooldownSeconds = remaining) }
                delay(TICK_MS)
                remaining--
            }
            store.update { it.copy(livenessCooldownSeconds = 0) }
        }
    }

    private companion object {
        const val TICK_MS = 1_000L
        val R_COOLDOWN = R.string.buka_rekening_biometrik_cooldown
        val R_ERROR_CHALLENGE = R.string.buka_rekening_error_liveness_challenge
        val R_ERROR_DEVICE_KEY = R.string.buka_rekening_error_liveness_device_key
    }
}
