package id.bca.bcamobile.ui.screen.buka_rekening

import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.model.KtpData
import id.bca.bcamobile.domain.onboarding.model.KtpOcrResult
import id.bca.bcamobile.domain.onboarding.model.LocalKtpScan
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.hasil_foto.BukaRekeningHasilFotoViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.hasil_foto.HasilFotoEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Perilaku layar Hasil Foto.
 *
 * Yang diuji di sini sebagian besar adalah apa yang **harus tetap** terjadi
 * setelah unggahan: teks OCR on-device benar-benar terkirim, dan hasil baca
 * lokal tidak dibuang bersama berkas fotonya.
 *
 * Keduanya pernah salah sekaligus. Teks ML Kit ditampilkan di layar lalu
 * dibuang, sementara server menjawab dari teks hardcoded — jadi identitas yang
 * masuk ke form data pribadi milik orang lain. Dan `localScan` ikut dihapus
 * bersama `ktpPhoto`, sehingga satu-satunya sumber data setelah unggahan adalah
 * hasil server, tanpa pembanding kalau keduanya berbeda.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BukaRekeningHasilFotoViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `teks ocr on-device terkirim bersama foto`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val store = BukaRekeningSessionStore()
        val photo = tempPhoto()
        store.update { it.copy(ktpPhoto = photo, localScan = scan(RAW_TEXT)) }

        BukaRekeningHasilFotoViewModel(repo, store)
            .onEvent(HasilFotoEvent.OcrConfirmed)
        runCurrent()

        // Tanpa ini server tidak punya apa pun untuk dibaca, dan jawabannya
        // dulu berupa identitas yang tidak berhubungan dengan fotonya.
        assertEquals(RAW_TEXT, repo.lastClientOcrText)
    }

    @Test
    fun `tanpa scan lokal yang terkirim adalah teks kosong, bukan null`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            val store = BukaRekeningSessionStore()
            store.update { it.copy(ktpPhoto = tempPhoto(), localScan = null) }

            BukaRekeningHasilFotoViewModel(repo, store)
                .onEvent(HasilFotoEvent.OcrConfirmed)
            runCurrent()

            // Server menolak teks kosong dengan OCR_NOT_KTP. Itu jawaban yang
            // benar: tidak ada yang terbaca dari kartunya.
            assertEquals("", repo.lastClientOcrText)
        }

    @Test
    fun `scan lokal dipertahankan setelah unggahan, berkas fotonya tidak`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            repo.ocrResult = DataResult.Success(ocrResult())
            val store = BukaRekeningSessionStore()
            val photo = tempPhoto()
            store.update { it.copy(ktpPhoto = photo, localScan = scan(RAW_TEXT)) }

            BukaRekeningHasilFotoViewModel(repo, store)
                .onEvent(HasilFotoEvent.OcrConfirmed)
            runCurrent()

            val after = store.current
            // Salinan lokal foto tidak boleh tertinggal di cache.
            assertNull(after.ktpPhoto)
            assertFalse("berkas foto harus terhapus", photo.exists())
            // Hasil bacanya tetap ada sebagai pembanding hasil server.
            assertNotNull(after.localScan)
            assertEquals(RAW_TEXT, after.localScan?.rawText)
        }

    /**
     * Unggahan yang gagal harus **menyimpan** fotonya.
     *
     * Kalau tidak, satu kegagalan jaringan memaksa nasabah memfoto ulang KTP-nya
     * padahal fotonya sudah bagus — dan tombol coba lagi tidak punya apa pun
     * untuk dikirim.
     */
    @Test
    fun `unggahan gagal mempertahankan foto supaya bisa dicoba lagi`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            // Default fake-nya memang Failure; dinyatakan di sini supaya
            // maksudnya terbaca, bukan bergantung pada nilai bawaan.
            repo.ocrResult = DataResult.Failure(ApiFailure.Unknown)
            val store = BukaRekeningSessionStore()
            val photo = tempPhoto()
            store.update { it.copy(ktpPhoto = photo, localScan = scan(RAW_TEXT)) }

            BukaRekeningHasilFotoViewModel(repo, store)
                .onEvent(HasilFotoEvent.OcrConfirmed)
            runCurrent()

            val after = store.current
            assertNotNull("foto harus dipertahankan", after.ktpPhoto)
            assertTrue("berkas foto tidak boleh terhapus", photo.exists())
            assertNull("tidak ada hasil OCR server", after.ocr)
        }

    @Test
    fun `ambil ulang membersihkan foto dan kedua hasil baca`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val store = BukaRekeningSessionStore()
        val photo = tempPhoto()
        store.update {
            it.copy(ktpPhoto = photo, localScan = scan(RAW_TEXT), ocr = ocrResult())
        }

        BukaRekeningHasilFotoViewModel(repo, store)
            .onEvent(HasilFotoEvent.PhotoDiscarded)
        runCurrent()

        val after = store.current
        assertNull(after.ktpPhoto)
        assertNull(after.localScan)
        assertNull(after.ocr)
        assertFalse(photo.exists())
    }

    /**
     * Lencana "terverifikasi" hanya sah bila registri benar-benar dihubungi.
     *
     * Server dulu mengirim `dukcapil_match: true` untuk sesi yang tidak punya
     * registri sama sekali, jadi lencananya muncul tanpa ada yang memverifikasi.
     */
    @Test
    fun `dukcapil tidak dicek berarti belum terverifikasi tapi tidak memblokir`() {
        val store = BukaRekeningSessionStore()
        store.update {
            it.copy(ocr = ocrResult(dukcapilMatch = false, dukcapilChecked = false))
        }

        assertFalse(store.current.isOcrVerified)
    }

    @Test
    fun `dukcapil dicek dan cocok berarti terverifikasi`() {
        val store = BukaRekeningSessionStore()
        store.update {
            it.copy(ocr = ocrResult(dukcapilMatch = true, dukcapilChecked = true))
        }

        assertTrue(store.current.isOcrVerified)
    }

    @Test
    fun `dukcapil dicek tapi tidak cocok berarti belum terverifikasi`() {
        val store = BukaRekeningSessionStore()
        store.update {
            it.copy(ocr = ocrResult(dukcapilMatch = false, dukcapilChecked = true))
        }

        assertFalse(store.current.isOcrVerified)
    }

    // -- Pembantu --------------------------------------------------------------

    private fun tempPhoto(): File =
        File.createTempFile("ktp-test", ".jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3))
            deleteOnExit()
        }

    private fun scan(rawText: String) = LocalKtpScan(
        data = ktpData(),
        // Di atas MIN_ACCEPTABLE_ACCURACY, supaya `needsRetake` tidak ikut campur.
        accuracyPercent = 100.0,
        rawText = rawText,
    )

    private fun ocrResult(
        dukcapilMatch: Boolean = false,
        dukcapilChecked: Boolean = false,
    ) = KtpOcrResult(
        ocrId = "ocr_test",
        accuracyPercent = 81.25,
        extracted = ktpData(),
        dukcapilMatch = dukcapilMatch,
        dukcapilChecked = dukcapilChecked,
        sharpness = "HIGH",
        glareDetected = false,
        allCornersVisible = true,
    )

    private fun ktpData() = KtpData(
        nik = "3174082104950001",
        namaLengkap = "MUHAMMAD ARDAN PRAYOGI",
        tempatLahir = "JAKARTA",
        tanggalLahir = "1995-04-21",
        jenisKelamin = "LAKI_LAKI",
        alamat = "JL. SUDIRMAN KAV. 45",
        rtRw = "003/005",
        kelurahan = "SENAYAN",
        kecamatan = "KEBAYORAN BARU",
        kota = "JAKARTA SELATAN",
        provinsi = "DKI JAKARTA",
        agama = "ISLAM",
        statusPerkawinan = "BELUM KAWIN",
    )

    private companion object {
        const val RAW_TEXT = "PROVINSI DKI JAKARTA\nNIK : 3174082104950001\nNama : MUHAMMAD ARDAN PRAYOGI"
    }
}
