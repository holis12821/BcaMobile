package id.bca.bcamobile.ui.screen.notifikasi

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.AppTextStyle
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Elevation
import id.bca.bcamobile.ui.theme.IconTint
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.SpacingHalfStep
import id.bca.bcamobile.ui.theme.StrokeWidth

// ── State Models ─────────────────────────────────────────────────────────

enum class NotifikasiTab {
    SEMUA,
    TRANSAKSI,
    PROMO,
    INFO,
}

enum class NotifikasiJenis {
    TRANSAKSI,
    PROMO,
    KEAMANAN,
    INFO,
}

enum class NotifikasiArah {
    MASUK,
    KELUAR,
    TIDAK_DIKETAHUI,
}

/**
 * Kelompok tanggal tempat satu notifikasi jatuh.
 *
 * Server mengirim daftar rata terurut dari yang terbaru, jadi kelompoknya
 * selalu berurutan dan header cukup dipancarkan setiap kali nilainya berganti —
 * tidak ada pengurutan ulang di sisi tampilan.
 */
enum class NotifikasiGrup {
    HARI_INI,
    KEMARIN,
    MINGGU_INI,
    LEBIH_LAMA,
}

/**
 * Aksi pintas di kaki kartu notifikasi.
 *
 * **Belum ada sumber datanya.** `GET /notifications` hanya membalas `type`,
 * `title`, dan `body`; tidak ada field aksi maupun deeplink. Nilainya karena
 * itu selalu `null` di aplikasi dan hanya terisi di `@Preview`, sama seperti
 * [NotifikasiArah] yang menunggu field arah dari backend. Begitu kontraknya
 * menambahkan aksi, cukup dipetakan di `NotifikasiViewModel`.
 */
enum class NotifikasiAksi {
    BUKTI_TRANSFER,
    QRIS,
    SALIN_REFERENSI,
}

data class NotifikasiItem(
    val id: String,
    val jenis: NotifikasiJenis,
    val title: String,
    val body: String,
    val waktu: String,
    val isRead: Boolean,
    val arah: NotifikasiArah = NotifikasiArah.TIDAK_DIKETAHUI,
    val grup: NotifikasiGrup = NotifikasiGrup.LEBIH_LAMA,

    /**
     * Nominal siap tampil, sudah diformat rupiah oleh ViewModel.
     *
     * `null` berarti tidak digambar sama sekali. Kontrak `GET /notifications`
     * tidak mengirim nominal, jadi nilainya selalu `null` di aplikasi — angka
     * rupiah tidak boleh diturunkan dari judul atau isi pesan.
     */
    val nominal: String? = null,

    /** Lihat [NotifikasiAksi]; `null` berarti kaki kartu hanya berisi chevron. */
    val aksi: NotifikasiAksi? = null,

    /**
     * Keterangan sumber di kaki kartu, misalnya penerbit pengumuman resmi.
     *
     * Sama seperti [aksi], belum ada field-nya di kontrak.
     */
    val sumber: String? = null,
)

data class NotifikasiUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val selectedTab: NotifikasiTab = NotifikasiTab.SEMUA,
    val items: List<NotifikasiItem> = emptyList(),
    val unreadCount: Int = 0,
    val hasMore: Boolean = false,

    /**
     * Jumlah notifikasi yang ditampilkan pada badge masing-masing tab.
     *
     * Nilai ini sebaiknya berasal dari ViewModel/API.
     */
    val tabCounts: Map<NotifikasiTab, Int> = emptyMap(),
) {
    val isTandaiSemuaAktif: Boolean
        get() = unreadCount > 0 && !isLoading

    fun countFor(tab: NotifikasiTab): Int {
        return tabCounts[tab] ?: 0
    }
}

// ── Main Screen ──────────────────────────────────────────────────────────

@Composable
fun NotifikasiScreen(
    state: NotifikasiUiState,
    onTabSelected: (NotifikasiTab) -> Unit,
    onItemClick: (NotifikasiItem) -> Unit,
    onTandaiSemuaClick: () -> Unit,
    onLoadMore: () -> Unit,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            NotifikasiTopBar(
                tandaiSemuaAktif = state.isTandaiSemuaAktif,
                onBackClick = onBackClick,
                onTandaiSemuaClick = onTandaiSemuaClick,
            )
        },
        containerColor = AppColor.NeutralCool50,
        modifier = modifier,
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {

            NotifikasiFilterChips(
                selected = state.selectedTab,
                counts = state.tabCounts,
                onSelected = onTabSelected,
            )

            when {
                state.isLoading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        CircularProgressIndicator(
                            color = AppColor.Primary700,
                        )
                    }
                }

                state.error != null -> {
                    NotifikasiPesan(
                        title = stringResource(
                            R.string.notifikasi_error_title,
                        ),
                        description = state.error,
                        actionLabel = stringResource(
                            R.string.mutasi_coba_lagi,
                        ),
                        onAction = onRetry,
                    )
                }

                state.items.isEmpty() -> {
                    NotifikasiPesan(
                        title = stringResource(
                            R.string.notifikasi_empty_title,
                        ),
                        description = stringResource(
                            R.string.notifikasi_empty_description,
                        ),
                        actionLabel = stringResource(
                            R.string.notifikasi_kembali_beranda,
                        ),
                        onAction = onBackClick,
                    )
                }

                else -> {
                    NotifikasiDaftar(
                        state = state,
                        onItemClick = onItemClick,
                        onLoadMore = onLoadMore,
                    )
                }
            }
        }
    }
}

// ── Top Bar ──────────────────────────────────────────────────────────────

/**
 * App bar biru pekat khusus layar ini.
 *
 * Tidak memakai `AppTopBar` bersama: desain menaruh judul rata kiri di samping
 * tombol kembali dan menggantikan ikon aksi dengan pil berlabel. Memaksakan dua
 * bentuk itu ke `AppTopBar` akan mengubah penampilan semua layar yang memakainya.
 */
@Composable
private fun NotifikasiTopBar(
    tandaiSemuaAktif: Boolean,
    onBackClick: () -> Unit,
    onTandaiSemuaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .background(AppColor.Primary700)
            .heightIn(min = AppSize.TopBarHeight)
            .padding(horizontal = Spacing.s4),
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier.weight(1f),
        ) {

            // Lingkaran yang digambar desain 40dp; area sentuhnya tetap 48dp.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(AppSize.MinTouchTarget)
                    .clickable(onClick = onBackClick),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(AppSize.TopBarButton)
                        .clip(AppShape.Full),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = stringResource(
                            R.string.notifikasi_kembali_beranda,
                        ),
                        tint = AppColor.Neutral100,
                        modifier = Modifier
                            .size(AppSize.Icon26)
                            .rotate(BACK_ICON_ROTATION),
                    )
                }
            }

            Text(
                text = stringResource(R.string.notifikasi_title),
                style = AppTextStyle.TopBarTitle,
                color = AppColor.Neutral100,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        NotifikasiTandaiDibacaPil(
            enabled = tandaiSemuaAktif,
            onClick = onTandaiSemuaClick,
        )
    }
}

@Composable
private fun NotifikasiTandaiDibacaPil(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (enabled) {
        AppColor.Neutral100
    } else {
        AppColor.Neutral100.copy(alpha = AppAlpha.A50)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = AppSize.MinTouchTarget)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingHalfStep.h1),
            modifier = Modifier
                .clip(AppShape.Full)
                .background(AppColor.Neutral100.copy(alpha = AppAlpha.A10))
                .border(
                    width = StrokeWidth.w0,
                    color = AppColor.Neutral100.copy(alpha = AppAlpha.A30),
                    shape = AppShape.Full,
                )
                .padding(
                    horizontal = Spacing.s3,
                    vertical = SpacingHalfStep.h1,
                ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_done_all),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(AppSize.IconSmall),
            )

            Text(
                text = stringResource(R.string.notifikasi_tandai_dibaca),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

// ── Filter Chips ─────────────────────────────────────────────────────────

@Composable
private fun NotifikasiFilterChips(
    selected: NotifikasiTab,
    counts: Map<NotifikasiTab, Int>,
    onSelected: (NotifikasiTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColor.NeutralCool50),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(
                    horizontal = Spacing.s4,
                    vertical = Spacing.s3,
                ),
        ) {
            NotifikasiTab.entries.forEach { tab ->
                NotifikasiChip(
                    tab = tab,
                    count = counts[tab] ?: 0,
                    selected = tab == selected,
                    onClick = { onSelected(tab) },
                )
            }
        }

        HorizontalDivider(
            thickness = StrokeWidth.w0,
            color = AppColor.NeutralCool200,
        )
    }
}

@Composable
private fun NotifikasiChip(
    tab: NotifikasiTab,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = AppSize.MinTouchTarget)
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingHalfStep.h1),
            modifier = Modifier
                .clip(AppShape.Full)
                .background(
                    if (selected) AppColor.Primary700 else AppColor.Neutral100,
                )
                .then(
                    if (selected) {
                        Modifier
                    } else {
                        Modifier.border(
                            width = StrokeWidth.w0,
                            color = AppColor.NeutralCool300,
                            shape = AppShape.Full,
                        )
                    },
                )
                .padding(
                    horizontal = Spacing.s4,
                    vertical = SpacingHalfStep.h2,
                ),
        ) {
            Text(
                text = stringResource(tab.labelRes()),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) AppColor.Neutral100 else AppColor.NeutralCool700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (count > 0) {
                NotifikasiChipBadge(
                    tab = tab,
                    count = count,
                    selected = selected,
                )
            }
        }
    }
}

/**
 * Badge angka di chip.
 *
 * Hanya tampil kalau angkanya lebih dari nol. `GET /notifications` tidak
 * mengirim jumlah per jenis, jadi di aplikasi angkanya belum pernah terisi —
 * menurunkannya dari halaman yang kebetulan termuat akan menampilkan angka yang
 * salah begitu paginasi berjalan.
 */
@Composable
private fun NotifikasiChipBadge(
    tab: NotifikasiTab,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = when {
        selected -> AppColor.Neutral100
        tab == NotifikasiTab.PROMO -> AppColor.Warning100
        else -> AppColor.NeutralCool200
    }

    val foreground = when {
        selected -> AppColor.Primary700
        tab == NotifikasiTab.PROMO -> AppColor.Warning700
        else -> AppColor.NeutralCool800
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = AppSize.BadgeMin)
            .widthIn(min = AppSize.BadgeMin)
            .clip(AppShape.Full)
            .background(background)
            .padding(horizontal = SpacingHalfStep.h1),
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
            maxLines = 1,
        )
    }
}

// ── Daftar ───────────────────────────────────────────────────────────────

@Composable
private fun NotifikasiDaftar(
    state: NotifikasiUiState,
    onItemClick: (NotifikasiItem) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Dihitung sekali per daftar, bukan di dalam tiap item: header hanya perlu
    // angkanya, dan menghitung ulang saat recomposition membuat biayanya kuadratik.
    val belumDibacaPerGrup = remember(state.items) {
        state.items.filterNot { it.isRead }.groupingBy { it.grup }.eachCount()
    }

    LazyColumn(
        contentPadding = PaddingValues(
            horizontal = Spacing.s4,
            vertical = Spacing.s4,
        ),
        modifier = modifier.fillMaxSize(),
    ) {
        state.items.forEachIndexed { index, item ->

            val grupBaru = index == 0 || state.items[index - 1].grup != item.grup

            if (grupBaru) {
                // Id ikut jadi kunci: kalau server pernah mengirim urutan yang
                // tidak monoton, satu grup bisa muncul dua kali dan kunci yang
                // hanya berisi nama grup akan bentrok.
                item(key = "grup_${item.grup.name}_${item.id}") {
                    NotifikasiGrupHeader(
                        grup = item.grup,
                        belumDibaca = belumDibacaPerGrup[item.grup] ?: 0,
                        modifier = Modifier.padding(
                            top = if (index == 0) Spacing.s0 else Spacing.s5,
                            bottom = SpacingHalfStep.h2,
                        ),
                    )
                }
            }

            item(key = item.id) {
                NotifikasiKartu(
                    item = item,
                    onClick = { onItemClick(item) },
                    modifier = Modifier.padding(bottom = Spacing.s3),
                )
            }
        }

        if (state.hasMore && !state.isLoadingMore) {
            item(key = "load_more") {

                LaunchedEffect(state.items.size) {
                    onLoadMore()
                }

                NotifikasiLoadMoreDots(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.s6),
                )
            }
        }

        if (state.isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.s6),
                ) {
                    CircularProgressIndicator(
                        color = AppColor.Primary700,
                        modifier = Modifier.size(AppSize.Icon24),
                    )
                }
            }
        }

        if (!state.hasMore && !state.isLoadingMore) {
            item(key = "akhir_daftar") {
                NotifikasiAkhirDaftar()
            }
        }
    }
}

@Composable
private fun NotifikasiGrupHeader(
    grup: NotifikasiGrup,
    belumDibaca: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.s1),
    ) {
        Text(
            text = stringResource(grup.labelRes()).uppercase(),
            style = AppTextStyle.GroupLabel,
            color = AppColor.NeutralCool500,
        )

        if (belumDibaca > 0) {
            Text(
                text = stringResource(
                    R.string.notifikasi_belum_dibaca_jumlah,
                    belumDibaca,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = AppColor.Primary700,
            )
        }
    }
}

// ── Kartu ────────────────────────────────────────────────────────────────

@Composable
private fun NotifikasiKartu(
    item: NotifikasiItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = Elevation.Card,
                shape = AppShape.R6,
            )
            .clip(AppShape.R6)
            .background(AppColor.Neutral100)
            .border(
                width = StrokeWidth.w0,
                color = AppColor.NeutralCool200,
                shape = AppShape.R6,
            )
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {

            // Garis aksen kiri hanya untuk yang belum dibaca; digambar di atas
            // garis tepi abu supaya sisi kirinya bersih seperti desain.
            if (!item.isRead) {
                Box(
                    modifier = Modifier
                        .width(StrokeWidth.w2)
                        .fillMaxHeight()
                        .background(AppColor.Primary700),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(SpacingHalfStep.h3),
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.s4),
            ) {

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(AppSize.AvatarMedium)
                        .clip(AppShape.Full)
                        .background(item.iconBackgroundColor()),
                ) {
                    Icon(
                        painter = painterResource(item.iconRes()),
                        contentDescription = null,
                        tint = item.iconTintColor(),
                        modifier = Modifier.size(AppSize.Icon24),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    NotifikasiKartuJudul(item = item)

                    if (item.nominal != null) {
                        Spacer(modifier = Modifier.height(Spacing.s1))

                        Text(
                            text = item.nominal,
                            style = AppTextStyle.CardAmount,
                            color = item.nominalColor(),
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(
                            if (item.nominal == null) Spacing.s1 else SpacingHalfStep.h1,
                        ),
                    )

                    // Tanpa batas baris: kartu di desain menampilkan isi pesan
                    // utuh, tidak seperti daftar rata sebelumnya yang memotong
                    // di dua baris. Judulnya tetap satu baris.
                    Text(
                        text = item.body,
                        style = AppTextStyle.CardBody,
                        color = AppColor.NeutralCool600,
                    )

                    Spacer(modifier = Modifier.height(Spacing.s3))

                    NotifikasiKartuKaki(item = item)
                }
            }
        }
    }
}

@Composable
private fun NotifikasiKartuJudul(
    item: NotifikasiItem,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = item.title,
            style = AppTextStyle.CardTitle,
            color = if (item.isRead) {
                AppColor.NeutralCool800
            } else {
                AppColor.NeutralCool900
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingHalfStep.h1),
        ) {
            if (!item.isRead) {
                // Titik ini satu-satunya penanda "belum dibaca" di baris judul,
                // jadi statusnya diumumkan; tanpa ini pembaca layar tidak punya
                // cara tahu bedanya.
                val penanda = stringResource(
                    R.string.notifikasi_penanda_belum_dibaca,
                )

                Box(
                    modifier = Modifier
                        .size(AppSize.UnreadDot)
                        .clip(AppShape.Full)
                        .background(AppColor.Primary700)
                        .semantics { contentDescription = penanda },
                )
            }

            Text(
                text = item.waktu,
                style = MaterialTheme.typography.labelMedium,
                color = AppColor.NeutralCool500,
                maxLines = 1,
            )
        }
    }
}

/**
 * Kaki kartu: pemisah, satu aksi pintas atau keterangan sumber, lalu chevron.
 *
 * Chevron selalu ada karena seluruh kartu memang bisa dibuka; aksi dan sumber
 * hanya tampil kalau datanya ada (lihat [NotifikasiAksi]).
 */
@Composable
private fun NotifikasiKartuKaki(
    item: NotifikasiItem,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {

        HorizontalDivider(
            thickness = StrokeWidth.w0,
            color = AppColor.NeutralCool100,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.s2),
        ) {
            when {
                item.aksi != null -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            painter = painterResource(item.aksi.iconRes()),
                            contentDescription = null,
                            tint = AppColor.Primary700,
                            modifier = Modifier.size(AppSize.Icon16),
                        )

                        Text(
                            text = stringResource(item.aksi.labelRes()),
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColor.Primary700,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                item.sumber != null -> {
                    Text(
                        text = item.sumber,
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColor.NeutralCool500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }

                else -> {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(
                    R.string.notifikasi_buka_detail,
                ),
                tint = AppColor.NeutralCool400,
                modifier = Modifier.size(AppSize.IconSmall),
            )
        }
    }
}

// ── Kaki daftar ──────────────────────────────────────────────────────────

@Composable
private fun NotifikasiAkhirDaftar(
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.s6),
    ) {
        Box(
            modifier = Modifier
                .width(AppSize.EndOfListBar)
                .height(StrokeWidth.w2)
                .clip(AppShape.Full)
                .background(AppColor.NeutralCool300),
        )

        Text(
            text = stringResource(R.string.notifikasi_akhir_daftar),
            style = MaterialTheme.typography.labelMedium,
            color = AppColor.NeutralCool400,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Load More ─────────────────────────────────────────────────────────────

@Composable
private fun NotifikasiLoadMoreDots(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(
        label = "notifikasi_load_more",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(
            Spacing.s2,
            Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        repeat(LOAD_MORE_DOT_COUNT) { index ->

            val dotAlpha by transition.animateFloat(
                initialValue = AppAlpha.A50,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = LOAD_MORE_PULSE_MILLIS,
                    ),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(
                        index * LOAD_MORE_PULSE_STAGGER_MILLIS,
                    ),
                ),
                label = "notifikasi_load_more_dot",
            )

            Box(
                modifier = Modifier
                    .size(AppSize.LoadMoreDot)
                    .alpha(dotAlpha)
                    .clip(AppShape.Full)
                    .background(AppColor.NeutralCool300),
            )
        }
    }
}

// ── Empty / Error ────────────────────────────────────────────────────────

@Composable
private fun NotifikasiPesan(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.s6),
    ) {

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = AppColor.NeutralCool900,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(Spacing.s2),
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColor.NeutralCool600,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(Spacing.s8),
        )

        Button(
            onClick = onAction,
            shape = AppShape.R6,
            contentPadding = PaddingValues(
                horizontal = Spacing.s6,
                vertical = Spacing.s3,
            ),
            modifier = Modifier.heightIn(
                min = AppSize.MinTouchTarget,
            ),
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

// ── Mapping ───────────────────────────────────────────────────────────────

private const val LOAD_MORE_DOT_COUNT = 3
private const val LOAD_MORE_PULSE_MILLIS = 600
private const val LOAD_MORE_PULSE_STAGGER_MILLIS = 150

/** `ic_arrow_forward` diputar; project belum punya panah kiri tersendiri. */
private const val BACK_ICON_ROTATION = 180f

private fun NotifikasiTab.labelRes(): Int {
    return when (this) {
        NotifikasiTab.SEMUA ->
            R.string.notifikasi_tab_semua

        NotifikasiTab.TRANSAKSI ->
            R.string.notifikasi_tab_transaksi

        NotifikasiTab.PROMO ->
            R.string.notifikasi_tab_promo_hadiah

        NotifikasiTab.INFO ->
            R.string.notifikasi_tab_info_bank
    }
}

private fun NotifikasiGrup.labelRes(): Int {
    return when (this) {
        NotifikasiGrup.HARI_INI ->
            R.string.notifikasi_grup_hari_ini

        NotifikasiGrup.KEMARIN ->
            R.string.notifikasi_grup_kemarin

        NotifikasiGrup.MINGGU_INI ->
            R.string.notifikasi_grup_minggu_ini

        NotifikasiGrup.LEBIH_LAMA ->
            R.string.notifikasi_grup_lebih_lama
    }
}

@DrawableRes
private fun NotifikasiAksi.iconRes(): Int {
    return when (this) {
        NotifikasiAksi.BUKTI_TRANSFER ->
            R.drawable.ic_receipt

        NotifikasiAksi.QRIS ->
            R.drawable.ic_qr_scan

        NotifikasiAksi.SALIN_REFERENSI ->
            R.drawable.ic_content_copy
    }
}

private fun NotifikasiAksi.labelRes(): Int {
    return when (this) {
        NotifikasiAksi.BUKTI_TRANSFER ->
            R.string.notifikasi_aksi_bukti_transfer

        NotifikasiAksi.QRIS ->
            R.string.notifikasi_aksi_qris

        NotifikasiAksi.SALIN_REFERENSI ->
            R.string.notifikasi_aksi_salin_referensi
    }
}

@DrawableRes
private fun NotifikasiItem.iconRes(): Int {
    return when (jenis) {

        NotifikasiJenis.TRANSAKSI -> {
            when (arah) {
                NotifikasiArah.MASUK ->
                    R.drawable.ic_arrow_downward

                NotifikasiArah.KELUAR ->
                    R.drawable.ic_arrow_upward

                NotifikasiArah.TIDAK_DIKETAHUI ->
                    R.drawable.ic_receipt
            }
        }

        NotifikasiJenis.PROMO ->
            R.drawable.ic_redeem

        NotifikasiJenis.KEAMANAN ->
            R.drawable.ic_shield

        NotifikasiJenis.INFO ->
            R.drawable.ic_info
    }
}

private fun NotifikasiItem.iconBackgroundColor(): Color {
    return when (jenis) {
        NotifikasiJenis.TRANSAKSI -> when (arah) {
            NotifikasiArah.MASUK -> IconTint.Success
            NotifikasiArah.KELUAR -> IconTint.Info
            NotifikasiArah.TIDAK_DIKETAHUI -> IconTint.Info
        }

        NotifikasiJenis.PROMO -> IconTint.Warning
        NotifikasiJenis.KEAMANAN -> IconTint.Info
        NotifikasiJenis.INFO -> IconTint.Neutral
    }
}

private fun NotifikasiItem.iconTintColor(): Color {
    return when (jenis) {
        NotifikasiJenis.TRANSAKSI -> when (arah) {
            NotifikasiArah.MASUK -> AppColor.Success700
            NotifikasiArah.KELUAR -> AppColor.Danger600
            NotifikasiArah.TIDAK_DIKETAHUI -> AppColor.Primary700
        }

        NotifikasiJenis.PROMO -> AppColor.Warning600
        NotifikasiJenis.KEAMANAN -> AppColor.Primary700
        NotifikasiJenis.INFO -> AppColor.NeutralCool600
    }
}

/** Arah dana menentukan warna nominal; tanpa arah, nominal tetap netral. */
private fun NotifikasiItem.nominalColor(): Color {
    return when (arah) {
        NotifikasiArah.MASUK -> AppColor.Success700
        NotifikasiArah.KELUAR -> AppColor.Danger600
        NotifikasiArah.TIDAK_DIKETAHUI -> AppColor.NeutralCool800
    }
}

// ── Preview ──────────────────────────────────────────────────────────────

/**
 * Contoh isi untuk pratinjau.
 *
 * Sengaja mengisi [NotifikasiItem.nominal], [NotifikasiItem.aksi], dan
 * [NotifikasiItem.sumber] — tiga hal yang belum dikirim backend — supaya
 * desainnya tetap bisa dinilai utuh. Di aplikasi ketiganya masih `null`.
 */
private val PREVIEW_ITEMS = listOf(
    NotifikasiItem(
        id = "1",
        jenis = NotifikasiJenis.TRANSAKSI,
        title = "Transfer Masuk",
        body = "Dana masuk dari BUDI SANTOSO (BCA 0129384729) telah berhasil diterima di rekening Anda.",
        waktu = "Baru saja",
        isRead = false,
        arah = NotifikasiArah.MASUK,
        grup = NotifikasiGrup.HARI_INI,
        nominal = "+ Rp 500.000",
        aksi = NotifikasiAksi.BUKTI_TRANSFER,
    ),
    NotifikasiItem(
        id = "2",
        jenis = NotifikasiJenis.PROMO,
        title = "Cashback 50% di Kopi Kenangan",
        body = "Nikmati promo istimewa cashback hingga Rp 25.000 dengan pembayaran QRIS di myBCA / BCA Mobile.",
        waktu = "10:30",
        isRead = false,
        grup = NotifikasiGrup.HARI_INI,
        aksi = NotifikasiAksi.QRIS,
    ),
    NotifikasiItem(
        id = "3",
        jenis = NotifikasiJenis.TRANSAKSI,
        title = "Pembayaran Tagihan PLN Berhasil",
        body = "Pembayaran PLN Pascabayar ID Pelanggan 53819028472 telah sukses diproses.",
        waktu = "13 Agu, 19:42",
        isRead = true,
        arah = NotifikasiArah.KELUAR,
        grup = NotifikasiGrup.KEMARIN,
        nominal = "- Rp 350.000",
        aksi = NotifikasiAksi.SALIN_REFERENSI,
    ),
    NotifikasiItem(
        id = "4",
        jenis = NotifikasiJenis.INFO,
        title = "Pemeliharaan Sistem Rutin",
        body = "Akan ada pemeliharaan sistem terencana pada 14 Agustus 2024 pukul 00:00 - 04:00 WIB.",
        waktu = "12 Agu",
        isRead = true,
        grup = NotifikasiGrup.MINGGU_INI,
        sumber = "Info Resmi PT Bank Central Asia Tbk",
    ),
)

private val PREVIEW_STATE = NotifikasiUiState(
    items = PREVIEW_ITEMS,
    unreadCount = 2,
    tabCounts = mapOf(
        NotifikasiTab.SEMUA to 3,
        NotifikasiTab.TRANSAKSI to 2,
        NotifikasiTab.PROMO to 1,
    ),
)

@Preview(
    name = "Terang",
    widthDp = 360,
    heightDp = 800,
)
@Composable
private fun NotifikasiScreenPreview() {
    BcaMobileTheme {
        NotifikasiScreen(
            state = PREVIEW_STATE,
            onTabSelected = {},
            onItemClick = {},
            onTandaiSemuaClick = {},
            onLoadMore = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}

@Preview(
    name = "Gelap",
    widthDp = 360,
    heightDp = 800,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun NotifikasiScreenDarkPreview() {
    BcaMobileTheme {
        NotifikasiScreen(
            state = PREVIEW_STATE,
            onTabSelected = {},
            onItemClick = {},
            onTandaiSemuaClick = {},
            onLoadMore = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}

@Preview(
    name = "Kosong",
    widthDp = 360,
    heightDp = 800,
)
@Composable
private fun NotifikasiScreenEmptyPreview() {
    BcaMobileTheme {
        NotifikasiScreen(
            state = NotifikasiUiState(),
            onTabSelected = {},
            onItemClick = {},
            onTandaiSemuaClick = {},
            onLoadMore = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}

@Preview(
    name = "Memuat",
    widthDp = 360,
    heightDp = 800,
)
@Composable
private fun NotifikasiScreenLoadingPreview() {
    BcaMobileTheme {
        NotifikasiScreen(
            state = NotifikasiUiState(isLoading = true),
            onTabSelected = {},
            onItemClick = {},
            onTandaiSemuaClick = {},
            onLoadMore = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}
