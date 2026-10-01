package id.bca.bcamobile.domain.card

import id.bca.bcamobile.domain.card.model.BlockedReason
import id.bca.bcamobile.domain.card.model.CardReplacement
import id.bca.bcamobile.domain.card.model.DeliveryMethod
import id.bca.bcamobile.domain.card.model.PaymentCard
import id.bca.bcamobile.domain.card.model.ReplacementReason
import id.bca.bcamobile.domain.common.DataResult

/**
 * Kartu milik nasabah. Kontrak dibaca dari `internal/domain/card/entity.go`
 * di repo backend, bukan dari contoh di `01-API-SPECIFICATION.md`.
 */
interface CardRepository {

    /**
     * `GET /account/cards`. Tanpa parameter — pemiliknya diambil dari access
     * token, jadi tidak ada cara meminta kartu orang lain.
     *
     * Nasabah tanpa kartu dijawab daftar kosong, **bukan** kegagalan: layar
     * membedakan "belum punya kartu" dari "ada yang rusak".
     */
    suspend fun cards(): DataResult<List<PaymentCard>>

    /**
     * `PUT /account/cards/{cardId}/settings`. Kirim **hanya** sakelar yang
     * diubah; keduanya null dijawab `400 VALIDATION_ERROR` oleh server.
     *
     * Bukan aksi keamanan — tidak butuh `verification_token`. Kartu hasilnya
     * dipakai untuk repaint, jangan memanggil ulang [cards].
     */
    suspend fun updateSettings(
        cardId: String,
        debitOnlineEnabled: Boolean? = null,
        internationalEnabled: Boolean? = null,
    ): DataResult<PaymentCard>

    /**
     * `POST /account/cards/{cardId}/block`.
     *
     * [verificationToken] dari `POST /auth/pin/verify` dengan purpose
     * `BLOCK_CARD`. Umurnya 120 detik dan sekali pakai — jangan di-cache.
     */
    suspend fun block(
        cardId: String,
        reason: BlockedReason,
        verificationToken: String,
    ): DataResult<PaymentCard>

    /**
     * `POST /account/cards/{cardId}/replacement`. **Berbiaya** — diperlakukan
     * seperti transaksi uang.
     *
     * [idempotencyKey] dikirim sebagai `X-Idempotency-Key` dan **wajib stabil**
     * melintasi retry maupun rotasi layar. Mengulang dengan kunci yang sama
     * dijawab body identik; kunci baru untuk niat yang sama dijawab
     * `409 CARD_REPLACEMENT_IN_PROGRESS` — itu penjaga kedua, bukan kegagalan.
     *
     * [verificationToken] memakai purpose `REPLACE_CARD`, bukan `BLOCK_CARD`.
     */
    suspend fun requestReplacement(
        cardId: String,
        reason: ReplacementReason,
        deliveryMethod: DeliveryMethod,
        verificationToken: String,
        idempotencyKey: String,
    ): DataResult<CardReplacement>
}
