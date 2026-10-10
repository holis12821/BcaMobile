package id.bca.bcamobile.ui.screen.buka_rekening.pilih_jenis

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.bca.bcamobile.R
import id.bca.bcamobile.domain.onboarding.model.ProductBadge
import id.bca.bcamobile.domain.onboarding.model.ProductConsent
import id.bca.bcamobile.domain.onboarding.model.ProductNotice
import id.bca.bcamobile.domain.onboarding.model.ProductStyle
import id.bca.bcamobile.domain.onboarding.model.ProductType
import id.bca.bcamobile.domain.onboarding.model.ProductUnavailableReason
import id.bca.bcamobile.domain.onboarding.model.SavingsProduct
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState
import id.bca.bcamobile.ui.screen.buka_rekening.common.formatAmount
import id.bca.bcamobile.ui.screen.buka_rekening.common.resolve

data class BukaRekeningPilihJenisUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val heading: String = "",
    val subtitle: String = "",
    val depositLabel: String = "",
    val ctaLabel: String = "",
    val notice: JenisRekeningNotice? = null,
    val consent: JenisRekeningConsent? = null,
    val jenisRekeningList: List<JenisRekening> = emptyList(),
    /** Baris yang sedang terpilih; diturunkan dari kode produk, bukan disimpan sendiri. */
    val selectedIndex: Int = 0,
    /**
     * Daftar yang tampil berasal dari `strings.xml`, bukan dari server.
     *
     * Ditandai di layar — **bukan** sebagai error. Tanpa penanda, nasabah tidak punya
     * alasan menekan "Muat ulang", dan tombol yang tidak punya alasan untuk ada sama
     * saja dengan tidak ada tombol.
     */
    val isShowingFallback: Boolean = false,
    /** Muat ulang sedang jalan; tombolnya mati sebentar, daftarnya tetap terbaca. */
    val isRefreshing: Boolean = false,
)

/** Kotak persiapan dokumen. [iconKey] masih kunci server — dipetakan di layar. */
data class JenisRekeningNotice(
    val iconKey: String,
    val title: String,
    val body: String,
)

data class JenisRekeningConsent(
    val prefix: String,
    val link: String,
    val suffix: String,
)

/**
 * Isi layar: dari katalog server kalau ada, dari `strings.xml` kalau tidak.
 *
 * Daftar bawaan bukan pratinjau — ia benar-benar dipakai saat katalog dimatikan server
 * atau perangkat sedang offline, supaya langkah pertama buka rekening tidak pernah jadi
 * layar kosong. Yang penting: tiap entri bawaan menyebut [ProductType]-nya sendiri, jadi
 * jalur fallback pun tidak bergantung pada urutan baris.
 */
@Composable
fun BukaRekeningFlowState.toPilihJenisUiState(): BukaRekeningPilihJenisUiState {
    val catalog = productCatalog
    val items = catalog?.sortedProducts?.map { it.toJenisRekening() }
        ?: defaultJenisRekeningList()
    val selected = items.indexOfFirst { it.productType == selectedProductType }

    return BukaRekeningPilihJenisUiState(
        isLoading = isLoading,
        error = error?.resolve(),
        heading = catalog?.page?.heading.orBlankRes(R.string.buka_rekening_pilih_jenis_heading),
        subtitle = catalog?.page?.subtitle.orBlankRes(R.string.buka_rekening_pilih_subtitle),
        depositLabel = catalog?.page?.depositLabel
            .orBlankRes(R.string.buka_rekening_setoran_awal),
        ctaLabel = catalog?.page?.ctaLabel.orBlankRes(R.string.buka_rekening_lanjut),
        notice = catalog?.page?.notice?.toUi() ?: defaultNotice(),
        consent = catalog?.page?.consent?.toUi() ?: defaultConsent(),
        jenisRekeningList = items,
        // Belum memilih apa pun berarti baris pertama yang disorot — bukan "tidak ada".
        selectedIndex = selected.takeIf { it >= 0 } ?: 0,
        isShowingFallback = catalog == null,
        isRefreshing = isRefreshingProductCatalog,
    )
}

@Composable
private fun String?.orBlankRes(resId: Int): String =
    this?.takeIf { it.isNotBlank() } ?: stringResource(resId)

@Composable
private fun ProductNotice.toUi(): JenisRekeningNotice = JenisRekeningNotice(
    iconKey = iconKey,
    title = title.orBlankRes(R.string.buka_rekening_persiapan_dokumen),
    body = body.orBlankRes(R.string.buka_rekening_persiapan_dokumen_desc),
)

@Composable
private fun ProductConsent.toUi(): JenisRekeningConsent = JenisRekeningConsent(
    prefix = prefix.orBlankRes(R.string.buka_rekening_syarat_prefix),
    link = link.orBlankRes(R.string.buka_rekening_syarat_link),
    suffix = suffix.orBlankRes(R.string.buka_rekening_syarat_suffix),
)

@Composable
private fun defaultNotice(): JenisRekeningNotice = JenisRekeningNotice(
    iconKey = ICON_KEY_INFO,
    title = stringResource(R.string.buka_rekening_persiapan_dokumen),
    body = stringResource(R.string.buka_rekening_persiapan_dokumen_desc),
)

@Composable
private fun defaultConsent(): JenisRekeningConsent = JenisRekeningConsent(
    prefix = stringResource(R.string.buka_rekening_syarat_prefix),
    link = stringResource(R.string.buka_rekening_syarat_link),
    suffix = stringResource(R.string.buka_rekening_syarat_suffix),
)

@Composable
private fun SavingsProduct.toJenisRekening(): JenisRekening = JenisRekening(
    productType = type,
    nama = name,
    deskripsi = description,
    // Server mengirim rupiah penuh; pemformatannya milik client.
    setoranAwalMinimum = stringResource(
        R.string.buka_rekening_jenis_setoran_format,
        formatAmount(minInitialDeposit),
    ),
    fitur = features,
    iconRes = productIconRes(iconKey),
    gaya = style.toGaya(),
    badge = badge.labelRes()?.let { stringResource(it) }.orEmpty(),
    isTersedia = availability.isSelectable,
    keteranganTidakTersedia = availability.takeIf { !it.isSelectable }
        ?.let { stringResource(it.reason.labelRes()) },
)

private fun ProductStyle.toGaya(): JenisRekeningGaya = when (this) {
    ProductStyle.PRIMARY -> JenisRekeningGaya.PRIMARY
    ProductStyle.SECONDARY -> JenisRekeningGaya.SECONDARY
    ProductStyle.NEUTRAL -> JenisRekeningGaya.NEUTRAL
}

/** `NONE` berarti server memang tidak mengirim badge; jangan mengarang label. */
private fun ProductBadge.labelRes(): Int? = when (this) {
    ProductBadge.MOST_POPULAR -> R.string.buka_rekening_paling_populer
    ProductBadge.NONE -> null
}

private fun ProductUnavailableReason?.labelRes(): Int = when (this) {
    ProductUnavailableReason.TEMPORARILY_DISABLED ->
        R.string.buka_rekening_jenis_tidak_tersedia_nonaktif
    ProductUnavailableReason.MAINTENANCE ->
        R.string.buka_rekening_jenis_tidak_tersedia_perbaikan
    ProductUnavailableReason.COMING_SOON ->
        R.string.buka_rekening_jenis_tidak_tersedia_segera
    ProductUnavailableReason.UNKNOWN, null ->
        R.string.buka_rekening_jenis_tidak_tersedia_umum
}
