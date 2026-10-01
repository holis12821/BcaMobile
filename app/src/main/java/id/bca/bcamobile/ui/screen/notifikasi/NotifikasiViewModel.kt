package id.bca.bcamobile.ui.screen.notifikasi

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.bca.bcamobile.R
import id.bca.bcamobile.core.network.messageOrNull
import id.bca.bcamobile.core.push.PushEventBus
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.notification.NotificationRepository
import id.bca.bcamobile.domain.notification.model.AppNotification
import id.bca.bcamobile.domain.notification.model.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * Notifikasi nasabah dari `GET /notifications`.
 *
 * Halaman berikutnya selalu lewat `pagination.cursor`; tidak ada nomor halaman.
 *
 * **Tab menyaring di server** lewat parameter `type`. Karena itu setiap tab
 * punya paginasinya sendiri: cursor lama milik filter lama, jadi berpindah tab
 * memuat ulang dari halaman pertama. Sebelumnya penyaringan dilakukan atas apa
 * yang kebetulan sudah termuat, sehingga tab yang sempit bisa terlihat kosong
 * padahal server masih punya halaman berikutnya.
 *
 * Tab "Semua" **tidak** mengirim `type` sama sekali — `ALL` bukan nilai yang
 * dikenal server dan dijawab `VALIDATION_ERROR`.
 */
@HiltViewModel
class NotifikasiViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val pushEventBus: PushEventBus,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotifikasiUiState())
    val uiState: StateFlow<NotifikasiUiState> = _uiState.asStateFlow()

    private var nextCursor: String? = null
    private var isLoadingPage = false
    private val loaded = mutableListOf<AppNotification>()

    init {
        load()

        // Push yang masuk saat layar ini terbuka hanya berarti "ada yang baru".
        // Muatannya tidak punya `id` maupun status baca, jadi ia tidak boleh
        // disisipkan ke daftar — daftarnya ditarik ulang dari server.
        viewModelScope.launch {
            pushEventBus.arrivals.collect { load() }
        }
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        nextCursor = null
        viewModelScope.launch { fetch(reset = true) }
    }

    /**
     * Ganti tab berarti ganti filter server, jadi daftar dan cursor dimulai
     * ulang. Mempertahankan cursor lama akan meminta "halaman berikutnya" dari
     * kueri yang sudah tidak berlaku.
     */
    fun onTabSelected(tab: NotifikasiTab) {
        if (tab == _uiState.value.selectedTab) return
        _uiState.update { it.copy(selectedTab = tab, isLoading = true, error = null) }
        nextCursor = null
        viewModelScope.launch { fetch(reset = true) }
    }

    fun onLoadMore() {
        if (isLoadingPage || nextCursor == null) return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch { fetch(reset = false) }
    }

    /**
     * Menandai satu notifikasi terbaca.
     *
     * Ditulis ke state lebih dulu supaya barisnya langsung berubah, lalu dikirim ke
     * server. Kalau `PUT /notifications/{id}/read` gagal, keadaan lama dikembalikan —
     * lencana "belum dibaca" di Beranda memakai `unread_count` dari server, jadi
     * client tidak boleh mengaku sudah membaca sesuatu yang tidak tercatat di sana.
     */
    fun onItemOpened(item: NotifikasiItem) {
        val index = loaded.indexOfFirst { it.id == item.id }
        if (index == -1 || loaded[index].isRead) return

        val before = loaded[index]
        loaded[index] = before.copy(isRead = true)
        _uiState.update {
            it.copy(
                items = loaded.toItems(),
                unreadCount = (it.unreadCount - 1).coerceAtLeast(0),
            )
        }

        viewModelScope.launch {
            if (repository.markRead(item.id) is DataResult.Failure) {
                loaded[index] = before
                _uiState.update {
                    it.copy(
                        items = loaded.toItems(),
                        unreadCount = it.unreadCount + 1,
                    )
                }
            }
        }
    }

    /** `PUT /notifications/read-all`; daftar dimuat ulang supaya hitungannya dari server. */
    fun onTandaiSemuaClick() {
        if (!_uiState.value.isTandaiSemuaAktif) return
        viewModelScope.launch {
            when (val result = repository.markAllRead()) {
                is DataResult.Success -> load()
                is DataResult.Failure -> _uiState.update {
                    it.copy(error = result.error.messageOrNull() ?: generalError())
                }
            }
        }
    }

    private suspend fun fetch(reset: Boolean) {
        isLoadingPage = true
        val result = repository.notifications(
            types = _uiState.value.selectedTab.wireTypes(),
            cursor = if (reset) null else nextCursor,
        )
        isLoadingPage = false

        when (result) {
            is DataResult.Success -> {
                if (reset) loaded.clear()
                loaded += result.value.items
                nextCursor = result.value.nextCursor
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = null,
                        items = loaded.toItems(),
                        // `GET /notifications` tidak mengirim `unread_count`
                        // (lihat NotificationsResponse) — nilainya selalu 0 dan
                        // tombol "Tandai Semua" karenanya tidak pernah menyala.
                        // Yang dihitung di sini adalah yang belum dibaca **di
                        // antara yang sudah termuat**; itu cukup untuk
                        // menentukan tombolnya berguna atau tidak. Lencana di
                        // Beranda tetap memakai `unread_notifications` dari
                        // `GET /account/dashboard`, jadi tidak ada dua sumber
                        // untuk angka yang sama.
                        unreadCount = loaded.count { notification -> !notification.isRead },
                        hasMore = result.value.nextCursor != null,
                    )
                }
            }

            is DataResult.Failure -> _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = result.error.messageOrNull() ?: generalError(),
                )
            }
        }
    }

    private fun generalError(): String = context.getString(R.string.error_general_retry)

    // -- Pemetaan domain → tampilan --------------------------------------------

    /**
     * Tidak ada penyaringan di sini lagi: server sudah mengirim tepat jenis
     * yang diminta tab. Menyaring dua kali hanya menyembunyikan selisih antara
     * pemetaan client dan daftar tertutup server.
     */
    private fun List<AppNotification>.toItems(): List<NotifikasiItem> =
        map { it.toNotifikasiItem() }

    private fun AppNotification.toNotifikasiItem(): NotifikasiItem = NotifikasiItem(
        id = id,
        jenis = type.toJenis(),
        title = title,
        body = body,
        waktu = createdAt.toRelativeLabel(),
        isRead = isRead,
        // `GET /notifications` hanya membalas `type`, tidak ada arah dana. Menebaknya
        // dari judul berarti memasang panah yang bisa salah arah — dibiarkan netral.
        arah = NotifikasiArah.TIDAK_DIKETAHUI,
        grup = createdAt.toGrup(),
        // Nominal, aksi pintas, dan keterangan sumber tidak ada di kontrak
        // `GET /notifications`. Dibiarkan null: angka rupiah dan tujuan aksi
        // tidak boleh diturunkan dari judul atau isi pesan.
    )

    /**
     * `created_at` adalah UTC; labelnya dihitung di zona waktu perangkat, kalau
     * tidak notifikasi jelang tengah malam bisa disebut "Kemarin" padahal hari ini.
     */
    private fun String.toRelativeLabel(): String {
        val instant = runCatching { Instant.parse(this) }.getOrNull() ?: return this
        val zone = ZoneId.systemDefault()
        val waktu = instant.atZone(zone)
        val hariIni = LocalDate.now(zone)

        return when {
            Duration.between(instant, Instant.now()) < BARU_SAJA ->
                context.getString(R.string.notifikasi_waktu_baru_saja)

            waktu.toLocalDate() == hariIni -> JAM.format(waktu)

            waktu.toLocalDate() == hariIni.minusDays(1) ->
                context.getString(R.string.notifikasi_waktu_kemarin)

            else -> TANGGAL.format(waktu)
        }
    }

    /**
     * Kelompok tanggal untuk header daftar.
     *
     * Dihitung di zona waktu perangkat dengan alasan yang sama seperti
     * [toRelativeLabel]: `created_at` adalah UTC, dan notifikasi jelang tengah
     * malam tidak boleh masuk kelompok yang salah.
     *
     * "Minggu ini" berarti tujuh hari terakhir, bukan minggu kalender —
     * pengelompokan yang berubah artinya setiap hari Senin lebih membingungkan
     * daripada membantu. Tanggal yang tidak bisa dibaca jatuh ke [LEBIH_LAMA]
     * supaya tidak pernah mengaku baru.
     */
    private fun String.toGrup(): NotifikasiGrup {
        val instant = runCatching { Instant.parse(this) }.getOrNull()
            ?: return NotifikasiGrup.LEBIH_LAMA

        val zone = ZoneId.systemDefault()
        val tanggal = instant.atZone(zone).toLocalDate()
        val hariIni = LocalDate.now(zone)

        return when {
            tanggal == hariIni -> NotifikasiGrup.HARI_INI
            tanggal == hariIni.minusDays(1) -> NotifikasiGrup.KEMARIN
            tanggal.isAfter(hariIni.minusDays(HARI_SEMINGGU)) -> NotifikasiGrup.MINGGU_INI
            else -> NotifikasiGrup.LEBIH_LAMA
        }
    }
}

/**
 * Jenis yang diminta per tab.
 *
 * Kosong berarti tanpa parameter `type` — itu arti tab "Semua".
 *
 * Desain hanya punya empat tab, sedangkan server mengenal lima jenis. Keamanan
 * **dan** sistem ikut tab Info supaya tidak ada notifikasi yang tak punya tab
 * dan menghilang dari layar.
 */
private fun NotifikasiTab.wireTypes(): Set<NotificationType> = when (this) {
    NotifikasiTab.SEMUA -> emptySet()
    NotifikasiTab.TRANSAKSI -> setOf(NotificationType.TRANSACTION)
    NotifikasiTab.PROMO -> setOf(NotificationType.PROMO)
    NotifikasiTab.INFO -> setOf(
        NotificationType.INFO,
        NotificationType.SECURITY,
        NotificationType.SYSTEM,
    )
}

/** Jenis yang tidak dikenal ditampilkan sebagai info, bukan disembunyikan. */
private fun NotificationType.toJenis(): NotifikasiJenis = when (this) {
    NotificationType.TRANSACTION -> NotifikasiJenis.TRANSAKSI
    NotificationType.PROMO -> NotifikasiJenis.PROMO
    NotificationType.SECURITY -> NotifikasiJenis.KEAMANAN
    // SYSTEM memakai tampilan Info: desain tidak menggambar jenis tersendiri
    // untuknya, dan Info adalah kelompok yang paling netral.
    NotificationType.SYSTEM,
    NotificationType.INFO,
    NotificationType.UNKNOWN,
    -> NotifikasiJenis.INFO
}

private val BARU_SAJA: Duration = Duration.ofMinutes(5)
private const val HARI_SEMINGGU = 7L
private val INDONESIAN = Locale.forLanguageTag("id-ID")
private val JAM = DateTimeFormatter.ofPattern("HH:mm", INDONESIAN)
private val TANGGAL = DateTimeFormatter.ofPattern("d MMM", INDONESIAN)
