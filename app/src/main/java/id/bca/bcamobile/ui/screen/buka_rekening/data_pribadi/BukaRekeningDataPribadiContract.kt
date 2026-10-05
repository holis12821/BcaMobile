package id.bca.bcamobile.ui.screen.buka_rekening.data_pribadi

import id.bca.bcamobile.ui.screen.buka_rekening.common.DataPribadiField
import id.bca.bcamobile.ui.screen.buka_rekening.common.JenisKelamin
import id.bca.bcamobile.ui.screen.buka_rekening.common.Pekerjaan
import id.bca.bcamobile.ui.screen.buka_rekening.common.Penghasilan
import id.bca.bcamobile.ui.screen.buka_rekening.common.SumberDana

/** Event layar Data Pribadi. */
sealed interface DataPribadiEvent {

    /**
     * Layar tampil. Mengisi form dari hasil OCR, sekali saja — kunjungan kedua
     * (mis. nasabah menekan Back dari layar OTP) tidak boleh menimpa koreksi
     * yang sudah diketik.
     */
    data object ScreenShown : DataPribadiEvent

    data class FieldChanged(val field: DataPribadiField, val value: String) : DataPribadiEvent

    /** Milidetik UTC dari `DatePicker`; diubah ke `yyyy-MM-dd` di ViewModel. */
    data class BirthDateSelected(val epochMillis: Long) : DataPribadiEvent

    data class GenderSelected(val value: JenisKelamin) : DataPribadiEvent

    data class DomicileSameToggled(val same: Boolean) : DataPribadiEvent

    data class PekerjaanSelected(val value: Pekerjaan) : DataPribadiEvent

    data class PenghasilanSelected(val value: Penghasilan) : DataPribadiEvent

    data class SumberDanaSelected(val value: SumberDana) : DataPribadiEvent

    /** Mengirim `personal_data`; server membalas dengan tantangan OTP. */
    data object PersonalDataSubmitted : DataPribadiEvent

    data object DraftSaveRequested : DataPribadiEvent
}
