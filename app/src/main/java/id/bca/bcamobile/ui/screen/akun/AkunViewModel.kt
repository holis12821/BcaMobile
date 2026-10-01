package id.bca.bcamobile.ui.screen.akun

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.bca.bcamobile.BuildConfig
import id.bca.bcamobile.R
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.core.network.messageOrNull
import id.bca.bcamobile.core.push.PushTokenRegistrar
import id.bca.bcamobile.domain.account.AccountRepository
import id.bca.bcamobile.domain.auth.AuthRepository
import id.bca.bcamobile.domain.auth.model.PinPurpose
import id.bca.bcamobile.domain.card.CardRepository
import id.bca.bcamobile.domain.card.model.BlockedReason
import id.bca.bcamobile.domain.card.model.CardErrorCode
import id.bca.bcamobile.domain.card.model.CardStatus
import id.bca.bcamobile.domain.card.model.CardStyle
import id.bca.bcamobile.domain.card.model.DeliveryMethod
import id.bca.bcamobile.domain.card.model.PaymentCard
import id.bca.bcamobile.domain.card.model.ReplacementReason
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface AkunEvent {
    data object LoggedOut : AkunEvent

    /** Server mewajibkan verifikasi PIN untuk blokir dan ganti kartu. */
    data object PinRequired : AkunEvent

    /** Aksi kartu selesai; dialog ditutup dan pesannya ditampilkan. */
    data class CardActionDone(val message: String) : AkunEvent
}

/** Aksi kartu yang sedang disiapkan; menentukan `purpose` token verifikasi. */
enum class AksiKartu {
    BLOKIR,
    GANTI,
}

@HiltViewModel
class AkunViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
    private val cardRepository: CardRepository,
    private val pushTokenRegistrar: PushTokenRegistrar,
    private val savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AkunUiState(appVersion = BuildConfig.VERSION_NAME))
    val uiState: StateFlow<AkunUiState> = _uiState.asStateFlow()

    private val _aksiKartuState = MutableStateFlow(AksiKartuUiState())
    val aksiKartuState: StateFlow<AksiKartuUiState> = _aksiKartuState.asStateFlow()

    private val _pinState = MutableStateFlow(KodeAksesUiState())
    val pinState: StateFlow<KodeAksesUiState> = _pinState.asStateFlow()

    private val _events = Channel<AkunEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** PIN hidup hanya di memori dan dibuang segera setelah dipakai. */
    private val pin = StringBuilder()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = accountRepository.profile()) {
                is DataResult.Success -> _uiState.update {
                    val profile = result.value
                    val utama = profile.accounts.firstOrNull { account -> account.isPrimary }
                        ?: profile.accounts.firstOrNull()
                    it.copy(
                        isLoading = false,
                        error = null,
                        userName = profile.displayName,
                        phoneNumber = profile.phoneNumber,
                        accountNumber = utama?.number?.groupDigits().orEmpty(),
                        email = profile.email,
                        // Absen berarti nasabah reguler: badge disembunyikan.
                        accountTier = profile.tier?.let(::tierLabel).orEmpty(),
                        isBiometricEnabled = profile.isBiometricEnabled,
                    )
                }

                is DataResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.messageOrNull())
                }
            }

            loadCards()
        }
    }

    /**
     * Daftar kartu dimuat terpisah dari profil: kegagalannya tidak boleh
     * mengosongkan seluruh layar Profil Saya, hanya bagian kartunya.
     */
    private suspend fun loadCards() {
        when (val result = cardRepository.cards()) {
            is DataResult.Success -> {
                // Desain hanya menggambar satu kartu. Yang dipilih kartu utama;
                // kalau tidak ada yang ditandai utama, yang pertama.
                val card = result.value.firstOrNull { it.isPrimary } ?: result.value.firstOrNull()
                _uiState.update {
                    it.copy(
                        card = card?.toItem(),
                        hasNoCards = result.value.isEmpty(),
                    )
                }
            }

            is DataResult.Failure -> _uiState.update {
                it.copy(
                    card = null,
                    hasNoCards = false,
                    cardMessage = result.error.messageOrNull() ?: generalError(),
                )
            }
        }
    }

    // ── Sakelar kanal kartu ────────────────────────────────────────────────

    /**
     * `PUT /account/cards/{id}/settings`.
     *
     * Hanya sakelar yang diubah yang dikirim — yang satunya dibiarkan null
     * supaya server tidak mengembalikannya ke nilai lama. Layar di-repaint dari
     * kartu yang dibalas, bukan dengan memuat ulang seluruh daftar.
     */
    fun onCardSettingToggle(kanal: KartuKanal, enabled: Boolean) {
        val card = _uiState.value.card ?: return
        if (!card.isActionable || _uiState.value.isCardBusy) return

        val before = card
        _uiState.update {
            it.copy(
                isCardBusy = true,
                cardMessage = null,
                card = when (kanal) {
                    KartuKanal.DEBIT_ONLINE -> card.copy(isDebitOnlineEnabled = enabled)
                    KartuKanal.LUAR_NEGERI -> card.copy(isLuarNegeriEnabled = enabled)
                },
            )
        }

        viewModelScope.launch {
            val result = cardRepository.updateSettings(
                cardId = card.cardId,
                debitOnlineEnabled = enabled.takeIf { kanal == KartuKanal.DEBIT_ONLINE },
                internationalEnabled = enabled.takeIf { kanal == KartuKanal.LUAR_NEGERI },
            )

            when (result) {
                is DataResult.Success -> _uiState.update {
                    it.copy(isCardBusy = false, card = result.value.toItem())
                }

                // Sakelar dikembalikan: yang tampil harus yang benar-benar
                // tersimpan di server, bukan yang sempat ditekan.
                is DataResult.Failure -> _uiState.update {
                    it.copy(
                        isCardBusy = false,
                        card = before,
                        cardMessage = result.error.toCardMessage(),
                    )
                }
            }
        }
    }

    // ── Blokir dan ganti kartu ─────────────────────────────────────────────

    fun onBlokirKartuClick() {
        val card = _uiState.value.card ?: return
        if (!card.isActionable) return
        _aksiKartuState.value = AksiKartuUiState(aksi = AksiKartu.BLOKIR)
    }

    fun onGantiKartuClick() {
        val card = _uiState.value.card ?: return
        if (card.status == KartuStatusUi.PENGGANTIAN) return
        _aksiKartuState.value = AksiKartuUiState(
            aksi = AksiKartu.GANTI,
            // Ketersediaan ambil di cabang hanya diketahui setelah server
            // menolaknya sekali; sebelum itu keduanya ditawarkan.
            isBranchPickupAvailable = isBranchPickupAvailable,
        )
    }

    fun onBlockReasonSelected(reason: BlockedReason) =
        _aksiKartuState.update { it.copy(blockReason = reason) }

    fun onReplacementReasonSelected(reason: ReplacementReason) =
        _aksiKartuState.update { it.copy(replacementReason = reason) }

    fun onDeliveryMethodSelected(method: DeliveryMethod) =
        _aksiKartuState.update { it.copy(deliveryMethod = method) }

    fun onAksiKartuDismiss() {
        _aksiKartuState.value = AksiKartuUiState()
        clearPin()
    }

    /** Pilihan lengkap → minta PIN. Tokennya baru diambil setelah PIN masuk. */
    fun onAksiKartuConfirm() {
        if (!_aksiKartuState.value.isConfirmEnabled) return
        viewModelScope.launch { _events.send(AkunEvent.PinRequired) }
    }

    // ── Verifikasi PIN ─────────────────────────────────────────────────────

    fun onPinDigit(digit: Int) {
        if (pin.length >= PIN_LENGTH || _pinState.value.isSubmitting) return
        pin.append(digit)
        _pinState.update { it.copy(enteredDigits = pin.length, isError = false, errorMessage = null) }
        if (pin.length == PIN_LENGTH) submitPin()
    }

    fun onPinDelete() {
        if (pin.isEmpty() || _pinState.value.isSubmitting) return
        pin.deleteCharAt(pin.lastIndex)
        _pinState.update { it.copy(enteredDigits = pin.length, isError = false) }
    }

    /**
     * Verifikasi PIN lalu jalankan aksinya.
     *
     * Token verifikasi **tidak pernah disimpan**: umurnya 120 detik dan sekali
     * pakai, jadi ia diambil dan dipakai dalam satu tarikan napas. Nasabah yang
     * termenung di dialog konfirmasi akan diminta PIN lagi, bukan memakai token
     * basi yang dijawab `AUTH_TOKEN_INVALID`.
     */
    fun submitPin() {
        val aksi = _aksiKartuState.value.aksi ?: return
        val card = _uiState.value.card ?: return
        if (pin.length < PIN_LENGTH || _pinState.value.isSubmitting) return

        val code = pin.toString()
        _pinState.update { it.copy(isSubmitting = true, isError = false, errorMessage = null) }

        viewModelScope.launch {
            val purpose = when (aksi) {
                AksiKartu.BLOKIR -> PinPurpose.BLOCK_CARD
                AksiKartu.GANTI -> PinPurpose.REPLACE_CARD
            }
            val verification = authRepository.verifyPin(code, purpose)
            clearPin()

            if (verification !is DataResult.Success) {
                val failure = verification as DataResult.Failure
                _pinState.value = KodeAksesUiState(
                    isError = true,
                    errorMessage = failure.error.messageOrNull() ?: generalError(),
                )
                return@launch
            }

            _uiState.update { it.copy(isCardBusy = true, cardMessage = null) }
            when (aksi) {
                AksiKartu.BLOKIR -> runBlock(card.cardId, verification.value.verificationToken)
                AksiKartu.GANTI -> runReplacement(card.cardId, verification.value.verificationToken)
            }
        }
    }

    private suspend fun runBlock(cardId: String, verificationToken: String) {
        val reason = _aksiKartuState.value.blockReason ?: return
        val result = cardRepository.block(cardId, reason, verificationToken)
        _pinState.value = KodeAksesUiState()

        when (result) {
            is DataResult.Success -> {
                _uiState.update { it.copy(isCardBusy = false, card = result.value.toItem()) }
                finishCardAction(context.getString(R.string.profil_kartu_blokir_berhasil))
            }

            is DataResult.Failure -> failCardAction(result.error)
        }
    }

    /**
     * Penggantian kartu **berbiaya**, jadi kuncinya harus stabil.
     *
     * Kunci disimpan di [SavedStateHandle] dan bukan di variabel ViewModel:
     * ViewModel memang bertahan melewati rotasi, tetapi tidak melewati proses
     * yang dimatikan sistem lalu dipulihkan — dan di situlah kunci baru untuk
     * niat yang sama akan lahir, lalu server menjawab
     * `409 CARD_REPLACEMENT_IN_PROGRESS` atas permintaan yang sudah berhasil.
     */
    private suspend fun runReplacement(cardId: String, verificationToken: String) {
        val state = _aksiKartuState.value
        val reason = state.replacementReason ?: return
        val delivery = state.deliveryMethod ?: return

        val scope = "$REPLACEMENT_KEY_PREFIX$cardId"
        val idempotencyKey = savedStateHandle.get<String>(scope)
            ?: UUID.randomUUID().toString().also { savedStateHandle[scope] = it }

        val result = cardRepository.requestReplacement(
            cardId = cardId,
            reason = reason,
            deliveryMethod = delivery,
            verificationToken = verificationToken,
            idempotencyKey = idempotencyKey,
        )
        _pinState.value = KodeAksesUiState()

        when (result) {
            is DataResult.Success -> {
                // Niat ini selesai; kunci dilepas supaya permintaan berikutnya
                // punya kuncinya sendiri.
                savedStateHandle.remove<String>(scope)
                _uiState.update { it.copy(isCardBusy = false) }
                finishCardAction(
                    context.getString(
                        R.string.profil_kartu_ganti_berhasil,
                        result.value.estimatedArrivalFrom,
                        result.value.estimatedArrivalTo,
                    ),
                )
                loadCards()
            }

            is DataResult.Failure -> {
                val error = result.error
                if (error is ApiFailure.Business &&
                    error.code == CardErrorCode.DELIVERY_UNAVAILABLE
                ) {
                    // Ambil di cabang tidak tersedia untuk kartu ini; pilihannya
                    // disembunyikan dan nasabah memilih ulang, bukan mengulang
                    // permintaan yang sama.
                    isBranchPickupAvailable = false
                    _uiState.update { it.copy(isCardBusy = false) }
                    _aksiKartuState.update {
                        it.copy(
                            isBranchPickupAvailable = false,
                            deliveryMethod = DeliveryMethod.COURIER,
                            message = error.message.ifBlank {
                                context.getString(R.string.profil_kartu_cabang_tidak_tersedia)
                            },
                        )
                    }
                    return
                }

                if (error is ApiFailure.Business &&
                    error.code == CardErrorCode.REPLACEMENT_IN_PROGRESS
                ) {
                    // Permintaan sebelumnya **berhasil**. Ini penjaga, bukan
                    // kegagalan — nasabah diarahkan ke statusnya, dan kunci
                    // lama tidak dibuang supaya percobaan berikutnya tetap
                    // menunjuk niat yang sama.
                    _uiState.update { it.copy(isCardBusy = false) }
                    finishCardAction(
                        error.message.ifBlank {
                            context.getString(R.string.profil_kartu_ganti_sedang_diproses)
                        },
                    )
                    loadCards()
                    return
                }

                failCardAction(error)
            }
        }
    }

    private suspend fun finishCardAction(message: String) {
        _aksiKartuState.value = AksiKartuUiState()
        _events.send(AkunEvent.CardActionDone(message))
    }

    private suspend fun failCardAction(error: ApiFailure) {
        _uiState.update { it.copy(isCardBusy = false) }
        _aksiKartuState.value = AksiKartuUiState()
        // Kartu bisa saja sudah berubah di server (mis. CARD_NOT_FOUND karena
        // daftarnya basi), jadi daftarnya ditarik ulang sebelum pesan tampil.
        if (error is ApiFailure.Business && error.code == CardErrorCode.NOT_FOUND) loadCards()
        _events.send(AkunEvent.CardActionDone(error.toCardMessage()))
    }

    private fun clearPin() {
        pin.setLength(0)
        _pinState.update { it.copy(enteredDigits = 0, isSubmitting = false) }
    }

    // ── Sakelar pengaturan akun ────────────────────────────────────────────

    /**
     * Sakelar notifikasi transaksi — `PUT /account/settings` melayani
     * `push_notification_enabled`. Nilainya tidak dibalas `profile`, jadi keadaan awalnya
     * mengikuti default hidup sampai nasabah mengubahnya.
     */
    fun onNotificationToggle(enabled: Boolean) {
        val previous = _uiState.value.isNotificationEnabled
        _uiState.update { it.copy(isNotificationEnabled = enabled) }
        viewModelScope.launch {
            val result = accountRepository.updateNotificationSetting(enabled)
            if (result is DataResult.Failure) {
                _uiState.update {
                    it.copy(isNotificationEnabled = previous, error = result.error.messageOrNull())
                }
            }
        }
    }

    /**
     * Sakelar e-Statement. `PUT /account/settings` melayani
     * `email_statement_enabled`; nilainya tidak dibalas `profile`, jadi keadaan
     * awalnya mengikuti default mati sampai nasabah mengubahnya.
     */
    fun onEmailStatementToggle(enabled: Boolean) {
        val previous = _uiState.value.isEmailStatementEnabled
        _uiState.update { it.copy(isEmailStatementEnabled = enabled) }
        viewModelScope.launch {
            val result = accountRepository.updateEmailStatementSetting(enabled)
            if (result is DataResult.Failure) {
                _uiState.update {
                    it.copy(
                        isEmailStatementEnabled = previous,
                        error = result.error.messageOrNull(),
                    )
                }
            }
        }
    }

    /** Saklar dipindah dulu supaya terasa responsif, lalu dikembalikan bila server menolak. */
    fun onBiometricToggle(enabled: Boolean) {
        val previous = _uiState.value.isBiometricEnabled
        _uiState.update { it.copy(isBiometricEnabled = enabled) }
        viewModelScope.launch {
            val result = accountRepository.updateBiometricSetting(enabled)
            if (result is DataResult.Failure) {
                _uiState.update {
                    it.copy(isBiometricEnabled = previous, error = result.error.messageOrNull())
                }
            }
        }
    }

    /** Token lokal dibersihkan repository, termasuk saat server tidak terjangkau. */
    fun onLogout() {
        viewModelScope.launch {
            // `POST /auth/logout` mencabut sesi, bukan baris `devices`:
            // `push_token` tetap terisi dan tidak ada endpoint untuk menghapusnya,
            // jadi server masih akan mengirim push ke ponsel yang sudah logout.
            // Mematikan token di sisi FCM menutup celah itu lewat jalur yang sudah
            // ada — push berikutnya dijawab UNREGISTERED dan backend membersihkan
            // barisnya sendiri.
            pushTokenRegistrar.onLogout()
            authRepository.logout()
            _events.send(AkunEvent.LoggedOut)
        }
    }

    // ── Pemetaan domain → tampilan ─────────────────────────────────────────

    /**
     * Ketersediaan ambil di cabang berlaku per kartu dan hanya diketahui dari
     * penolakan server, jadi disimpan di sini dan bukan di state layar.
     */
    private var isBranchPickupAvailable = true

    private fun generalError(): String = context.getString(R.string.error_general_retry)

    /** Tiap kode error kartu punya perilaku UI sendiri, bukan satu toast umum. */
    private fun ApiFailure.toCardMessage(): String = when {
        this is ApiFailure.Business && code == CardErrorCode.NOT_FOUND ->
            context.getString(R.string.profil_kartu_tidak_ditemukan)

        this is ApiFailure.Business && code == CardErrorCode.BLOCKED ->
            context.getString(R.string.profil_kartu_sudah_diblokir)

        this is ApiFailure.Business && code == CardErrorCode.REPLACEMENT_IN_PROGRESS ->
            context.getString(R.string.profil_kartu_ganti_sedang_diproses)

        this is ApiFailure.Business && code == CardErrorCode.TOKEN_INVALID ->
            context.getString(R.string.profil_kartu_verifikasi_kedaluwarsa)

        else -> messageOrNull() ?: generalError()
    }

    private fun tierLabel(tier: String): String = when (tier.uppercase()) {
        TIER_PRIORITAS -> context.getString(R.string.akun_tier_prioritas)
        TIER_SOLITAIRE -> context.getString(R.string.akun_tier_solitaire)
        // Tier baru yang belum dikenal build ini tetap dicetak apa adanya —
        // menyembunyikannya berarti menghilangkan badge yang memang berlaku.
        else -> tier
    }

    private fun PaymentCard.toItem(): KartuItem = KartuItem(
        cardId = cardId,
        maskedNumber = maskedNumber,
        holderName = cardholderName.ifBlank { _uiState.value.userName },
        productName = productName,
        validThru = validThru,
        status = status.toUi(),
        style = style.toUi(),
        isDebitOnlineEnabled = settings.debitOnlineEnabled,
        isLuarNegeriEnabled = settings.internationalEnabled,
        // Server hanya mengirim alasan saat kartu benar-benar diblokir, jadi
        // null di sini berarti tidak ada keterangan untuk ditampilkan.
        blockedReasonLabel = blockedReason?.let { context.getString(it.labelRes()) },
    )

    private companion object {
        const val PIN_LENGTH = 6
        const val REPLACEMENT_KEY_PREFIX = "card_replacement_key:"
        const val TIER_PRIORITAS = "PRIORITAS"
        const val TIER_SOLITAIRE = "SOLITAIRE"
    }
}

/**
 * State dialog aksi kartu (blokir / ganti).
 *
 * Dipisah dari [AkunUiState] supaya layar Profil Saya tidak ikut recompose
 * setiap kali pilihan di dialog berubah.
 */
data class AksiKartuUiState(
    val aksi: AksiKartu? = null,
    val blockReason: BlockedReason? = null,
    val replacementReason: ReplacementReason? = null,
    val deliveryMethod: DeliveryMethod? = null,
    /** False setelah server menjawab `CARD_DELIVERY_UNAVAILABLE` untuk kartu ini. */
    val isBranchPickupAvailable: Boolean = true,
    val message: String? = null,
) {
    val isConfirmEnabled: Boolean
        get() = when (aksi) {
            AksiKartu.BLOKIR -> blockReason != null
            AksiKartu.GANTI -> replacementReason != null && deliveryMethod != null
            null -> false
        }
}

private const val GROUP_SIZE = 4

/** `1234567890` menjadi `1234 5678 90`, seperti di desain. */
private fun String.groupDigits(): String = chunked(GROUP_SIZE).joinToString(" ")

private fun CardStatus.toUi(): KartuStatusUi = when (this) {
    CardStatus.ACTIVE -> KartuStatusUi.AKTIF
    CardStatus.BLOCKED -> KartuStatusUi.DIBLOKIR
    CardStatus.EXPIRED -> KartuStatusUi.KEDALUWARSA
    CardStatus.REPLACEMENT_PENDING -> KartuStatusUi.PENGGANTIAN
    CardStatus.UNKNOWN -> KartuStatusUi.TIDAK_DIKENAL
}

private fun CardStyle.toUi(): KartuStyleUi = when (this) {
    CardStyle.BLUE -> KartuStyleUi.BLUE
    CardStyle.GOLD -> KartuStyleUi.GOLD
    CardStyle.PLATINUM -> KartuStyleUi.PLATINUM
}

internal fun BlockedReason.labelRes(): Int = when (this) {
    BlockedReason.LOST -> R.string.profil_kartu_alasan_hilang
    BlockedReason.STOLEN -> R.string.profil_kartu_alasan_dicuri
    BlockedReason.DAMAGED -> R.string.profil_kartu_alasan_rusak
    BlockedReason.SUSPECTED_FRAUD -> R.string.profil_kartu_alasan_penipuan
}

internal fun ReplacementReason.labelRes(): Int = when (this) {
    ReplacementReason.DAMAGED -> R.string.profil_kartu_alasan_rusak
    ReplacementReason.LOST -> R.string.profil_kartu_alasan_hilang
    ReplacementReason.UPGRADE -> R.string.profil_kartu_alasan_upgrade
}

internal fun DeliveryMethod.labelRes(): Int = when (this) {
    DeliveryMethod.COURIER -> R.string.profil_kartu_kirim_kurir
    DeliveryMethod.BRANCH_PICKUP -> R.string.profil_kartu_kirim_cabang
}
