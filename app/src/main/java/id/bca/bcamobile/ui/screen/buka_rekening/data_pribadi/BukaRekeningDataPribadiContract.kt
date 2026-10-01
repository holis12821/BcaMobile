package id.bca.bcamobile.ui.screen.buka_rekening.data_pribadi

import id.bca.bcamobile.ui.screen.buka_rekening.common.JenisKelamin

/** Event layar Data Pribadi. */
sealed interface DataPribadiEvent {

    data class GenderSelected(val value: JenisKelamin) : DataPribadiEvent

    data class DomicileSameToggled(val same: Boolean) : DataPribadiEvent

    /** Mengirim `personal_data`; server membalas dengan tantangan OTP. */
    data object PersonalDataSubmitted : DataPribadiEvent

    data object DraftSaveRequested : DataPribadiEvent
}
