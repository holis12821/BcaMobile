package id.bca.bcamobile.ui.screen.buka_rekening.data_pribadi

import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.R
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.common.DataPribadiForm
import id.bca.bcamobile.ui.screen.buka_rekening.common.OnboardingErrorCode
import id.bca.bcamobile.ui.screen.buka_rekening.common.isBusinessCode
import id.bca.bcamobile.ui.screen.buka_rekening.common.isValid
import id.bca.bcamobile.ui.screen.buka_rekening.common.toErrorText
import id.bca.bcamobile.ui.screen.buka_rekening.common.toJenisKelamin
import id.bca.bcamobile.ui.screen.buka_rekening.common.toPersonalData
import id.bca.bcamobile.ui.screen.buka_rekening.common.withField
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class BukaRekeningDataPribadiViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    fun onEvent(event: DataPribadiEvent) {
        when (event) {
            DataPribadiEvent.ScreenShown -> prefillFromOcr()

            is DataPribadiEvent.FieldChanged -> updateForm {
                it.withField(event.field, event.value)
            }

            is DataPribadiEvent.BirthDateSelected -> updateForm {
                it.copy(tanggalLahir = event.epochMillis.toIsoDate())
            }

            is DataPribadiEvent.GenderSelected ->
                store.update { it.copy(jenisKelamin = event.value) }

            is DataPribadiEvent.DomicileSameToggled ->
                store.update { it.copy(alamatDomisiliSama = event.same) }

            is DataPribadiEvent.PekerjaanSelected -> updateForm { it.copy(pekerjaan = event.value) }

            is DataPribadiEvent.PenghasilanSelected ->
                updateForm { it.copy(penghasilan = event.value) }

            is DataPribadiEvent.SumberDanaSelected ->
                updateForm { it.copy(sumberDana = event.value) }

            DataPribadiEvent.PersonalDataSubmitted -> savePersonalData()

            DataPribadiEvent.DraftSaveRequested ->
                emitMessage(ErrorText.Res(R.string.buka_rekening_draft_saved))
        }
    }

    /**
     * Mengisi form dari hasil pindai e-KTP, sekali per sesi.
     *
     * Hasil server dipakai lebih dulu, hasil baca di perangkat jadi cadangan —
     * urutan yang sama dengan [BukaRekeningFlowState.ktpData], supaya yang tampil
     * di layar ini sama dengan yang tampil di layar Hasil Foto.
     */
    private fun prefillFromOcr() {
        val current = store.current
        if (current.dataPribadi != null) return
        val data = current.ktpData ?: return

        store.update {
            it.copy(
                dataPribadi = DataPribadiForm.from(data),
                // Jenis kelamin dari e-KTP jadi pilihan awal; nasabah tetap bisa
                // menggantinya kalau OCR salah baca.
                jenisKelamin = it.jenisKelamin ?: data.jenisKelamin.toJenisKelamin(),
            )
        }
    }

    private fun updateForm(block: (DataPribadiForm) -> DataPribadiForm) {
        store.update { state ->
            state.copy(
                dataPribadi = block(state.dataPribadi ?: DataPribadiForm()),
                // Satu pesan error di atas tombol tidak boleh menetap setelah
                // nasabah mulai memperbaiki; tanda merah per field tetap tampil
                // karena diturunkan dari isi form.
                error = null,
            )
        }
    }

    private fun savePersonalData() {
        val current = store.current
        val ocr = current.ocr
        if (ocr == null) {
            store.update { it.copy(error = ErrorText.Res(R.string.buka_rekening_error_no_ocr)) }
            return
        }

        val form = current.dataPribadi ?: DataPribadiForm.from(ocr.extracted)
        if (!form.isValid) {
            // Ditahan di client: `personal-data` yang ditolak server tetap
            // menghabiskan kuota rate limit sesi.
            store.update {
                it.copy(
                    dataPribadi = form,
                    showDataPribadiErrors = true,
                    error = ErrorText.Res(R.string.buka_rekening_dp_error_form),
                )
            }
            return
        }

        val data = form.toPersonalData(
            nik = ocr.extracted.nik,
            namaLengkap = ocr.extracted.namaLengkap,
            jenisKelamin = current.jenisKelamin,
            jenisKelaminKtp = ocr.extracted.jenisKelamin,
            alamatDomisiliSama = current.alamatDomisiliSama,
        )

        launchWithLoading {
            when (val result = repository.savePersonalData(ocr.ocrId, data)) {
                is DataResult.Success -> {
                    val challenge = result.value
                    // OTP pembuka step ini menolkan penghitung gagal dan kuota
                    // kirim ulang di server, jadi blokir lokal ikut dilepas.
                    // Jendela hitung mundurnya dimulai di ViewModel layar OTP.
                    store.update {
                        it.copy(
                            otpSentTo = challenge.otpSentTo,
                            otpExpiresAt = challenge.otpExpiresAt,
                            otpCode = "",
                            isOtpInputBlocked = false,
                            isOtpResendBlocked = false,
                            showDataPribadiErrors = false,
                        )
                    }
                    store.send(BukaRekeningSideEffect.AdvanceTo(challenge.currentStep))
                }
                is DataResult.Failure -> handlePersonalDataFailure(result.error)
            }
        }
    }

    /**
     * `OTP_DELIVERY_FAILED` bukan penolakan: data pribadi **sudah** tersimpan dan server
     * sudah pindah ke `OTP_VERIFY` — yang gagal hanya pengiriman SMS-nya, sementara kodenya
     * tetap terbit dan sah (`06-BUKA-REKENING-API-SPEC.md` §3b).
     *
     * Diperlakukan sebagai kegagalan biasa, nasabah tertahan di layar ini padahal server
     * sudah maju, jadi tap "Lanjut" berikutnya hanya menghasilkan `ONBOARDING_INVALID_STEP`
     * dan butuh dua tap untuk sampai ke layar OTP. Posisi barunya **ditanyakan** lewat
     * [refreshStep], bukan ditebak lokal: `current_step` dari server tetap satu-satunya
     * sumber kebenaran navigasi. Blokir kirim ulang dilepas supaya nasabah punya jalan
     * keluar begitu sampai di layar OTP — itu yang diminta spec, bukan mengulang flow.
     */
    private suspend fun handlePersonalDataFailure(error: ApiFailure) {
        if (!error.isBusinessCode(OnboardingErrorCode.OTP_DELIVERY_FAILED)) {
            handleFailure(error)
            return
        }

        store.update {
            it.copy(
                otpCode = "",
                isOtpInputBlocked = false,
                isOtpResendBlocked = false,
                showDataPribadiErrors = false,
                error = error.toErrorText(),
            )
        }
        refreshStep()
    }
}

/** Milidetik dari `DatePicker` adalah UTC; tanggal lahir dikirim `yyyy-MM-dd`. */
private fun Long.toIsoDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().toString()
