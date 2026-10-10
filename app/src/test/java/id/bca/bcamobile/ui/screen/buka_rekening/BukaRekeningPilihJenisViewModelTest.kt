package id.bca.bcamobile.ui.screen.buka_rekening

import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.model.ProductAvailability
import id.bca.bcamobile.domain.onboarding.model.ProductAvailabilityStatus
import id.bca.bcamobile.domain.onboarding.model.ProductBadge
import id.bca.bcamobile.domain.onboarding.model.ProductStyle
import id.bca.bcamobile.domain.onboarding.model.ProductType
import id.bca.bcamobile.domain.onboarding.model.ProductUnavailableReason
import id.bca.bcamobile.domain.onboarding.model.SavingsProduct
import id.bca.bcamobile.domain.onboarding.model.SavingsProductCatalog
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.pilih_jenis.BukaRekeningPilihJenisViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.pilih_jenis.PilihJenisEvent
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Layar Pilih Jenis Rekening setelah katalognya pindah ke server.
 *
 * Dua hal yang diuji di sini adalah dua hal yang paling mudah rusak tanpa suara:
 *
 * 1. **Produk dikenali dari kodenya, bukan dari posisi barisnya.** Versi sebelumnya
 *    memetakan indeks baris ke ordinal `ProductType`, jadi satu perubahan `display_order`
 *    di server membuat nasabah membuka rekening yang bukan pilihannya — tanpa error.
 * 2. **Katalog yang gagal dimuat tidak menghentikan apa pun.** Layar punya daftar bawaan,
 *    dan `POST /sessions` tidak menuntut katalog ada.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BukaRekeningPilihJenisViewModelTest {

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
    fun `katalog dimuat saat diminta dan default server yang terpilih`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        repo.savingsProductsResult = DataResult.Success(catalog())
        val store = BukaRekeningSessionStore()

        val viewModel = BukaRekeningPilihJenisViewModel(repo, store)
        viewModel.onEvent(PilihJenisEvent.ProductCatalogRequested)
        runCurrent()

        assertEquals(1, repo.savingsProductsCount)
        // Baris pertama (`display_order = 1`) sedang tutup dan `is_default` ada di
        // TAHAPAN_XPRESI. Yang terpilih harus produk itu, bukan baris teratas.
        assertEquals(ProductType.TAHAPAN_XPRESI, store.current.selectedProductType)
    }

    @Test
    fun `tanpa penanda default, produk pertama yang bisa dipilih yang terpilih`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            repo.savingsProductsResult = DataResult.Success(
                catalog().let { c -> c.copy(products = c.products.map { it.copy(isDefault = false) }) },
            )
            val store = BukaRekeningSessionStore()

            val viewModel = BukaRekeningPilihJenisViewModel(repo, store)
            viewModel.onEvent(PilihJenisEvent.ProductCatalogRequested)
            runCurrent()

            // TAHAPAN_BCA urutan pertama tapi tutup, jadi yang terpilih yang berikutnya.
            assertEquals(ProductType.TAHAPAN_XPRESI, store.current.selectedProductType)
        }

    @Test
    fun `katalog gagal tidak menulis error dan tidak memilih apa pun`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        // Default fake sudah Failure — ini keadaan `503 ONBOARDING_CATALOG_UNAVAILABLE`
        // maupun perangkat offline.
        val store = BukaRekeningSessionStore()

        val viewModel = BukaRekeningPilihJenisViewModel(repo, store)
        viewModel.onEvent(PilihJenisEvent.ProductCatalogRequested)
        runCurrent()

        // Error di langkah pertama akan menghentikan pendaftaran yang masih bisa jalan.
        assertNull(store.current.error)
        assertNull(store.current.productCatalog)
        // Layar memakai daftar bawaan, dan pilihannya baru terisi saat nasabah menekan
        // kartunya — daftar bawaan membawa ProductType-nya sendiri.
        assertNull(store.current.selectedProductType)
    }

    @Test
    fun `pilihan nasabah menang atas default server`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        repo.savingsProductsResult = DataResult.Success(catalog())
        val store = BukaRekeningSessionStore()
        store.update { it.copy(selectedProductType = ProductType.TABUNGANKU) }

        val viewModel = BukaRekeningPilihJenisViewModel(repo, store)
        viewModel.onEvent(PilihJenisEvent.ProductCatalogRequested)
        runCurrent()

        // Nasabah yang kembali lewat Back tidak boleh kehilangan pilihannya.
        assertEquals(ProductType.TABUNGANKU, store.current.selectedProductType)
    }

    @Test
    fun `katalog yang sudah ada tidak ditarik ulang`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        repo.savingsProductsResult = DataResult.Success(catalog())
        val store = BukaRekeningSessionStore()

        val viewModel = BukaRekeningPilihJenisViewModel(repo, store)
        viewModel.onEvent(PilihJenisEvent.ProductCatalogRequested)
        runCurrent()
        viewModel.onEvent(PilihJenisEvent.ProductCatalogRequested)
        runCurrent()

        assertEquals(1, repo.savingsProductsCount)
    }

    @Test
    fun `katalog yang sudah lewat lima menit ditarik ulang`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        repo.savingsProductsResult = DataResult.Success(catalog())
        val store = BukaRekeningSessionStore()

        val viewModel = BukaRekeningPilihJenisViewModel(repo, store)
        viewModel.onEvent(PilihJenisEvent.ProductCatalogRequested)
        runCurrent()
        assertEquals(1, repo.savingsProductsCount)

        // Umur palsu, bukan menunggu: `isProductCatalogFresh()` membaca jam dinding
        // (`System.currentTimeMillis()`), jadi waktu virtual `runTest` tidak memajukannya.
        store.update { it.copy(productCatalogFetchedAt = System.currentTimeMillis() - SIX_MINUTES) }
        viewModel.onEvent(PilihJenisEvent.ProductCatalogRequested)
        runCurrent()

        // Server menandai katalognya `max-age=300`; setoran awal yang basi adalah angka
        // yang salah di layar, dan angka itulah yang tercatat sebagai yang dilihat nasabah.
        assertEquals(2, repo.savingsProductsCount)
    }

    @Test
    fun `katalog tanpa penanda waktu dianggap basi`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        repo.savingsProductsResult = DataResult.Success(catalog())
        val store = BukaRekeningSessionStore()
        // Katalog terisi tanpa `productCatalogFetchedAt` — umurnya tidak diketahui.
        store.update { it.copy(productCatalog = catalog()) }

        val viewModel = BukaRekeningPilihJenisViewModel(repo, store)
        viewModel.onEvent(PilihJenisEvent.ProductCatalogRequested)
        runCurrent()

        assertEquals(1, repo.savingsProductsCount)
    }

    @Test
    fun `muat ulang menarik katalog lagi setelah gagal`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val store = BukaRekeningSessionStore()

        val viewModel = BukaRekeningPilihJenisViewModel(repo, store)
        viewModel.onEvent(PilihJenisEvent.ProductCatalogRequested)
        runCurrent()
        assertNull(store.current.productCatalog)

        // Sebelum ada event ini, satu-satunya jalan mencoba lagi adalah keluar-masuk flow.
        repo.savingsProductsResult = DataResult.Success(catalog())
        viewModel.onEvent(PilihJenisEvent.ProductCatalogReloadRequested)
        runCurrent()

        assertEquals(2, repo.savingsProductsCount)
        assertEquals(ProductType.TAHAPAN_XPRESI, store.current.selectedProductType)
    }

    @Test
    fun `muat ulang tidak menyentuh isLoading bersama`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        repo.savingsProductsResult = DataResult.Success(catalog())
        val store = BukaRekeningSessionStore()
        val viewModel = BukaRekeningPilihJenisViewModel(repo, store)

        viewModel.onEvent(PilihJenisEvent.ProductCatalogReloadRequested)
        // Belum `runCurrent()`: ini saat request sedang jalan.
        assertTrue(store.current.isRefreshingProductCatalog)
        // `isLoading` membuat layar mengganti isinya dengan spinner — justru mengosongkan
        // daftar yang sedang dibaca nasabah.
        assertFalse(store.current.isLoading)

        runCurrent()
        assertFalse(store.current.isRefreshingProductCatalog)
    }

    @Test
    fun `muat ulang ganda saat masih jalan hanya satu request`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        repo.savingsProductsResult = DataResult.Success(catalog())
        val store = BukaRekeningSessionStore()
        val viewModel = BukaRekeningPilihJenisViewModel(repo, store)

        viewModel.onEvent(PilihJenisEvent.ProductCatalogReloadRequested)
        viewModel.onEvent(PilihJenisEvent.ProductCatalogReloadRequested)
        runCurrent()

        assertEquals(1, repo.savingsProductsCount)
    }

    @Test
    fun `produk tersimpan sebagai kode`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val store = BukaRekeningSessionStore()

        val viewModel = BukaRekeningPilihJenisViewModel(repo, store)
        viewModel.onEvent(PilihJenisEvent.ProductSelected(ProductType.TABUNGANKU))
        runCurrent()

        assertEquals(ProductType.TABUNGANKU, store.current.selectedProductType)
    }

    // -- Helper ----------------------------------------------------------------

    /**
     * Katalog yang sengaja dibuat "menjebak": baris teratas sedang tutup, dan `is_default`
     * ada di baris kedua. Katalog yang urutannya kebetulan benar tidak akan pernah
     * memperlihatkan bug pemetaan indeks.
     */
    private fun catalog() = SavingsProductCatalog(
        catalogVersion = "2026-10-07.1",
        page = null,
        products = listOf(
            product(
                type = ProductType.TAHAPAN_BCA,
                displayOrder = 1,
                status = ProductAvailabilityStatus.DISABLED,
                reason = ProductUnavailableReason.MAINTENANCE,
            ),
            product(type = ProductType.TAHAPAN_XPRESI, displayOrder = 2, isDefault = true),
            product(type = ProductType.TABUNGANKU, displayOrder = 3),
        ),
    )

    private fun product(
        type: ProductType,
        displayOrder: Int,
        isDefault: Boolean = false,
        status: ProductAvailabilityStatus = ProductAvailabilityStatus.AVAILABLE,
        reason: ProductUnavailableReason? = null,
    ) = SavingsProduct(
        type = type,
        name = type.wireValue,
        description = "",
        minInitialDeposit = 500_000,
        currency = "IDR",
        iconKey = "WALLET",
        style = ProductStyle.PRIMARY,
        features = emptyList(),
        isPopular = false,
        badge = ProductBadge.NONE,
        isDefault = isDefault,
        displayOrder = displayOrder,
        availability = ProductAvailability(status = status, reason = reason),
    )

    private companion object {
        /** Lewat `max-age=300` server, jadi katalog seumur ini sudah basi. */
        const val SIX_MINUTES = 6 * 60 * 1000L
    }
}
