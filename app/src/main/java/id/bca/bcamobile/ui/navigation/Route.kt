package id.bca.bcamobile.ui.navigation

import kotlinx.serialization.Serializable

// ── Auth ────────────────────────────────────────────────────────────────

@Serializable data object Login
@Serializable data object KodeAkses
@Serializable data object FaceId
@Serializable data object TouchId
@Serializable data object BukaRekening
@Serializable data object BukaRekeningPilihKartu
@Serializable data object BukaRekeningSyaratKetentuan
@Serializable data object BukaRekeningPanduanFoto
@Serializable data object BukaRekeningKameraFoto
@Serializable data object BukaRekeningHasilFoto
@Serializable data object BukaRekeningDataPribadi
@Serializable data object BukaRekeningOtp
@Serializable data object BukaRekeningVerifikasiBiometrik
@Serializable data object BukaRekeningAntreanVideoCall
@Serializable data object BukaRekeningVideoCall
@Serializable data object BukaRekeningBuatKredensial
@Serializable data object BukaRekeningRingkasan
@Serializable data object BukaRekeningBerhasilDibuat
@Serializable data object GantiKodeAkses

// ── Main ────────────────────────────────────────────────────────────────

@Serializable data object Home
@Serializable data object Mutasi
@Serializable data object Riwayat
@Serializable data object Akun
@Serializable data object RentangWaktu

@Serializable data object Transfer
@Serializable data object TransferAntarRekening
@Serializable data object TransferPin
@Serializable data object TransferBukti

// ── EWallet ─────────────────────────────────────────────────────────────

@Serializable data object EWalletPilih
@Serializable data object EWalletNominal
@Serializable data object EWalletPin
@Serializable data object EWalletBukti