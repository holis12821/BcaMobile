package id.bca.bcamobile.ui.screen.buka_rekening.hasil_foto

import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.R
import id.bca.bcamobile.core.camera.CameraCapture
import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.domain.onboarding.model.KtpOcrResult
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.common.FlashMode
import id.bca.bcamobile.ui.screen.buka_rekening.common.toJenisKelamin
import javax.inject.Inject

@HiltViewModel
class BukaRekeningHasilFotoViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    fun onEvent(event: HasilFotoEvent) {
        when (event) {
            HasilFotoEvent.PhotoDiscarded -> discardPhoto()
            HasilFotoEvent.OcrResultRequested -> loadOcrResult()
            HasilFotoEvent.OcrConfirmed -> confirmOcr()
        }
    }

    private fun discardPhoto() {
        CameraCapture.discard(store.current.ktpPhoto)
        store.update {
            it.copy(ktpPhoto = null, localScan = null, ocr = null, captureResolution = "")
        }
    }

    private fun loadOcrResult() {
        launchWithLoading {
            when (val result = repository.getOcrResult()) {
                is DataResult.Success -> applyOcr(result.value)
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }

    /** Hasil OCR jadi sumber prefill form data pribadi, termasuk jenis kelamin. */
    private fun applyOcr(ocr: KtpOcrResult) {
        store.update {
            it.copy(
                ocr = ocr,
                jenisKelamin = it.jenisKelamin ?: ocr.extracted.jenisKelamin.toJenisKelamin(),
            )
        }
    }

    /**
     * Kalau server sudah punya hasil OCR untuk sesi ini, langsung maju. Kalau
     * belum, fotonya diunggah sekarang.
     */
    private fun confirmOcr() {
        val current = store.current
        when {
            current.ocr != null -> advanceTo(OnboardingStep.PERSONAL_DATA)
            current.ktpPhoto != null -> uploadKtpPhoto()
            else -> store.update {
                it.copy(error = ErrorText.Res(R.string.buka_rekening_error_no_ocr))
            }
        }
    }

    /** Unggah foto ke server untuk OCR resmi dan pencocokan Dukcapil. */
    private fun uploadKtpPhoto() {
        val current = store.current
        val photo = current.ktpPhoto ?: run {
            store.update { it.copy(error = ErrorText.Res(R.string.buka_rekening_error_no_photo)) }
            return
        }

        launchWithLoading {
            val result = repository.uploadKtpPhoto(
                photo = photo,
                flashUsed = current.flashMode != FlashMode.OFF,
                autoCaptured = current.isAutoCaptureEnabled,
                resolution = current.captureResolution,
            )
            when (result) {
                is DataResult.Success -> {
                    applyOcr(result.value)
                    // Foto sudah di server; salinan lokalnya tidak boleh tertinggal.
                    CameraCapture.discard(photo)
                    store.update { it.copy(ktpPhoto = null) }
                    store.send(
                        BukaRekeningSideEffect.AdvanceTo(OnboardingStep.PERSONAL_DATA),
                    )
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }
}
