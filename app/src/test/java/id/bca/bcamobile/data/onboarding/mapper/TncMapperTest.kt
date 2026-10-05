package id.bca.bcamobile.data.onboarding.mapper

import id.bca.bcamobile.data.onboarding.remote.dto.TncConsentDto
import id.bca.bcamobile.data.onboarding.remote.dto.TncNoticeDto
import id.bca.bcamobile.data.onboarding.remote.dto.TncResponse
import id.bca.bcamobile.data.onboarding.remote.dto.TncSectionDto
import id.bca.bcamobile.data.onboarding.remote.dto.TncTrustBannerDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pemetaan respons S&K ke dokumen domain.
 *
 * Fokusnya satu keputusan: dua field mana yang **tidak** boleh jatuh ke "kosong lalu
 * lanjut". Tanpa `version` tidak ada yang bisa dikirim sebagai `accepted_tnc_version`,
 * dan tanpa `sections` layar menawarkan persetujuan atas teks hukum yang tidak terpampang.
 */
class TncMapperTest {

    @Test
    fun `respons lengkap dipetakan apa adanya`() {
        val document = fullResponse().toDomain()!!

        assertEquals("2026-09-01", document.version)
        assertEquals("Persetujuan Resmi Nasabah", document.trustTitle)
        assertEquals("PENTING", document.notice?.label)
        assertEquals("S&K BCA", document.consent?.link)
        assertEquals("Setuju & Lanjutkan", document.agreeCta)
    }

    @Test
    fun `urutan pasal dari server dipertahankan`() {
        // Server sudah mengurutkannya; menyortir ulang di client akan mengacak nomor
        // pasal yang tertulis di dalam judulnya sendiri.
        val document = fullResponse().toDomain()!!

        assertEquals(
            listOf("1. Pasal satu", "2. Pasal dua", "3. Pasal tiga"),
            document.sections.map { it.title },
        )
    }

    @Test
    fun `dokumen tanpa versi bukan dokumen yang bisa disetujui`() {
        assertNull(fullResponse().copy(version = "").toDomain())
    }

    @Test
    fun `dokumen tanpa pasal bukan dokumen yang bisa disetujui`() {
        assertNull(fullResponse().copy(sections = emptyList()).toDomain())
    }

    @Test
    fun `objek bersarang yang hilang tidak menggagalkan dokumen`() {
        // Banner, notice, dan consent yang hilang hanya menyembunyikan bagiannya di layar.
        // Menolak seluruh dokumen karena salah satunya absen jauh lebih merugikan.
        val document = fullResponse()
            .copy(trustBanner = null, notice = null, consent = null)
            .toDomain()

        assertNotNull(document)
        assertEquals("", document!!.trustTitle)
        assertNull(document.notice)
        assertNull(document.consent)
    }

    @Test
    fun `icon_key diteruskan sebagai kunci, bukan diterjemahkan di lapisan data`() {
        // Pemetaan ke drawable terjadi di layar, yang punya cabang cadangan. Kalau
        // diterjemahkan di sini, kunci baru dari server tidak punya jalan keluar.
        val document = fullResponse()
            .copy(sections = listOf(TncSectionDto(iconKey = "KUNCI_BARU", title = "t", body = "b")))
            .toDomain()!!

        assertEquals("KUNCI_BARU", document.sections.single().iconKey)
    }

    private fun fullResponse() = TncResponse(
        version = "2026-09-01",
        heading = "Syarat & Ketentuan Pembukaan Rekening",
        subtitle = "Mohon baca dan pahami.",
        trustBanner = TncTrustBannerDto(
            title = "Persetujuan Resmi Nasabah",
            subtitle = "Diawasi OJK",
        ),
        sections = listOf(
            TncSectionDto(iconKey = "ACCOUNT_BOX", title = "1. Pasal satu", body = "isi satu"),
            TncSectionDto(iconKey = "LOCK", title = "2. Pasal dua", body = "isi dua"),
            TncSectionDto(iconKey = "SAVINGS", title = "3. Pasal tiga", body = "isi tiga"),
        ),
        notice = TncNoticeDto(label = "PENTING", body = "Pastikan tempat tenang."),
        consent = TncConsentDto(prefix = "Saya setuju ", link = "S&K BCA", suffix = "."),
        agreeCta = "Setuju & Lanjutkan",
        effectiveFrom = "2026-09-01T00:00:00+07:00",
        isActive = true,
    )
}
