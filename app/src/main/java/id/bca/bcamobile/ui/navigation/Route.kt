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
@Serializable data object Notifikasi

/** Bukti transaksi yang ditarik ulang lewat `GET /transactions/{id}/receipt`. */
@Serializable data class BuktiTransaksi(val transactionId: String)

@Serializable data object Transfer
@Serializable data object TransferAntarRekening
@Serializable data object TransferPin
@Serializable data object TransferBukti

// ── EWallet ─────────────────────────────────────────────────────────────

@Serializable data object UbahKodeAkses
@Serializable data object RekeningKartu
@Serializable data object AturLimit

/** Dialog PIN di atas Atur Limit; `PUT transaction-limit` menolak tanpa verifikasi PIN. */
@Serializable data object AturLimitPin

/**
 * Dialog pemilih alasan blokir / alasan + metode kirim penggantian kartu.
 * Kedua endpoint mewajibkannya, tapi pemilihnya tidak ada di artefak desain.
 */
@Serializable data object KartuAksi

/**
 * Dialog PIN di atas Profil Saya untuk blokir dan ganti kartu.
 * Terpisah dari [AturLimitPin] karena `purpose` token-nya berbeda.
 */
@Serializable data object KartuPin

/** Pusat Bantuan (FAQ); `GET /content/help-center`, tanpa Authorization. */
@Serializable data object PusatBantuan

/** Kontak Halo BCA; `GET /content/contact-cs`, tanpa Authorization. */
@Serializable data object HubungiCs

@Serializable data object QrisScan
@Serializable data object QrisKonfirmasi
@Serializable data object QrisPin
@Serializable data object QrisBukti

@Serializable data object EWalletPilih
@Serializable data object EWalletNominal
@Serializable data object EWalletPin
@Serializable data object EWalletBukti