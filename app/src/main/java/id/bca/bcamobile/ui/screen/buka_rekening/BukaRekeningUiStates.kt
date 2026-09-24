package id.bca.bcamobile.ui.screen.buka_rekening

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.bca.bcamobile.R
import id.bca.bcamobile.core.liveness.LivenessDetector
import id.bca.bcamobile.core.network.ErrorText
import java.util.Locale

/**
 * Menurunkan `UiState` tiap layar dari satu [BukaRekeningFlowState].
 *
 * Layar-layarnya tetap stateless dan tidak tahu-menahu soal ViewModel: mereka
 * hanya menerima data yang sudah jadi. Semua teks lewat `strings.xml`.
 */

@Composable
fun ErrorText.resolve(): String = when (this) {
    is ErrorText.Res -> stringResource(id, *args.toTypedArray())
    is ErrorText.Raw -> value
}

@Composable
fun BukaRekeningFlowState.toPilihJenisUiState(): BukaRekeningPilihJenisUiState =
    BukaRekeningPilihJenisUiState(
        isLoading = isLoading,
        error = error?.resolve(),
        jenisRekeningList = defaultJenisRekeningList(),
    )

@Composable
fun BukaRekeningFlowState.toPilihKartuUiState(): BukaRekeningPilihKartuUiState =
    BukaRekeningPilihKartuUiState(
        isLoading = isLoading,
        error = error?.resolve(),
        kartuList = defaultKartuPasporList(),
        selectedIndex = selectedCardIndex,
    )

@Composable
fun BukaRekeningFlowState.toHasilFotoUiState(): BukaRekeningHasilFotoUiState {
    val data = ktpData ?: return BukaRekeningHasilFotoUiState(isFotoValid = false)
    val cornersVisible = ocr?.allCornersVisible ?: true
    val sudut = stringResource(
        if (cornersVisible) {
            R.string.buka_rekening_hasil_foto_sudut_presisi
        } else {
            R.string.buka_rekening_hasil_foto_sudut_terpotong
        },
    )
    return BukaRekeningHasilFotoUiState(
        isFotoValid = !needsRetake && (ocr == null || ocr.dukcapilMatch) &&
            ocr?.glareDetected != true && cornersVisible,
        resolusiInfo = if (captureResolution.isBlank()) {
            ""
        } else {
            stringResource(
                R.string.buka_rekening_hasil_foto_resolusi_format,
                captureResolution,
                sudut,
            )
        },
        ocrAccuracy = stringResource(
            R.string.buka_rekening_hasil_foto_akurasi_format,
            formatAccuracy(ktpAccuracy),
        ),
        nik = data.nik.groupNik(),
        namaLengkap = data.namaLengkap,
        tempatTanggalLahir = joinNonBlank(
            data.tempatLahir.uppercase(INDONESIAN),
            data.tanggalLahir.toKtpDate(),
        ),
        alamat = joinNonBlank(
            data.alamat,
            data.rtRw,
            data.kelurahan,
            data.kecamatan,
            data.kota,
        ).uppercase(INDONESIAN),
        agama = data.agama.uppercase(INDONESIAN),
        statusPerkawinan = data.statusPerkawinan.uppercase(INDONESIAN),
    )
}

@Composable
fun BukaRekeningFlowState.toDataPribadiUiState(): BukaRekeningDataPribadiUiState {
    val data = ktpData ?: return BukaRekeningDataPribadiUiState(
        alamatDomisiliSama = alamatDomisiliSama,
    )
    return BukaRekeningDataPribadiUiState(
        nik = data.nik,
        namaLengkap = data.namaLengkap,
        tempatLahir = data.tempatLahir,
        tanggalLahir = data.tanggalLahir.toDisplayDate(),
        jenisKelamin = jenisKelamin ?: JenisKelamin.LAKI_LAKI,
        alamatLengkap = data.alamat,
        rtRw = data.rtRw.replace("/", " / "),
        kodePos = "",
        kelurahanKecamatan = joinNonBlank(data.kelurahan, data.kecamatan),
        kotaProvinsi = joinNonBlank(data.kota, data.provinsi),
        alamatDomisiliSama = alamatDomisiliSama,
        // Tiga nilai di bawah masih default karena layar belum punya picker-nya.
        jenisPekerjaan = stringResource(R.string.buka_rekening_pekerjaan_karyawan_swasta),
        penghasilanPerBulan = stringResource(R.string.buka_rekening_penghasilan_10_20_juta),
        sumberDanaUtama = stringResource(R.string.buka_rekening_sumber_dana_gaji),
    )
}

/**
 * Kode OTP ikut diturunkan dari flow state, bukan disimpan layar, supaya hanya ada
 * satu tempat yang memegangnya — dan tempat itu tidak pernah menyentuh disk.
 */
@Composable
fun BukaRekeningFlowState.toOtpUiState(): BukaRekeningOtpUiState =
    BukaRekeningOtpUiState(
        nomorTersamar = otpSentTo,
        kode = otpCode,
        detikTersisa = otpCountdownSeconds,
        isLoading = isLoading,
        isInputDiblokir = isOtpInputBlocked,
        isKirimUlangDiblokir = isOtpResendBlocked,
        error = error?.resolve(),
    )

fun BukaRekeningFlowState.toKameraFotoUiState(): BukaRekeningKameraFotoUiState =
    BukaRekeningKameraFotoUiState(
        isAutoCaptureEnabled = isAutoCaptureEnabled,
        flashMode = flashMode,
        isDetecting = isProcessingPhoto || isAutoCaptureEnabled,
    )

fun BukaRekeningFlowState.toVerifikasiBiometrikUiState(): VerifikasiBiometrikUiState =
    VerifikasiBiometrikUiState(
        faceDetected = liveness.faceDetected,
        precisionPercent = liveness.precisionPercent,
        currentAction = liveness.completedActions,
        totalActions = LivenessDetector.TOTAL_CHALLENGES,
    )

fun BukaRekeningFlowState.toAntreanUiState(): AntreanVideoCallUiState {
    val ticket = queue ?: return AntreanVideoCallUiState(
        nomorAntrean = "",
        jumlahAntreanDepan = 0,
        estimasiMenit = 0,
        petugasSiap = false,
    )
    return AntreanVideoCallUiState(
        nomorAntrean = ticket.queueNumber,
        jumlahAntreanDepan = ticket.position,
        estimasiMenit = ticket.estimatedWaitSeconds / SECONDS_PER_MINUTE,
        petugasSiap = ticket.position == 0,
    )
}

fun BukaRekeningFlowState.toBuatKredensialUiState(): BuatKredensialUiState =
    BuatKredensialUiState(
        kodeAkses = accessCode,
        konfirmasiKodeAkses = confirmAccessCode,
        isKodeAksesVisible = isAccessCodeVisible,
        isKonfirmasiVisible = isConfirmAccessCodeVisible,
        pinDigitCount = pin.length,
        konfirmasiPinDigitCount = confirmPin.length,
        isPinCocok = pin.isNotEmpty() && pin == confirmPin,
        isValid6Karakter = accessCode.length == CREDENTIAL_LENGTH,
        isValidTidakBerurutan = accessCode.isNotEmpty() && !accessCode.isSequential(),
        isValidTidakBerulang = accessCode.isNotEmpty() && !accessCode.isAllSameChar(),
    )

@Composable
fun BukaRekeningFlowState.toRingkasanUiState(): RingkasanUiState {
    val data = ktpData
    return RingkasanUiState(
        produkRekening = product?.name.orEmpty(),
        isPalingPopuler = selectedProductIndex == POPULAR_PRODUCT_INDEX,
        mataUang = product?.currency.orEmpty(),
        setoranAwalMinimum = product?.minInitialDeposit?.let {
            stringResource(R.string.buka_rekening_ringkasan_setoran_format, formatAmount(it))
        }.orEmpty(),
        fasilitasDigital = product?.features?.joinToString(", ").orEmpty(),
        namaLengkap = data?.namaLengkap.orEmpty(),
        nik = data?.nik?.groupNik().orEmpty(),
        isNikVerified = ocr?.dukcapilMatch == true,
        tempatTanggalLahir = data?.let {
            joinNonBlank(it.tempatLahir, it.tanggalLahir.toDisplayDate())
        }.orEmpty(),
        alamatKtp = data?.let { joinNonBlank(it.alamat, it.kota) }.orEmpty(),
        pekerjaan = stringResource(R.string.buka_rekening_pekerjaan_karyawan_swasta),
        nomorHandphone = otpSentTo,
        isOtpVerified = isOtpVerified,
        alamatEmail = "",
        isOcrVerified = isOcrVerified,
        isBiometrikVerified = isBiometrikVerified,
        isVideoCallVerified = isVideoCallVerified,
        videoCallCsName = videoCallCsName,
        isAgreed = isAgreed,
    )
}

@Composable
fun BukaRekeningFlowState.toBerhasilDibuatUiState(): BerhasilDibuatUiState {
    val created = account ?: return BerhasilDibuatUiState()
    return BerhasilDibuatUiState(
        jenisRekening = product?.name ?: created.accountType,
        nomorRekening = created.accountNumber.groupNik(),
        namaPemilik = created.accountHolder,
        kantorCabang = created.branch,
        minimumSetoran = stringResource(
            R.string.buka_rekening_ringkasan_setoran_format,
            formatAmount(created.minInitialDeposit),
        ),
    )
}

// -- Format --------------------------------------------------------------------

private val INDONESIAN = Locale.forLanguageTag("id-ID")
private const val SECONDS_PER_MINUTE = 60
private const val POPULAR_PRODUCT_INDEX = 0

private fun formatAccuracy(value: Double): String = String.format(INDONESIAN, "%.1f", value)

/** `500000` menjadi `500.000`, pemisah ribuan sesuai lokal Indonesia. */
private fun formatAmount(value: Long): String = String.format(INDONESIAN, "%,d", value)
