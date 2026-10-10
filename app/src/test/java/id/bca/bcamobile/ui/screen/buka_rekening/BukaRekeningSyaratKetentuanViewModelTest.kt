package id.bca.bcamobile.ui.screen.buka_rekening

import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.model.OnboardingSession
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.domain.onboarding.model.ProductType
import id.bca.bcamobile.domain.onboarding.model.TncConsent
import id.bca.bcamobile.domain.onboarding.model.TncDocument
import id.bca.bcamobile.domain.onboarding.model.TncNotice
import id.bca.bcamobile.domain.onboarding.model.TncSection
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.syarat_ketentuan.BukaRekeningSyaratKetentuanViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.syarat_ketentuan.SyaratKetentuanEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Layar Syarat & Ketentuan setelah teksnya pindah ke server.
 *
 * Inti kontraknya satu hal: versi yang tercatat sebagai disetujui harus berasal dari
 * dokumen yang **benar-benar terpampang**, bukan konstanta. Itu yang server periksa, dan
 * selisihnya dijawab `409 TNC_VERSION_OUTDATED` — yang pemulihannya memuat ulang, bukan
 * mengulang request dengan nilai yang sama.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BukaRekeningSyaratKetentuanViewModelTest {

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
    fun `dokumen dimuat saat layar dibuka, bukan saat tombol ditekan`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        repo.tncResult = DataResult.Success(document())

        val viewModel = viewModel(repo, store())
        runCurrent()

        // Nasabah harus bisa membaca sebelum menyetujui.
        assertEquals(1, repo.tncCount)
        assertEquals("2026-09-01", viewModel.tnc.value.document?.version)
        assertNull(viewModel.tnc.value.error)
    }

    @Test
    fun `versi yang dikirim berasal dari dokumen yang terpampang`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        // Versi yang tidak mungkin ditebak konstanta mana pun.
        repo.tncResult = DataResult.Success(document(version = "2027-07-07"))
        repo.createSessionResult = DataResult.Success(session())
        val store = store()

        val viewModel = viewModel(repo, store)
        runCurrent()
        viewModel.onEvent(SyaratKetentuanEvent.TncAccepted)
        runCurrent()

        assertEquals(listOf("2027-07-07"), repo.createSessionTncVersions)
    }

    @Test
    fun `tanpa dokumen tidak ada sesi yang dibuat`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        repo.tncResult = DataResult.Failure(ApiFailure.Network)
        repo.createSessionResult = DataResult.Success(session())

        val viewModel = viewModel(repo, store())
        runCurrent()
        viewModel.onEvent(SyaratKetentuanEvent.TncAccepted)
        runCurrent()

        // Tanpa penjaga ini sesi bisa lahir dengan accepted_tnc_version kosong, dan
        // backend menolaknya 400 VALIDATION_ERROR di tempat yang membingungkan.
        assertTrue(repo.createSessionTncVersions.isEmpty())
        assertNotNull(viewModel.tnc.value.error)
    }

    @Test
    fun `gagal muat bisa dicoba ulang`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        repo.tncResult = DataResult.Failure(ApiFailure.Network)

        val viewModel = viewModel(repo, store())
        runCurrent()
        assertEquals(1, repo.tncCount)

        repo.tncResult = DataResult.Success(document())
        viewModel.onEvent(SyaratKetentuanEvent.TncReloadRequested)
        runCurrent()

        assertEquals(2, repo.tncCount)
        assertNull(viewModel.tnc.value.error)
        assertEquals("2026-09-01", viewModel.tnc.value.document?.version)
    }

    @Test
    fun `409 memuat ulang S&K dan memberi tahu nasabah`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        repo.tncResult = DataResult.Success(document(version = "2026-09-01"))
        repo.createSessionResult = DataResult.Failure(
            ApiFailure.TncOutdated(
                currentVersion = "2026-12-01",
                message = "Syarat & Ketentuan telah diperbarui.",
            ),
        )
        val store = store()
        val effects = collectEffects(store)

        val viewModel = viewModel(repo, store)
        runCurrent()
        // Server pindah versi tepat setelah layar ini memuat yang lama.
        repo.tncResult = DataResult.Success(document(version = "2026-12-01"))
        viewModel.onEvent(SyaratKetentuanEvent.TncAccepted)
        runCurrent()

        // Muat ulang, bukan mengulang request: mengirim versi lama lagi dijamin gagal.
        assertEquals(2, repo.tncCount)
        assertEquals("2026-12-01", viewModel.tnc.value.document?.version)
        assertTrue(effects.any { it is BukaRekeningSideEffect.ShowMessage })
        // Versi baru itu juga yang membuat `remember(version)` di layar mengosongkan
        // centang persetujuan — nasabah belum menyetujui teks yang baru berganti.
        assertTrue(
            "versi harus berganti supaya centang lahir ulang",
            viewModel.tnc.value.document?.version != "2026-09-01",
        )
    }

    @Test
    fun `409 tidak memancarkan AdvanceTo`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        repo.tncResult = DataResult.Success(document())
        repo.createSessionResult = DataResult.Failure(
            ApiFailure.TncOutdated(currentVersion = "2026-12-01"),
        )
        val store = store()
        val effects = collectEffects(store)

        val viewModel = viewModel(repo, store)
        runCurrent()
        viewModel.onEvent(SyaratKetentuanEvent.TncAccepted)
        runCurrent()

        // Sesi tidak lahir, jadi tidak ada langkah berikutnya untuk dituju.
        assertTrue(effects.none { it is BukaRekeningSideEffect.AdvanceTo })
        assertNull(store.current.sessionId)
    }

    @Test
    fun `sesi yang lahir mengikuti step dari server`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        repo.tncResult = DataResult.Success(document())
        repo.createSessionResult = DataResult.Success(session(OnboardingStep.OCR))
        val store = store()
        val effects = collectEffects(store)

        val viewModel = viewModel(repo, store)
        runCurrent()
        viewModel.onEvent(SyaratKetentuanEvent.TncAccepted)
        runCurrent()

        assertEquals("onb_1", store.current.sessionId)
        assertEquals(
            BukaRekeningSideEffect.AdvanceTo(OnboardingStep.OCR),
            effects.last(),
        )
    }

    // -- Helper ----------------------------------------------------------------

    private fun viewModel(
        repo: FakeOnboardingRepository,
        store: BukaRekeningSessionStore,
    ) = BukaRekeningSyaratKetentuanViewModel(repo, store)

    /** Produk sudah dipilih dua layar sebelumnya; tanpa itu createSession memang berhenti. */
    private fun store() = BukaRekeningSessionStore().apply {
        update { it.copy(selectedProductType = ProductType.TAHAPAN_BCA) }
    }

    private fun TestScope.collectEffects(
        store: BukaRekeningSessionStore,
    ): List<BukaRekeningSideEffect> {
        val effects = mutableListOf<BukaRekeningSideEffect>()
        backgroundScope.launch { store.sideEffect.collect { effects += it } }
        runCurrent()
        return effects
    }

    private fun document(version: String = "2026-09-01") = TncDocument(
        version = version,
        heading = "Syarat & Ketentuan Pembukaan Rekening",
        subtitle = "Mohon baca dan pahami.",
        trustTitle = "Persetujuan Resmi Nasabah",
        trustSubtitle = "Diawasi OJK",
        sections = listOf(
            TncSection(iconKey = "ACCOUNT_BOX", title = "1. Ketentuan Umum", body = "isi"),
        ),
        notice = TncNotice(label = "PENTING", body = "Pastikan tempat tenang."),
        consent = TncConsent(prefix = "Saya setuju ", link = "S&K BCA", suffix = "."),
        agreeCta = "Setuju & Lanjutkan",
        effectiveFrom = "2026-09-01T00:00:00+07:00",
        isActive = true,
    )

    private fun session(step: OnboardingStep = OnboardingStep.CARD_SELECTION) = OnboardingSession(
        sessionId = "onb_1",
        product = null,
        currentStep = step,
    )
}
