package id.bca.bcamobile.ui.navigation

import kotlinx.serialization.Serializable

// ── Auth ────────────────────────────────────────────────────────────────

@Serializable data object Login
@Serializable data object KodeAkses
@Serializable data object FaceId
@Serializable data object TouchId
@Serializable data object BukaRekening
@Serializable data object GantiKodeAkses

// ── Main ────────────────────────────────────────────────────────────────

@Serializable data object Home
@Serializable data object Mutasi
@Serializable data object Riwayat
@Serializable data object Akun
@Serializable data object RentangWaktu

@Serializable data object Transfer
@Serializable data object TransferAntarRekening

// ── EWallet ─────────────────────────────────────────────────────────────

@Serializable data object EWalletPilih
@Serializable data object EWalletNominal
@Serializable data object EWalletPin
@Serializable data object EWalletBukti