package id.bca.bcamobile.ui.screen.buka_rekening.common

import androidx.annotation.StringRes
import id.bca.bcamobile.R

/**
 * Langkah alur buka rekening seperti yang dihitung untuk nasabah.
 *
 * **Satu-satunya sumber nomor dan penyebut progress bar.** Sebelumnya tiap layar
 * menuliskan dua angkanya sendiri sebagai literal, dan hasilnya lima penyebut berbeda
 * dalam satu alur (4, 7, 8, 11, 14), nomor yang mundur dari "9 dari 11" di Kredensial
 * ke "6 dari 7" di Ringkasan, serta nomor 3 yang dipakai tiga layar sekaligus. Karena
 * nomor dan total sekarang sama-sama diturunkan dari daftar ini, keduanya tidak bisa
 * lagi berselisih antar layar.
 *
 * Urutannya **urutan layar**, bukan urutan [id.bca.bcamobile.domain.onboarding.model.OnboardingStep]:
 * nasabah memilih kartu sebelum menyetujui S&K, sementara enum server menaruh `TNC`
 * lebih dulu karena keduanya baru tercatat bersamaan saat sesi dibuat. Peta step server
 * ke route tetap di `AuthGraph.toRoute()` dan tidak diikat ke daftar ini — itu soal
 * navigasi resume, ini soal tampilan.
 *
 * Satu langkah bisa dilayani beberapa layar, dan itu memang mengikuti server:
 * `VERIFIKASI_IDENTITAS` mencakup Panduan Foto, Kamera Foto, dan Hasil Foto — ketiganya
 * satu `OnboardingStep.OCR` — dan `VIDEO_CALL` mencakup Antrean dan Video Call.
 * Kamera Foto dan Video Call sendiri tidak menampilkan progress bar karena layarnya
 * penuh pratinjau kamera.
 *
 * Layar Berhasil Dibuat tidak ada di sini: itu hasil alurnya, bukan langkah yang harus
 * dilalui, dan memang tidak pernah menampilkan progress bar.
 *
 * Menambah layar baru ke alur berarti menambah entri di sini — dan itu memang
 * disengaja: nomor langkah tidak boleh bisa ditambahkan diam-diam di satu layar saja.
 */
enum class BukaRekeningLangkah(@StringRes val labelRes: Int) {
    PILIH_PRODUK(R.string.buka_rekening_pilih_produk),
    PILIH_KARTU(R.string.buka_rekening_kartu_step_label),
    SYARAT_KETENTUAN(R.string.buka_rekening_sk_step_label),
    VERIFIKASI_IDENTITAS(R.string.buka_rekening_foto_step_label),
    DATA_PRIBADI(R.string.buka_rekening_dp_step_label),
    VERIFIKASI_OTP(R.string.buka_rekening_otp_step_label),
    VERIFIKASI_BIOMETRIK(R.string.buka_rekening_biometrik_step_label),
    VIDEO_CALL(R.string.buka_rekening_antrean_step_label),
    KREDENSIAL(R.string.buka_rekening_kredensial_step_label),
    RINGKASAN(R.string.buka_rekening_ringkasan_step_label),
    ;

    /** Nomor yang dibaca nasabah, mulai dari 1 — bukan [ordinal]. */
    val nomor: Int get() = ordinal + 1

    companion object {
        /** Penyebut "Langkah n dari N"; ikut bertambah sendiri saat entri ditambah. */
        val total: Int get() = entries.size
    }
}
