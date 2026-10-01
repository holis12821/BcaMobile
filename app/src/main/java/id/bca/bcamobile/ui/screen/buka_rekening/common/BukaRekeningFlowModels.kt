package id.bca.bcamobile.ui.screen.buka_rekening.common

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
