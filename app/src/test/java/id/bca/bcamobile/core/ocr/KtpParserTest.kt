package id.bca.bcamobile.core.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Teks contoh meniru keluaran ML Kit yang sebenarnya: label tidak selalu rapi,
 * titik dua kadang hilang, dan baris terpecah.
 */
class KtpParserTest {

    private val teksBersih = """
        PROVINSI DKI JAKARTA
        JAKARTA SELATAN
        NIK : 3174082104950001
        Nama : MUHAMMAD ARDAN PRAYOGI
        Tempat/Tgl Lahir : JAKARTA, 21-04-1995
        Jenis Kelamin : LAKI-LAKI Gol. Darah : O
        Alamat : JL. SUDIRMAN KAV. 45 NO. 12B
        RT/RW : 004/002
        Kel/Desa : SENAYAN
        Kecamatan : KEBAYORAN BARU
        Agama : ISLAM
        Status Perkawinan : BELUM KAWIN
        Pekerjaan : KARYAWAN SWASTA
        Kewarganegaraan : WNI
    """.trimIndent()

    @Test
    fun `ktp bersih terbaca lengkap`() {
        val hasil = KtpParser.parse(teksBersih)

        assertEquals("3174082104950001", hasil.data.nik)
        assertEquals("MUHAMMAD ARDAN PRAYOGI", hasil.data.namaLengkap)
        assertEquals("JAKARTA", hasil.data.tempatLahir)
        assertEquals("LAKI_LAKI", hasil.data.jenisKelamin)
        assertEquals("SENAYAN", hasil.data.kelurahan)
        assertEquals("KEBAYORAN BARU", hasil.data.kecamatan)
        assertEquals("ISLAM", hasil.data.agama)
        assertEquals("BELUM KAWIN", hasil.data.statusPerkawinan)
        assertEquals("DKI JAKARTA", hasil.data.provinsi)
        assertTrue(hasil.missingFields.isEmpty())
        assertEquals(100.0, hasil.accuracyPercent, 0.01)
    }

    @Test
    fun `tanggal lahir dinormalkan ke format ISO`() {
        assertEquals("1995-04-21", KtpParser.parse(teksBersih).data.tanggalLahir)
    }

    @Test
    fun `rt rw dinormalkan jadi satu bentuk`() {
        assertEquals("004/002", KtpParser.parse(teksBersih).data.rtRw)
        val spasi = KtpParser.parse("RT/RW : 004 / 002")
        assertEquals("004/002", spasi.data.rtRw)
    }

    @Test
    fun `nik terbaca walau labelnya salah baca`() {
        val rusak = "N1K 3174082104950001"
        assertEquals("3174082104950001", KtpParser.parse(rusak).data.nik)
    }

    @Test
    fun `titik dua hilang tetap terbaca`() {
        val tanpaTitikDua = """
            Nama MUHAMMAD ARDAN PRAYOGI
            Agama ISLAM
        """.trimIndent()
        val hasil = KtpParser.parse(tanpaTitikDua)
        assertEquals("MUHAMMAD ARDAN PRAYOGI", hasil.data.namaLengkap)
        assertEquals("ISLAM", hasil.data.agama)
    }

    @Test
    fun `jenis kelamin jatuh ke digit tanggal NIK saat barisnya hilang`() {
        // Digit tanggal 61 = 21 + 40, penanda perempuan.
        val hasil = KtpParser.parse("NIK : 3174086104950002")
        assertEquals("PEREMPUAN", hasil.data.jenisKelamin)
    }

    @Test
    fun `gol darah tidak ikut terbawa ke jenis kelamin`() {
        val hasil = KtpParser.parse("Jenis Kelamin : PEREMPUAN Gol. Darah : AB")
        assertEquals("PEREMPUAN", hasil.data.jenisKelamin)
    }

    @Test
    fun `akurasi turun dan field hilang tercatat saat teks tidak lengkap`() {
        val hasil = KtpParser.parse("NIK : 3174082104950001\nNama : BUDI SANTOSO")

        assertTrue(hasil.accuracyPercent < 80.0)
        assertTrue(hasil.missingFields.contains("alamat"))
        assertTrue(hasil.missingFields.contains("provinsi"))
        assertTrue(!hasil.missingFields.contains("nik"))
    }

    @Test
    fun `teks kosong menghasilkan akurasi nol`() {
        val hasil = KtpParser.parse("")
        assertEquals(0.0, hasil.accuracyPercent, 0.01)
        assertEquals(13, hasil.missingFields.size)
    }

    @Test
    fun `angka dan tanda baca dibuang dari nama`() {
        val hasil = KtpParser.parse("Nama : BUD1 SANT0SO ~")
        assertEquals("BUD SANTSO", hasil.data.namaLengkap)
    }

    @Test
    fun `masking nik hanya menyisakan empat digit di tiap ujung`() {
        assertEquals("3174********0001", KtpParser.maskNik("3174082104950001"))
    }

    @Test
    fun `kabupaten dikenali sebagai kota`() {
        val hasil = KtpParser.parse("PROVINSI JAWA BARAT\nKABUPATEN BOGOR")
        assertEquals("BOGOR", hasil.data.kota)
        assertEquals("JAWA BARAT", hasil.data.provinsi)
    }
}
