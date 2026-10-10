package id.bca.bcamobile.ui.screen.buka_rekening.kamera_foto

import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import id.bca.bcamobile.core.camera.CameraCapture
import id.bca.bcamobile.R
import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.core.ocr.KtpTextRecognizer
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.common.next
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class BukaRekeningKameraFotoViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
    private val textRecognizer: KtpTextRecognizer,
) : BukaRekeningStepViewModel(repository, store) {

    fun onEvent(event: KameraFotoEvent) {
        when (event) {
            is KameraFotoEvent.KtpPhotoCaptured -> processCapturedPhoto(event)

            KameraFotoEvent.GalleryImportFailed ->
                store.update {
                    it.copy(
                        isProcessingPhoto = false,
                        error = ErrorText.Res(R.string.buka_rekening_error_galeri_gagal),
                    )
                }

            KameraFotoEvent.FlashModeToggled ->
                store.update { it.copy(flashMode = it.flashMode.next()) }

            is KameraFotoEvent.AutoCaptureToggled ->
                store.update { it.copy(isAutoCaptureEnabled = event.enabled) }
        }
    }

    /**
     * Membaca e-KTP di perangkat lebih dulu.
     *
     * Hasilnya langsung tampil di layar pratinjau sementara foto belum diunggah,
     * dan foto yang terbaca buruk bisa ditolak di sana — menghemat kuota unggah
     * OCR yang dibatasi 10 kali per jam per sesi.
     */
    private fun processCapturedPhoto(event: KameraFotoEvent.KtpPhotoCaptured) {
        // Foto lama diganti; berkasnya tidak boleh menumpuk di cache.
        CameraCapture.discard(store.current.ktpPhoto)

        store.update {
            it.copy(
                ktpPhoto = event.photo,
                captureResolution = event.resolution,
                ocr = null,
                localScan = null,
                isProcessingPhoto = true,
                error = null,
            )
        }

        viewModelScope.launch {
            val bitmap = CameraCapture.decode(event.photo)
            val scan = bitmap?.let { textRecognizer.scan(it) }
            bitmap?.recycle()

            store.update { it.copy(localScan = scan, isProcessingPhoto = false) }
            store.send(BukaRekeningSideEffect.ShowCaptureResult)
        }
    }
}
