package id.bca.bcamobile.data.onboarding.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlamatKtpDto(
    @SerialName("alamat_lengkap") val alamatLengkap: String,
    @SerialName("rt_rw") val rtRw: String,
    @SerialName("kode_pos") val kodePos: String,
    val kelurahan: String,
    val kecamatan: String,
    val kota: String,
    val provinsi: String,
)

@Serializable
data class PersonalDataDto(
    val nik: String,
    @SerialName("nama_lengkap") val namaLengkap: String,
    @SerialName("tempat_lahir") val tempatLahir: String,
    @SerialName("tanggal_lahir") val tanggalLahir: String,
    @SerialName("jenis_kelamin") val jenisKelamin: String,
    @SerialName("alamat_ktp") val alamatKtp: AlamatKtpDto,
    @SerialName("alamat_domisili_sama") val alamatDomisiliSama: Boolean,
    val pekerjaan: String,
    @SerialName("penghasilan_per_bulan") val penghasilanPerBulan: String,
    @SerialName("sumber_dana_utama") val sumberDanaUtama: String,
    @SerialName("nomor_hp") val nomorHp: String,
    val email: String,
)

@Serializable
data class SavePersonalDataRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("ocr_id") val ocrId: String,
    @SerialName("personal_data") val personalData: PersonalDataDto,
)

@Serializable
data class SavePersonalDataResponse(
    @SerialName("personal_data_id") val personalDataId: String,
    @SerialName("otp_sent_to") val otpSentTo: String = "",
    @SerialName("otp_expires_at") val otpExpiresAt: String? = null,
    @SerialName("current_step") val currentStep: String? = null,
)

@Serializable
data class VerifyOtpRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("otp_code") val otpCode: String,
)

@Serializable
data class VerifyOtpResponse(
    val verified: Boolean = false,
    @SerialName("current_step") val currentStep: String? = null,
)

@Serializable
data class ResendOtpRequest(
    @SerialName("session_id") val sessionId: String,
)

@Serializable
data class ResendOtpResponse(
    @SerialName("otp_sent_to") val otpSentTo: String = "",
    @SerialName("otp_expires_at") val otpExpiresAt: String? = null,
)
