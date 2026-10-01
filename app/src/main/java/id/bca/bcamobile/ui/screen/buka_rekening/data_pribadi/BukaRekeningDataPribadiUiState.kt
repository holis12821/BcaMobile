package id.bca.bcamobile.ui.screen.buka_rekening.data_pribadi

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState
import id.bca.bcamobile.ui.screen.buka_rekening.common.JenisKelamin
import id.bca.bcamobile.ui.screen.buka_rekening.common.joinNonBlank
import id.bca.bcamobile.ui.screen.buka_rekening.common.toDisplayDate

data class BukaRekeningDataPribadiUiState(
    val nik: String = "",
    val namaLengkap: String = "",
    val tempatLahir: String = "",
    val tanggalLahir: String = "",
    val jenisKelamin: JenisKelamin = JenisKelamin.LAKI_LAKI,
    val alamatLengkap: String = "",
    val rtRw: String = "",
    val kodePos: String = "",
    val kelurahanKecamatan: String = "",
    val kotaProvinsi: String = "",
    val alamatDomisiliSama: Boolean = true,
    val jenisPekerjaan: String = "",
    val penghasilanPerBulan: String = "",
    val sumberDanaUtama: String = "",
)

@Composable
fun BukaRekeningFlowState.toDataPribadiUiState(): BukaRekeningDataPribadiUiState {
    val data = ktpData ?: return BukaRekeningDataPribadiUiState(
        alamatDomisiliSama = alamatDomisiliSama,
    )
    return BukaRekeningDataPribadiUiState(
        nik = data.nik,
        namaLengkap = data.namaLengkap,
        tempatLahir = data.tempatLahir,
        tanggalLahir = data.tanggalLahir.toDisplayDate(),
        jenisKelamin = jenisKelamin ?: JenisKelamin.LAKI_LAKI,
        alamatLengkap = data.alamat,
        rtRw = data.rtRw.replace("/", " / "),
        kodePos = "",
        kelurahanKecamatan = joinNonBlank(data.kelurahan, data.kecamatan),
        kotaProvinsi = joinNonBlank(data.kota, data.provinsi),
        alamatDomisiliSama = alamatDomisiliSama,
        // Tiga nilai di bawah masih default karena layar belum punya picker-nya.
        jenisPekerjaan = stringResource(R.string.buka_rekening_pekerjaan_karyawan_swasta),
        penghasilanPerBulan = stringResource(R.string.buka_rekening_penghasilan_10_20_juta),
        sumberDanaUtama = stringResource(R.string.buka_rekening_sumber_dana_gaji),
    )
}
