package id.bca.bcamobile.ui.screen.buka_rekening.data_pribadi

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState
import id.bca.bcamobile.ui.screen.buka_rekening.common.DataPribadiField
import id.bca.bcamobile.ui.screen.buka_rekening.common.DataPribadiForm
import id.bca.bcamobile.ui.screen.buka_rekening.common.JenisKelamin
import id.bca.bcamobile.ui.screen.buka_rekening.common.Pekerjaan
import id.bca.bcamobile.ui.screen.buka_rekening.common.Penghasilan
import id.bca.bcamobile.ui.screen.buka_rekening.common.SumberDana
import id.bca.bcamobile.ui.screen.buka_rekening.common.errorOf
import id.bca.bcamobile.ui.screen.buka_rekening.common.resolve
import id.bca.bcamobile.ui.screen.buka_rekening.common.tanggalLahirError
import id.bca.bcamobile.ui.screen.buka_rekening.common.toDisplayDate

/**
 * State layar Data Pribadi.
 *
 * NIK dan nama datang dari OCR dan tidak bisa diubah; sisanya isian form yang
 * bisa dikoreksi nasabah. Nilai di sini sudah **bentuk tampilan** — tanggal lahir
 * `21 April 1995`, label dropdown sudah diterjemahkan. Bentuk wire-nya tinggal di
 * [DataPribadiForm] dan hanya disentuh ViewModel.
 */
data class BukaRekeningDataPribadiUiState(
    val nik: String = "",
    val namaLengkap: String = "",
    val tempatLahir: String = "",
    /** Teks siap baca; kosong berarti belum dipilih dan placeholder yang tampil. */
    val tanggalLahir: String = "",
    val jenisKelamin: JenisKelamin = JenisKelamin.LAKI_LAKI,
    val alamatLengkap: String = "",
    val rtRw: String = "",
    val kodePos: String = "",
    val kelurahan: String = "",
    val kecamatan: String = "",
    val kota: String = "",
    val provinsi: String = "",
    val alamatDomisiliSama: Boolean = true,
    val nomorHp: String = "",
    val email: String = "",
    val jenisPekerjaan: String = "",
    val penghasilanPerBulan: String = "",
    val sumberDanaUtama: String = "",
    /** Pesan per field, hanya terisi setelah nasabah menekan Lanjut sekali. */
    val fieldErrors: Map<DataPribadiField, String> = emptyMap(),
    val tanggalLahirError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    /**
     * Tombol Lanjut tetap aktif walau form belum lengkap — justru penekanannya
     * yang memunculkan tanda merah di field yang kurang. Yang mematikannya hanya
     * request yang sedang berjalan, supaya `personal-data` tidak terkirim dua kali.
     */
    val isLanjutEnabled: Boolean get() = !isLoading
}

@Composable
fun BukaRekeningFlowState.toDataPribadiUiState(): BukaRekeningDataPribadiUiState {
    val form = dataPribadi ?: DataPribadiForm()
    return BukaRekeningDataPribadiUiState(
        nik = ktpData?.nik.orEmpty(),
        namaLengkap = ktpData?.namaLengkap.orEmpty(),
        tempatLahir = form.tempatLahir,
        tanggalLahir = form.tanggalLahir.takeIf { it.isNotBlank() }?.toDisplayDate().orEmpty(),
        jenisKelamin = jenisKelamin ?: JenisKelamin.LAKI_LAKI,
        alamatLengkap = form.alamatLengkap,
        rtRw = form.rtRw,
        kodePos = form.kodePos,
        kelurahan = form.kelurahan,
        kecamatan = form.kecamatan,
        kota = form.kota,
        provinsi = form.provinsi,
        alamatDomisiliSama = alamatDomisiliSama,
        nomorHp = form.nomorHp,
        email = form.email,
        jenisPekerjaan = stringResource(form.pekerjaan.labelRes),
        penghasilanPerBulan = stringResource(form.penghasilan.labelRes),
        sumberDanaUtama = stringResource(form.sumberDana.labelRes),
        fieldErrors = if (showDataPribadiErrors) form.fieldErrorTexts() else emptyMap(),
        tanggalLahirError = if (showDataPribadiErrors) {
            form.tanggalLahirError()?.let { stringResource(it) }
        } else {
            null
        },
        isLoading = isLoading,
        error = error?.resolve(),
    )
}

/**
 * Seluruh field dipetakan sekaligus, bukan dicari saat digambar: `stringResource`
 * tidak boleh dipanggil dari dalam lambda yang bukan composable, dan daftar field
 * di sini tetap jumlahnya.
 */
@Composable
private fun DataPribadiForm.fieldErrorTexts(): Map<DataPribadiField, String> =
    DataPribadiField.entries.mapNotNull { field ->
        errorOf(field)?.let { field to stringResource(it) }
    }.toMap()

/** Pilihan dropdown beserta labelnya, dirakit sekali untuk dipakai menu di layar. */
@Composable
fun pekerjaanOptions(): List<Pair<Pekerjaan, String>> =
    Pekerjaan.entries.map { it to stringResource(it.labelRes) }

@Composable
fun penghasilanOptions(): List<Pair<Penghasilan, String>> =
    Penghasilan.entries.map { it to stringResource(it.labelRes) }

@Composable
fun sumberDanaOptions(): List<Pair<SumberDana, String>> =
    SumberDana.entries.map { it to stringResource(it.labelRes) }
