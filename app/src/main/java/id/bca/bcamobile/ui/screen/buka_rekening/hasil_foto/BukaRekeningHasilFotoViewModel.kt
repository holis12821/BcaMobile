package id.bca.bcamobile.ui.screen.buka_rekening.hasil_foto

import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.R
import id.bca.bcamobile.core.camera.CameraCapture
import id.bca.bcamobile.core.network.ApiFailure
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
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BukaRekeningHasilFotoViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    /**
     * Penjaga agar probe resume hanya jalan sekali selama ViewModel hidup.
     *
     * Umurnya sama dengan entri navigasi layar ini, jadi masuk lagi dari kamera
     * memberi ViewModel baru dan probe-nya boleh jalan lagi — sementara perubahan
     * state di dalam layar yang sama tidak bisa memicunya berulang.
     */
    private var hasProbedStoredOcr = false

    fun onEvent(event: HasilFotoEvent) {
        when (event) {
            HasilFotoEvent.PhotoDiscarded -> discardPhoto()
            HasilFotoEvent.ScreenShown -> probeStoredOcrOnce()
            HasilFotoEvent.OcrConfirmed -> confirmOcr()
        }
    }

    /**
     * Hanya untuk melanjutkan draf: tidak ada foto lokal, tapi server mungkin sudah
     * menyimpan hasil OCR dari sesi sebelumnya. Kalau fotonya masih ada, yang berlaku
     * adalah foto itu dan tidak ada yang perlu ditanyakan ke server.
     */
    private fun probeStoredOcrOnce() {
        if (hasProbedStoredOcr) return
        val current = store.current
        if (current.sessionId == null || current.ocr != null || current.ktpPhoto != null) return

        hasProbedStoredOcr = true
        loadOcrResult()
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
                // "Server belum punya hasil OCR" adalah jawaban yang sah untuk
                // pertanyaan ini, bukan kegagalan: nasabah tinggal memfoto. Tanpa
                // cabang ini ia jatuh ke handleFailure dan — sebelum
                // OcrResultMissing dipisahkan — memusnahkan sesi yang masih hidup.
                is DataResult.Failure ->
                    if (result.error != ApiFailure.OcrResultMissing) {
                        handleFailure(result.error)
                    }
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
     * Membuang foto lokal setelah terunggah, tapi **mempertahankan** [localScan].
     *
     * Dulu `ktpPhoto` dan scan lokalnya ikut hilang bersama-sama. Itu bermasalah
     * karena `ktpData` memilih `ocr ?: localScan`: begitu scan lokal hilang,
     * satu-satunya sumber data adalah hasil server, dan tidak ada lagi
     * pembanding kalau keduanya berbeda. Scan-nya tetap di memory saja — tidak
     * pernah ditulis ke disk — dan dibersihkan bersama seluruh state flow saat
     * graph buka rekening ditinggalkan.
     */
    private fun discardUploadedPhoto(photo: File) {
        CameraCapture.discard(photo)
        store.update { it.copy(ktpPhoto = null) }
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

    /**
     * Unggah foto ke server untuk divalidasi.
     *
     * Pencocokan ke Dukcapil ikut bila server punya registri — `dukcapilChecked`
     * di hasilnya yang memberi tahu. Yang selalu dijalankan server adalah
     * pemeriksaan tata letak e-KTP, bentuk NIK, dan kecocokan NIK dengan tanggal
     * lahir serta jenis kelamin.
     */
    private fun uploadKtpPhoto() {
        val current = store.current
        val photo = current.ktpPhoto ?: run {
            store.update { it.copy(error = ErrorText.Res(R.string.buka_rekening_error_no_photo)) }
            return
        }

        // Teks yang dibaca ML Kit di perangkat ikut dikirim: itulah yang dibaca
        // server selama belum ada mesin OCR di sana. Tanpa ini server menjawab
        // dari teks hardcoded, dan form data pribadi terisi identitas orang lain.
        val clientOcrText = current.localScan?.rawText.orEmpty()

        launchWithLoading {
            val result = repository.uploadKtpPhoto(
                photo = photo,
                flashUsed = current.flashMode != FlashMode.OFF,
                autoCaptured = current.isAutoCaptureEnabled,
                resolution = current.captureResolution,
                clientOcrText = clientOcrText,
            )
            when (result) {
                is DataResult.Success -> {
                    applyOcr(result.value)
                    // Foto sudah di server; salinan lokalnya tidak boleh tertinggal.
                    discardUploadedPhoto(photo)
                    store.send(
                        BukaRekeningSideEffect.AdvanceTo(OnboardingStep.PERSONAL_DATA),
                    )
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }
}
