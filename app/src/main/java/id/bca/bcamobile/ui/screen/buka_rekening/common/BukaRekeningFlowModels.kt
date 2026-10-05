package id.bca.bcamobile.ui.screen.buka_rekening.common

import androidx.annotation.StringRes
import id.bca.bcamobile.R

/**
 * Nilai yang dipakai lebih dari satu layar, jadi tempatnya di `common/`.
 *
 * [FlashMode] dipakai layar Kamera Foto dan ikut terkirim sebagai metadata unggahan
 * e-KTP; [JenisKelamin] diisi layar Data Pribadi dan ikut payload `personal_data`.
 */
enum class FlashMode { AUTO, ON, OFF }

enum class JenisKelamin { LAKI_LAKI, PEREMPUAN }

/** Urutan putar tombol flash: AUTO, ON, OFF, kembali ke AUTO. */
fun FlashMode.next(): FlashMode = when (this) {
    FlashMode.AUTO -> FlashMode.ON
    FlashMode.ON -> FlashMode.OFF
    FlashMode.OFF -> FlashMode.AUTO
}

/**
 * Tiga daftar pilihan di section Pekerjaan & Penghasilan.
 *
 * [wire] ditulis eksplisit, tidak diturunkan dari `name`, karena nilainya adalah
 * kontrak dengan server (`06-BUKA-REKENING-API-SPEC.md` §3a): mengganti nama
 * konstanta Kotlin tidak boleh diam-diam mengubah isi payload. Nilai penghasilan
 * bahkan tidak bisa jadi nama konstanta — Kotlin melarang pengenal berawalan angka.
 */
enum class Pekerjaan(val wire: String, @param:StringRes val labelRes: Int) {
    KARYAWAN_SWASTA("KARYAWAN_SWASTA", R.string.buka_rekening_pekerjaan_karyawan_swasta),
    PNS("PNS", R.string.buka_rekening_pekerjaan_pns),
    TNI_POLRI("TNI_POLRI", R.string.buka_rekening_pekerjaan_tni_polri),
    WIRASWASTA("WIRASWASTA", R.string.buka_rekening_pekerjaan_wiraswasta),
    PROFESIONAL("PROFESIONAL", R.string.buka_rekening_pekerjaan_profesional),
    PELAJAR_MAHASISWA("PELAJAR_MAHASISWA", R.string.buka_rekening_pekerjaan_pelajar),
    IBU_RUMAH_TANGGA("IBU_RUMAH_TANGGA", R.string.buka_rekening_pekerjaan_irt),
    LAINNYA("LAINNYA", R.string.buka_rekening_pekerjaan_lainnya),
}

enum class Penghasilan(val wire: String, @param:StringRes val labelRes: Int) {
    DIBAWAH_5_JUTA("DIBAWAH_5_JUTA", R.string.buka_rekening_penghasilan_dibawah_5_juta),
    LIMA_SAMPAI_10_JUTA("5_10_JUTA", R.string.buka_rekening_penghasilan_5_10_juta),
    SEPULUH_SAMPAI_20_JUTA("10_20_JUTA", R.string.buka_rekening_penghasilan_10_20_juta),
    DUA_PULUH_SAMPAI_50_JUTA("20_50_JUTA", R.string.buka_rekening_penghasilan_20_50_juta),
    DIATAS_50_JUTA("DIATAS_50_JUTA", R.string.buka_rekening_penghasilan_diatas_50_juta),
}

enum class SumberDana(val wire: String, @param:StringRes val labelRes: Int) {
    GAJI("GAJI", R.string.buka_rekening_sumber_dana_gaji),
    USAHA("USAHA", R.string.buka_rekening_sumber_dana_usaha),
    INVESTASI("INVESTASI", R.string.buka_rekening_sumber_dana_investasi),
    WARISAN("WARISAN", R.string.buka_rekening_sumber_dana_warisan),
    LAINNYA("LAINNYA", R.string.buka_rekening_sumber_dana_lainnya),
}
