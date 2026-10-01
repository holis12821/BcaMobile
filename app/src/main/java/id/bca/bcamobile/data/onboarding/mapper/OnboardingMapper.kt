package id.bca.bcamobile.data.onboarding.mapper

import id.bca.bcamobile.data.onboarding.remote.dto.AccountDto
import id.bca.bcamobile.data.onboarding.remote.dto.AlamatKtpDto
import id.bca.bcamobile.data.onboarding.remote.dto.BiometricResponse
import id.bca.bcamobile.data.onboarding.remote.dto.CardCatalogResponse
import id.bca.bcamobile.data.onboarding.remote.dto.CardDto
import id.bca.bcamobile.data.onboarding.remote.dto.SessionCardDto
import id.bca.bcamobile.data.onboarding.remote.dto.CreateSessionResponse
import id.bca.bcamobile.data.onboarding.remote.dto.GetSessionResponse
import id.bca.bcamobile.data.onboarding.remote.dto.JoinQueueResponse
import id.bca.bcamobile.data.onboarding.remote.dto.LivenessMetaDto
import id.bca.bcamobile.data.onboarding.remote.dto.OcrResponse
import id.bca.bcamobile.data.onboarding.remote.dto.PersonalDataDto
import id.bca.bcamobile.data.onboarding.remote.dto.ProductDto
import id.bca.bcamobile.data.onboarding.remote.dto.PublicKeyResponse
import id.bca.bcamobile.data.onboarding.remote.dto.ResendOtpResponse
import id.bca.bcamobile.data.onboarding.remote.dto.SavePersonalDataResponse
import id.bca.bcamobile.data.onboarding.remote.dto.SetCredentialsResponse
import id.bca.bcamobile.data.onboarding.remote.dto.StepsCompletedDto
import id.bca.bcamobile.data.onboarding.remote.dto.VerifyOtpResponse
import id.bca.bcamobile.domain.onboarding.model.AlamatKtp
import id.bca.bcamobile.domain.onboarding.model.BiometricResult
import id.bca.bcamobile.domain.onboarding.model.CardAvailability
import id.bca.bcamobile.domain.onboarding.model.CardAvailabilityStatus
import id.bca.bcamobile.domain.onboarding.model.CardBadge
import id.bca.bcamobile.domain.onboarding.model.CardCatalog
import id.bca.bcamobile.domain.onboarding.model.CardDelivery
import id.bca.bcamobile.domain.onboarding.model.CardEligibility
import id.bca.bcamobile.domain.onboarding.model.CardFees
import id.bca.bcamobile.domain.onboarding.model.CardLimits
import id.bca.bcamobile.domain.onboarding.model.CardStyle
import id.bca.bcamobile.domain.onboarding.model.CardTier
import id.bca.bcamobile.domain.onboarding.model.CardUnavailableReason
import id.bca.bcamobile.domain.onboarding.model.PasporCard
import id.bca.bcamobile.domain.onboarding.model.PasporCardType
import id.bca.bcamobile.domain.onboarding.model.SelectedCard
import id.bca.bcamobile.domain.onboarding.model.CreatedAccount
import id.bca.bcamobile.domain.onboarding.model.CredentialResult
import id.bca.bcamobile.domain.onboarding.model.KtpData
import id.bca.bcamobile.domain.onboarding.model.KtpOcrResult
import id.bca.bcamobile.domain.onboarding.model.LivenessMeta
import id.bca.bcamobile.domain.onboarding.model.OnboardingSession
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.domain.onboarding.model.IceServer
import id.bca.bcamobile.domain.onboarding.model.OperatingHours
import id.bca.bcamobile.domain.onboarding.model.OtpChallenge
import id.bca.bcamobile.domain.onboarding.model.OtpVerification
import id.bca.bcamobile.domain.onboarding.model.PersonalData
import id.bca.bcamobile.domain.onboarding.model.PersonalDataResult
import id.bca.bcamobile.domain.onboarding.model.Product
import id.bca.bcamobile.domain.onboarding.model.ProductType
import id.bca.bcamobile.domain.onboarding.model.PublicKeyMaterial
import id.bca.bcamobile.domain.onboarding.model.QueueTicket
import id.bca.bcamobile.domain.onboarding.model.StepsCompleted

// -- DTO -> domain -------------------------------------------------------------

/**
 * `current_step` yang tidak dikenal tidak boleh menjatuhkan flow; [fallback]
 * dipakai supaya versi server yang lebih baru tetap bisa dijalankan client lama.
 */
private fun stepOf(wire: String?, fallback: OnboardingStep): OnboardingStep =
    OnboardingStep.fromWire(wire) ?: fallback

fun ProductDto.toDomain(): Product? {
    val productType = ProductType.fromWire(type) ?: return null
    return Product(
        type = productType,
        name = name,
        currency = currency,
        minInitialDeposit = minInitialDeposit,
        features = features,
    )
}

fun StepsCompletedDto.toDomain(): StepsCompleted = StepsCompleted(
    tncAccepted = tncAccepted,
    cardSelected = cardSelected,
    ocrVerified = ocrVerified,
    personalDataSaved = personalDataSaved,
    otpVerified = otpVerified,
    biometricVerified = biometricVerified,
    videoCallVerified = videoCallVerified,
    credentialsSet = credentialsSet,
    submitted = submitted,
)

fun CreateSessionResponse.toDomain(): OnboardingSession = OnboardingSession(
    sessionId = sessionId,
    product = product?.toDomain(),
    card = card?.toDomain(),
    currentStep = stepOf(currentStep, OnboardingStep.OCR),
    expiresAt = expiresAt,
)

fun GetSessionResponse.toDomain(): OnboardingSession = OnboardingSession(
    sessionId = sessionId,
    product = product?.toDomain(),
    card = card?.toDomain(),
    currentStep = stepOf(currentStep, OnboardingStep.OCR),
    stepsCompleted = stepsCompleted?.toDomain() ?: StepsCompleted(),
    expiresAt = expiresAt,
)

fun SessionCardDto.toDomain(): SelectedCard = SelectedCard(
    cardType = PasporCardType.fromWire(cardType),
    name = name,
    style = CardStyle.fromWire(style),
    monthlyAdminFee = fees?.monthlyAdmin ?: 0L,
    catalogVersion = catalogVersion?.takeIf(String::isNotBlank),
)

/** Kartu tanpa `card_type` yang dikenal dibuang: tidak ada gunanya menampilkan pilihan yang tidak bisa dikirim. */
fun CardCatalogResponse.toDomain(): CardCatalog = CardCatalog(
    catalogVersion = catalogVersion,
    productType = ProductType.fromWire(productType),
    defaultCardType = PasporCardType.fromWire(defaultCardType),
    currency = currency,
    cards = cards.mapNotNull { it.toDomain() },
)

fun CardDto.toDomain(): PasporCard? {
    val type = PasporCardType.fromWire(cardType) ?: return null
    return PasporCard(
        cardType = type,
        name = name,
        network = network,
        tier = CardTier.fromWire(tierKey),
        style = CardStyle.fromWire(style),
        badge = CardBadge.fromWire(badgeKey),
        isPopular = isPopular,
        displayOrder = displayOrder,
        fees = CardFees(
            monthlyAdmin = fees?.monthlyAdmin ?: 0L,
            cardIssuance = fees?.cardIssuance ?: 0L,
            cardReplacement = fees?.cardReplacement ?: 0L,
        ),
        limits = CardLimits(
            cashWithdrawal = limits?.cashWithdrawal ?: 0L,
            transferBca = limits?.transferBca ?: 0L,
            transferInterbank = limits?.transferInterbank ?: 0L,
            debitPurchase = limits?.debitPurchase ?: 0L,
        ),
        availability = CardAvailability(
            status = CardAvailabilityStatus.fromWire(availability?.status),
            reason = CardUnavailableReason.fromWire(availability?.reasonKey),
        ),
        delivery = CardDelivery(
            physicalCardAvailable = delivery?.physicalCardAvailable ?: false,
            estimatedDaysMin = delivery?.estimatedDaysMin,
            estimatedDaysMax = delivery?.estimatedDaysMax,
            branchPickupAvailable = delivery?.branchPickupAvailable ?: false,
        ),
        eligibility = CardEligibility(
            minAge = eligibility?.minAge ?: 0,
            minInitialDeposit = eligibility?.minInitialDeposit ?: 0L,
        ),
    )
}

fun OcrResponse.toDomain(): KtpOcrResult = KtpOcrResult(
    ocrId = ocrId,
    accuracyPercent = accuracyPercent,
    extracted = KtpData(
        nik = extracted.nik,
        namaLengkap = extracted.namaLengkap,
        tempatLahir = extracted.tempatLahir,
        tanggalLahir = extracted.tanggalLahir,
        jenisKelamin = extracted.jenisKelamin,
        alamat = extracted.alamat,
        rtRw = extracted.rtRw,
        kelurahan = extracted.kelurahan,
        kecamatan = extracted.kecamatan,
        kota = extracted.kota,
        provinsi = extracted.provinsi,
        agama = extracted.agama,
        statusPerkawinan = extracted.statusPerkawinan,
    ),
    dukcapilMatch = dukcapilMatch,
    sharpness = photoQuality?.sharpness,
    glareDetected = photoQuality?.glareDetected ?: false,
    allCornersVisible = photoQuality?.allCornersVisible ?: true,
)

fun SavePersonalDataResponse.toDomain(): PersonalDataResult = PersonalDataResult(
    personalDataId = personalDataId,
    otpSentTo = otpSentTo,
    otpExpiresAt = otpExpiresAt,
    currentStep = stepOf(currentStep, OnboardingStep.OTP_VERIFY),
)

fun ResendOtpResponse.toDomain(): OtpChallenge = OtpChallenge(
    otpSentTo = otpSentTo,
    otpExpiresAt = otpExpiresAt,
)

fun VerifyOtpResponse.toDomain(): OtpVerification = OtpVerification(
    verified = verified,
    currentStep = stepOf(currentStep, OnboardingStep.BIOMETRIC),
)

fun BiometricResponse.toDomain(): BiometricResult = BiometricResult(
    biometricId = biometricId,
    livenessVerified = livenessVerified,
    livenessScore = livenessScore,
    faceMatchWithKtp = faceMatchWithKtp,
    faceMatchScore = faceMatchScore,
)

fun JoinQueueResponse.toDomain(): QueueTicket = QueueTicket(
    queueId = queueId,
    queueNumber = queueNumber,
    position = position,
    estimatedWaitSeconds = estimatedWaitSeconds,
    operatingHours = operatingHours?.let {
        OperatingHours(start = it.start, end = it.end, timezone = it.timezone)
    },
    signalingUrl = signalingUrl,
    signalingExpiresInSeconds = signalingExpiresIn,
    iceServers = iceServers.map {
        IceServer(urls = it.urls, username = it.username, credential = it.credential)
    },
)

fun PublicKeyResponse.toDomain(): PublicKeyMaterial = PublicKeyMaterial(
    algorithm = algorithm,
    keyId = keyId,
    publicKeyPem = publicKeyPem,
)

fun SetCredentialsResponse.toDomain(): CredentialResult = CredentialResult(
    credentialId = credentialId,
    biometricLoginAvailable = biometricLoginAvailable,
)

fun AccountDto.toDomain(): CreatedAccount = CreatedAccount(
    accountNumber = accountNumber,
    accountType = accountType,
    accountHolder = accountHolder,
    branch = branch,
    branchCode = branchCode,
    currency = currency,
    status = status,
    minInitialDeposit = minInitialDeposit,
    initialDepositDeadline = initialDepositDeadline,
)

// -- Domain -> DTO -------------------------------------------------------------

fun PersonalData.toDto(): PersonalDataDto = PersonalDataDto(
    nik = nik,
    namaLengkap = namaLengkap,
    tempatLahir = tempatLahir,
    tanggalLahir = tanggalLahir,
    jenisKelamin = jenisKelamin,
    alamatKtp = alamatKtp.toDto(),
    alamatDomisiliSama = alamatDomisiliSama,
    pekerjaan = pekerjaan,
    penghasilanPerBulan = penghasilanPerBulan,
    sumberDanaUtama = sumberDanaUtama,
    nomorHp = nomorHp,
    email = email,
)

fun AlamatKtp.toDto(): AlamatKtpDto = AlamatKtpDto(
    alamatLengkap = alamatLengkap,
    rtRw = rtRw,
    kodePos = kodePos,
    kelurahan = kelurahan,
    kecamatan = kecamatan,
    kota = kota,
    provinsi = provinsi,
)

fun LivenessMeta.toDto(): LivenessMetaDto = LivenessMetaDto(
    challengeType = challengeType,
    completedActions = completedActions,
    precisionScore = precisionScore,
)
