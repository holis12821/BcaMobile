package id.bca.bcamobile.ui.screen.buka_rekening

import id.bca.bcamobile.R
import id.bca.bcamobile.ui.screen.buka_rekening.common.DataPribadiField
import id.bca.bcamobile.ui.screen.buka_rekening.common.DataPribadiForm
import id.bca.bcamobile.ui.screen.buka_rekening.common.MaskedPhone
import id.bca.bcamobile.ui.screen.buka_rekening.common.errorOf
import id.bca.bcamobile.ui.screen.buka_rekening.common.isValid
import id.bca.bcamobile.ui.screen.buka_rekening.common.maskedPhoneOrNull
import id.bca.bcamobile.ui.screen.buka_rekening.common.tanggalLahirError
import id.bca.bcamobile.ui.screen.buka_rekening.common.withField
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validasi form Data Pribadi — layar yang menerbitkan OTP.
 *
 * Aturannya datang dari skill `frontend-otp-verification` §5a: pola nomor HP
 * harus sama dengan gateway SMS backend, bukan regex lama yang salah di dua
 * arah, dan tanggal lahir punya rentang wajar.
 */
class DataPribadiFormTest {

    // -- Nomor HP --------------------------------------------------------------

    @Test
    fun `nomor dengan pemisah dan tanpa angka nol di depan diterima`() {
        // Dua bentuk yang dulu ditolak client padahal server menerimanya.
        assertNull(form(nomorHp = "0812-3456-7890").errorOf(DataPribadiField.NOMOR_HP))
        assertNull(form(nomorHp = "81234567890").errorOf(DataPribadiField.NOMOR_HP))
    }

    @Test
    fun `bentuk nasional, 62, dan +62 sama-sama diterima`() {
        listOf("081234568889", "6281234568889", "+6281234568889").forEach {
            assertNull("ditolak: $it", form(nomorHp = it).errorOf(DataPribadiField.NOMOR_HP))
        }
    }

    @Test
    fun `nomor yang pasti ditolak server juga ditolak di client`() {
        // Tiga bentuk yang dulu lolos client lalu dijawab 422 oleh server.
        val ditolak = listOf(
            "08123456789012", // 14 digit, di luar 10..13
            "0801234567", // blok 080 tidak dialokasikan untuk seluler
            "+628012345678", // sama, lewat awalan +62
        )
        ditolak.forEach {
            assertEquals(
                "lolos padahal harus ditolak: $it",
                R.string.buka_rekening_dp_error_nomor_hp,
                form(nomorHp = it).errorOf(DataPribadiField.NOMOR_HP),
            )
        }
    }

    @Test
    fun `nomor terlalu pendek dan bukan seluler ditolak`() {
        assertEquals(
            R.string.buka_rekening_dp_error_nomor_hp,
            form(nomorHp = "0812345").errorOf(DataPribadiField.NOMOR_HP),
        )
        // Telepon rumah: tidak diawali 08.
        assertEquals(
            R.string.buka_rekening_dp_error_nomor_hp,
            form(nomorHp = "0215550123").errorOf(DataPribadiField.NOMOR_HP),
        )
    }

    @Test
    fun `nomor kosong dilaporkan sebagai wajib diisi, bukan salah format`() {
        assertEquals(
            R.string.buka_rekening_dp_error_wajib,
            form(nomorHp = "").errorOf(DataPribadiField.NOMOR_HP),
        )
    }

    // -- Tanggal lahir ---------------------------------------------------------

    @Test
    fun `tanggal lahir di masa depan ditolak`() {
        val besok = LocalDate.now().plusDays(1).toString()
        assertEquals(
            R.string.buka_rekening_dp_error_tanggal,
            form(tanggalLahir = besok).tanggalLahirError(),
        )
    }

    @Test
    fun `tanggal lahir lebih dari 120 tahun lalu ditolak`() {
        val terlaluLama = LocalDate.now().minusYears(121).toString()
        assertEquals(
            R.string.buka_rekening_dp_error_tanggal,
            form(tanggalLahir = terlaluLama).tanggalLahirError(),
        )
    }

    @Test
    fun `tanggal yang tidak terbaca sebagai ISO ditolak`() {
        // Bisa terjadi kalau OCR membaca tanggal e-KTP dengan format lain.
        assertEquals(
            R.string.buka_rekening_dp_error_tanggal,
            form(tanggalLahir = "21-04-1995").tanggalLahirError(),
        )
    }

    @Test
    fun `tanggal lahir wajar diterima`() {
        assertNull(form(tanggalLahir = "1995-04-21").tanggalLahirError())
        assertNull(form(tanggalLahir = LocalDate.now().toString()).tanggalLahirError())
    }

    // -- Penyaringan input -----------------------------------------------------

    @Test
    fun `kode pos hanya menerima lima angka`() {
        val form = DataPribadiForm().withField(DataPribadiField.KODE_POS, "12a34567")
        assertEquals("12345", form.kodePos)
    }

    @Test
    fun `rt rw hanya menerima angka dan garis miring`() {
        val form = DataPribadiForm().withField(DataPribadiField.RT_RW, "00a3/0b05")
        assertEquals("003/005", form.rtRw)
    }

    @Test
    fun `rt rw tanpa pemisah ditolak`() {
        assertEquals(
            R.string.buka_rekening_dp_error_rt_rw,
            form(rtRw = "003005").errorOf(DataPribadiField.RT_RW),
        )
    }

    @Test
    fun `email tanpa domain lengkap ditolak`() {
        assertEquals(
            R.string.buka_rekening_dp_error_email,
            form(email = "ardan@example").errorOf(DataPribadiField.EMAIL),
        )
    }

    // -- Gerbang submit --------------------------------------------------------

    @Test
    fun `form lengkap dinyatakan sah`() {
        assertTrue(form().isValid)
    }

    @Test
    fun `satu field kosong membatalkan seluruh form`() {
        assertFalse(form(kodePos = "").isValid)
        assertFalse(form(tanggalLahir = "").isValid)
    }

    // -- Penyamaran nomor tujuan OTP ------------------------------------------

    @Test
    fun `samaran nomor sama dengan bentuk otp_sent_to dari server`() {
        // Contoh di `06-BUKA-REKENING-API-SPEC.md` §3a: 081234568889 -> 0812****8889.
        val masked = "081234568889".maskedPhoneOrNull()
        assertEquals("0812", masked?.prefix)
        assertEquals("8889", masked?.suffix)
    }

    @Test
    fun `semua bentuk nomor yang sah menghasilkan samaran yang sama`() {
        // Cadangan ini menggantikan `otp_sent_to` saat server tidak mengirimnya, jadi
        // hasilnya tidak boleh berubah hanya karena nasabah mengetik +62 atau pakai tanda
        // hubung — kalau berbeda, nomor di layar berganti bentuk tergantung cara mengetik.
        val samaran = listOf(
            "081234568889",
            "6281234568889",
            "+6281234568889",
            "0812-3456-8889",
            "81234568889",
        ).map { it.maskedPhoneOrNull() }

        assertEquals(listOf(MaskedPhone(prefix = "0812", suffix = "8889")), samaran.distinct())
    }

    @Test
    fun `nomor yang tidak sah tidak menghasilkan samaran`() {
        // Null-lah yang membuat layar jatuh ke keterangan umum alih-alih memotong
        // sembarang teks jadi "0812****".
        listOf("", "08", "0801234567890", "08123456789012345", "jelas-bukan-nomor")
            .forEach { assertNull("tersamar padahal tidak sah: $it", it.maskedPhoneOrNull()) }
    }

    private fun form(
        tanggalLahir: String = "1995-04-21",
        rtRw: String = "003/005",
        kodePos: String = "12190",
        nomorHp: String = "081234568889",
        email: String = "m.ardan@example.com",
    ) = DataPribadiForm(
        tempatLahir = "Jakarta",
        tanggalLahir = tanggalLahir,
        alamatLengkap = "Jl. Sudirman Kav. 45 No. 12B",
        rtRw = rtRw,
        kodePos = kodePos,
        kelurahan = "Senayan",
        kecamatan = "Kebayoran Baru",
        kota = "Jakarta Selatan",
        provinsi = "DKI Jakarta",
        nomorHp = nomorHp,
        email = email,
    )
}
