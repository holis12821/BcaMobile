package id.bca.bcamobile.ui.screen.buka_rekening

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aturan kredensial harus ditolak di client sebelum dienkripsi dan dikirim —
 * kontraknya ada di 06-BUKA-REKENING-API-SPEC.md bagian Simpan Kredensial.
 */
class CredentialValidationTest {

    @Test
    fun `kode akses enam karakter alfanumerik diterima`() {
        assertTrue("Bca202".isValidAccessCode())
    }

    @Test
    fun `kode akses selain enam karakter ditolak`() {
        assertFalse("Bca20".isValidAccessCode())
        assertFalse("Bca2021".isValidAccessCode())
    }

    @Test
    fun `kode akses dengan karakter non-alfanumerik ditolak`() {
        assertFalse("Bca-02".isValidAccessCode())
    }

    @Test
    fun `pin enam digit diterima`() {
        assertTrue("284913".isValidPin())
    }

    @Test
    fun `pin berisi huruf ditolak`() {
        assertFalse("28491a".isValidPin())
    }

    @Test
    fun `deret naik dan turun sama-sama ditolak`() {
        assertTrue("123456".isSequential())
        assertTrue("654321".isSequential())
        assertFalse("135790".isSequential())
    }

    @Test
    fun `karakter seragam ditolak`() {
        assertTrue("111111".isAllSameChar())
        assertFalse("111112".isAllSameChar())
    }

    @Test
    fun `flow valid hanya saat konfirmasi cocok dan pin berbeda dari kode akses`() {
        val valid = BukaRekeningFlowState(
            accessCode = "Bca202",
            confirmAccessCode = "Bca202",
            pin = "284913",
            confirmPin = "284913",
        )
        assertTrue(valid.isCredentialValid())

        assertFalse(valid.copy(confirmPin = "284914").isCredentialValid())
        assertFalse(valid.copy(confirmAccessCode = "Bca203").isCredentialValid())
    }

    @Test
    fun `pin yang sama persis dengan kode akses ditolak`() {
        val state = BukaRekeningFlowState(
            accessCode = "284913",
            confirmAccessCode = "284913",
            pin = "284913",
            confirmPin = "284913",
        )
        assertFalse(state.isCredentialValid())
    }

    @Test
    fun `nik dikelompokkan empat digit`() {
        assertEquals("3174 0821 0495 0001", "3174082104950001".groupNik())
    }

    @Test
    fun `tanggal ISO diubah ke format tampilan dan format ktp`() {
        assertEquals("21-04-1995", "1995-04-21".toKtpDate())
        assertEquals("1995-13-45", "1995-13-45".toKtpDate())
    }

    @Test
    fun `bagian alamat yang kosong tidak ikut dirangkai`() {
        assertEquals("Senayan, Jakarta", joinNonBlank("Senayan", "", "Jakarta"))
    }
}
