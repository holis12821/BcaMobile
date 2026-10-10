package id.bca.bcamobile.data.onboarding

import id.bca.bcamobile.core.network.ApiCaller
import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.core.liveness.LivenessChallenge
import id.bca.bcamobile.core.security.LivenessAttestor
import id.bca.bcamobile.core.security.RsaEncryptor
import id.bca.bcamobile.core.security.buildPinPayload
import id.bca.bcamobile.data.onboarding.local.OnboardingSessionStore
import id.bca.bcamobile.data.onboarding.mapper.toDomain
import id.bca.bcamobile.data.onboarding.mapper.toDto
import id.bca.bcamobile.data.onboarding.mapper.toMetaDto
import id.bca.bcamobile.data.onboarding.remote.OnboardingApi
import id.bca.bcamobile.data.onboarding.remote.dto.CreateSessionRequest
import id.bca.bcamobile.data.onboarding.remote.dto.JoinQueueRequest
import id.bca.bcamobile.data.onboarding.remote.dto.LivenessChallengeRequest
import id.bca.bcamobile.data.onboarding.remote.dto.ResendOtpRequest
import id.bca.bcamobile.data.onboarding.remote.dto.SavePersonalDataRequest
import id.bca.bcamobile.data.onboarding.remote.dto.SetCardRequest
import id.bca.bcamobile.data.onboarding.remote.dto.SetCredentialsRequest
import id.bca.bcamobile.data.onboarding.remote.dto.SubmitRequest
import id.bca.bcamobile.data.onboarding.remote.dto.VerifyOtpRequest
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.domain.onboarding.model.BiometricResult
import id.bca.bcamobile.domain.onboarding.model.CardCatalog
import id.bca.bcamobile.domain.onboarding.model.CreatedAccount
import id.bca.bcamobile.domain.onboarding.model.CredentialResult
import id.bca.bcamobile.domain.onboarding.model.KtpOcrResult
import id.bca.bcamobile.domain.onboarding.model.LivenessSubmission
import id.bca.bcamobile.domain.onboarding.model.OnboardingSession
import id.bca.bcamobile.domain.onboarding.model.OtpChallenge
import id.bca.bcamobile.domain.onboarding.model.OtpVerification
import id.bca.bcamobile.domain.onboarding.model.PasporCardType
import id.bca.bcamobile.domain.onboarding.model.PersonalData
import id.bca.bcamobile.domain.onboarding.model.PersonalDataResult
import id.bca.bcamobile.domain.onboarding.model.ProductType
import id.bca.bcamobile.domain.onboarding.model.QueueTicket
import id.bca.bcamobile.domain.onboarding.model.SavingsProductCatalog
import id.bca.bcamobile.domain.onboarding.model.SelectedCard
import id.bca.bcamobile.domain.onboarding.model.TncDocument
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val api: OnboardingApi,
    private val caller: ApiCaller,
    private val store: OnboardingSessionStore,
    private val encryptor: RsaEncryptor,
    private val attestor: LivenessAttestor,
    private val json: Json,
) : OnboardingRepository {

    override fun savedSessionId(): String? = store.sessionId

    override fun clearLocalSession() = store.clear()

    /**
     * Katalog tanpa produk yang layak dipilih jadi [ApiFailure.Unknown], bukan
     * `Success` berisi daftar kosong: layar punya satu jalur pemulihan untuk keduanya —
     * daftar bawaan `strings.xml` — dan membedakannya hanya menambah cabang yang
     * perilakunya sama.
     */
    override suspend fun savingsProducts(): DataResult<SavingsProductCatalog> =
        onboardingCall { api.savingsProducts() }.flatMapSuccess { response ->
            response.toDomain()
                ?.let { DataResult.Success(it) }
                ?: DataResult.Failure(ApiFailure.Unknown)
        }

    /**
     * Dokumen cacat (tanpa versi atau tanpa pasal) jadi [ApiFailure.Unknown], bukan
     * `Success(null)`: layar harus menampilkan keadaan gagal lengkap dengan "Coba Lagi",
     * bukan kerangka kosong yang tombolnya mati tanpa penjelasan.
     */
    override suspend fun tnc(): DataResult<TncDocument> =
        onboardingCall { api.tnc() }.flatMapSuccess { response ->
            response.toDomain()
                ?.let { DataResult.Success(it) }
                ?: DataResult.Failure(ApiFailure.Unknown)
        }

    override suspend fun cardCatalog(productType: ProductType): DataResult<CardCatalog> =
        onboardingCall { api.cardCatalog(productType.wireValue) }.mapSuccess { it.toDomain() }

    override suspend fun createSession(
        productType: ProductType,
        acceptedTncVersion: String,
        cardType: PasporCardType?,
        cardCatalogVersion: String?,
    ): DataResult<OnboardingSession> {
        // Sesi baru berarti idempotency key lama tidak relevan lagi.
        store.clear()
        val result = onboardingCall {
            api.createSession(
                CreateSessionRequest(
                    productType = productType.wireValue,
                    deviceId = store.deviceId(),
                    acceptedTncVersion = acceptedTncVersion,
                    cardType = cardType?.wireValue,
                    cardCatalogVersion = cardCatalogVersion,
                ),
            )
        }
        return result.mapSuccess { it.toDomain().also { session -> store.sessionId = session.sessionId } }
    }

    override suspend fun selectCard(
        cardType: PasporCardType,
        cardCatalogVersion: String?,
    ): DataResult<SelectedCard> = withSession { sessionId ->
        onboardingCall(allowRetry = false) {
            api.setCard(
                sessionId = sessionId,
                request = SetCardRequest(
                    cardType = cardType.wireValue,
                    cardCatalogVersion = cardCatalogVersion,
                ),
            )
        }.flatMapSuccess { response ->
            // Tanpa objek `card`, client tidak tahu kartu mana yang akhirnya tercatat.
            response.card
                ?.let { DataResult.Success(it.toDomain()) }
                ?: DataResult.Failure(ApiFailure.Unknown)
        }
    }

    override suspend fun getSession(sessionId: String): DataResult<OnboardingSession> =
        onboardingCall { api.getSession(sessionId) }.mapSuccess { it.toDomain() }

    override suspend fun cancelSession(): DataResult<Unit> {
        val sessionId = store.sessionId ?: return DataResult.Failure(ApiFailure.SessionNotFound)
        val result = onboardingCall { api.cancelSession(sessionId) }
        store.clear()
        return result.mapSuccess { }
    }

    override suspend fun uploadKtpPhoto(
        photo: File,
        flashUsed: Boolean,
        autoCaptured: Boolean,
        resolution: String,
        clientOcrText: String,
    ): DataResult<KtpOcrResult> = withSession { sessionId ->
        onboardingCall {
            api.processOcr(
                sessionId = sessionId.asTextPart(),
                ktpPhoto = photo.asImagePart(PART_KTP_PHOTO),
                flashUsed = flashUsed.toString().asTextPart(),
                autoCaptured = autoCaptured.toString().asTextPart(),
                resolution = resolution.asTextPart(),
                clientOcrText = clientOcrText.asTextPart(),
            )
        }.mapSuccess { it.toDomain() }
    }

    override suspend fun getOcrResult(): DataResult<KtpOcrResult> = withSession { sessionId ->
        onboardingCall { api.getOcrResult(sessionId) }.mapSuccess { it.toDomain() }
    }

    override suspend fun savePersonalData(
        ocrId: String,
        data: PersonalData,
    ): DataResult<PersonalDataResult> = withSession { sessionId ->
        onboardingCall {
            api.savePersonalData(
                SavePersonalDataRequest(
                    sessionId = sessionId,
                    ocrId = ocrId,
                    personalData = data.toDto(),
                ),
            )
        }.mapSuccess { it.toDomain() }
    }

    override suspend fun verifyOtp(otpCode: String): DataResult<OtpVerification> =
        withSession { sessionId ->
            // OTP tidak boleh diulang otomatis: percobaan terbuang memicu OTP_BLOCKED.
            onboardingCall(allowRetry = false) {
                api.verifyOtp(VerifyOtpRequest(sessionId = sessionId, otpCode = otpCode))
            }.flatMapSuccess { response ->
                // `verified: false` dengan HTTP 200 di luar kontrak; diperlakukan
                // seperti kode salah supaya layar tidak diam tanpa penjelasan.
                if (response.verified) {
                    DataResult.Success(response.toDomain())
                } else {
                    DataResult.Failure(ApiFailure.Business(CODE_OTP_INVALID, ""))
                }
            }
        }

    override suspend fun resendOtp(): DataResult<OtpChallenge> = withSession { sessionId ->
        onboardingCall(allowRetry = false) {
            api.resendOtp(ResendOtpRequest(sessionId = sessionId))
        }.mapSuccess { it.toDomain() }
    }

    override suspend fun requestLivenessChallenge(): DataResult<LivenessChallenge> =
        withSession { sessionId ->
            val publicKey = attestor.publicKey()
                ?: return@withSession DataResult.Failure(
                    ApiFailure.Business(CODE_DEVICE_KEY_MISSING, ""),
                )

            // allowRetry = false: setiap percobaan menerbitkan nonce baru di server,
            // dan percobaan otomatis membakar kuota percobaan nasabah tanpa dia tahu.
            onboardingCall(allowRetry = false) {
                api.requestLivenessChallenge(
                    LivenessChallengeRequest(
                        sessionId = sessionId,
                        deviceKeyId = publicKey.keyId,
                        devicePublicKey = publicKey.base64,
                        signatureAlgorithm = attestor.signatureAlgorithm,
                    ),
                )
            }.mapSuccess { it.toDomain() }
        }

    override suspend fun submitLiveness(
        submission: LivenessSubmission,
    ): DataResult<BiometricResult> = withSession { sessionId ->
        val stepMetaJson = json.encodeToString(submission.stepFrames.map { it.toMetaDto() })
        val riskJson = json.encodeToString(submission.riskSignals.toDto())

        // allowRetry = false: nonce dikonsumsi server secara atomik pada percobaan
        // pertama, jadi percobaan kedua dengan payload yang sama pasti ditolak sebagai
        // pemakaian ulang — dan itu tercatat sebagai kegagalan di jejak audit.
        onboardingCall(allowRetry = false) {
            api.processBiometric(
                sessionId = sessionId.asTextPart(),
                challengeId = submission.challengeId.asTextPart(),
                nonce = submission.nonce.asTextPart(),
                deviceKeyId = submission.deviceKeyId.asTextPart(),
                devicePublicKey = submission.devicePublicKey.asTextPart(),
                signature = submission.signature.asTextPart(),
                signatureAlgorithm = submission.signatureAlgorithm.asTextPart(),
                stepMeta = stepMetaJson.asTextPart(),
                riskSignals = riskJson.asTextPart(),
                integrityToken = submission.integrityToken?.asTextPart(),
                neutralFrame = submission.neutralFrame.asImagePart(
                    partName = PART_NEUTRAL_FRAME,
                    fileName = "neutral.jpg",
                ),
                stepFrames = submission.stepFrames.map { frame ->
                    frame.jpeg.asImagePart(
                        partName = PART_LIVENESS_FRAMES,
                        fileName = "step_${frame.index}.jpg",
                    )
                },
            )
        }.mapSuccess { it.toDomain() }
    }

    override suspend fun joinVideoCallQueue(): DataResult<QueueTicket> = withSession { sessionId ->
        onboardingCall { api.joinQueue(JoinQueueRequest(sessionId = sessionId)) }
            .mapSuccess { it.toDomain() }
    }

    override suspend fun saveCredentials(
        accessCode: String,
        pin: String,
    ): DataResult<CredentialResult> = withSession { sessionId ->
        val keyResult = onboardingCall { api.getPublicKey() }.mapSuccess { it.toDomain() }
        val key = when (keyResult) {
            is DataResult.Failure -> return@withSession keyResult
            is DataResult.Success -> keyResult.value
        }

        // Dev mode: server belum punya kunci RSA, kredensial dikirim apa adanya —
        // `decryptCredential` di backend mengembalikannya tanpa membongkar amplop.
        val accessCodePayload: String
        val pinPayload: String
        if (key.isDevMode) {
            accessCodePayload = accessCode
            pinPayload = pin
        } else {
            // Yang dienkripsi adalah amplop {pin, nonce, ts}, bukan rahasianya
            // langsung: `POST /onboarding/credentials` membongkarnya lewat
            // `DecryptPIN` yang sama dengan endpoint ber-PIN lain, jadi rahasia
            // polos akan ditolak sebagai CRED_DECRYPTION_FAILED.
            accessCodePayload = encryptor.encrypt(buildPinPayload(accessCode), key.publicKeyPem)
                ?: return@withSession DataResult.Failure(
                    ApiFailure.Business(CODE_ENCRYPTION_FAILED, ""),
                )
            pinPayload = encryptor.encrypt(buildPinPayload(pin), key.publicKeyPem)
                ?: return@withSession DataResult.Failure(
                    ApiFailure.Business(CODE_ENCRYPTION_FAILED, ""),
                )
        }

        onboardingCall(allowRetry = false) {
            api.setCredentials(
                SetCredentialsRequest(
                    sessionId = sessionId,
                    accessCodeEncrypted = accessCodePayload,
                    pinEncrypted = pinPayload,
                    encryptionKeyId = key.keyId,
                ),
            )
        }.mapSuccess { it.toDomain() }
    }

    override suspend fun submitApplication(
        agreementVersion: String,
        idempotencyKey: String,
    ): DataResult<CreatedAccount> = withSession { sessionId ->
        onboardingCall {
            api.submit(
                idempotencyKey = idempotencyKey,
                request = SubmitRequest(
                    sessionId = sessionId,
                    agreementAccepted = true,
                    agreementVersion = agreementVersion,
                ),
            )
        }.mapSuccess { it.account.toDomain() }
    }

    // -- Helper ----------------------------------------------------------------

    private inline fun <T> withSession(block: (String) -> DataResult<T>): DataResult<T> {
        val sessionId = store.sessionId ?: return DataResult.Failure(ApiFailure.SessionNotFound)
        return block(sessionId)
    }

    private fun String.asTextPart(): RequestBody = toRequestBody(TEXT_PLAIN.toMediaType())

    private fun File.asImagePart(partName: String): MultipartBody.Part =
        MultipartBody.Part.createFormData(
            partName,
            name,
            asRequestBody(IMAGE_JPEG.toMediaType()),
        )

    /**
     * Frame liveness diunggah dari memori, tanpa pernah menjadi berkas.
     *
     * Sengaja tidak ada varian `File` untuk frame wajah: begitu ada, jalur yang menulis
     * frame ke `cacheDir` akan kembali dengan sendirinya.
     */
    private fun ByteArray.asImagePart(partName: String, fileName: String): MultipartBody.Part =
        MultipartBody.Part.createFormData(
            partName,
            fileName,
            toRequestBody(IMAGE_JPEG.toMediaType()),
        )

    /**
     * 404 tanpa error code pada endpoint onboarding berarti sesinya yang hilang,
     * bukan sumber daya lain. Domain lain memakai default [ApiFailure.Unknown].
     */
    private suspend fun <T> onboardingCall(
        allowRetry: Boolean = true,
        block: suspend () -> Response<ApiEnvelope<T>>,
    ): DataResult<T> = caller.call(
        allowRetry = allowRetry,
        notFoundAs = ApiFailure.SessionNotFound,
        block = block,
    )

    private companion object {
        const val TEXT_PLAIN = "text/plain"
        const val IMAGE_JPEG = "image/jpeg"
        const val PART_KTP_PHOTO = "ktp_photo"
        const val PART_NEUTRAL_FRAME = "neutral_frame"
        const val PART_LIVENESS_FRAMES = "liveness_frames"
        const val CODE_OTP_INVALID = "OTP_INVALID"
        const val CODE_ENCRYPTION_FAILED = "CRED_ENCRYPTION_FAILED"

        /**
         * Keystore perangkat menolak membuat kunci pengikat. Dilaporkan sebagai
         * kegagalan, bukan dilewati: tanpa kunci, server tidak bisa memastikan
         * payload datang dari perangkat ini.
         */
        const val CODE_DEVICE_KEY_MISSING = "CLIENT_DEVICE_KEY_MISSING"
    }
}

private inline fun <T, R> DataResult<T>.mapSuccess(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(value))
    is DataResult.Failure -> this
}

private inline fun <T, R> DataResult<T>.flatMapSuccess(
    transform: (T) -> DataResult<R>,
): DataResult<R> = when (this) {
    is DataResult.Success -> transform(value)
    is DataResult.Failure -> this
}
