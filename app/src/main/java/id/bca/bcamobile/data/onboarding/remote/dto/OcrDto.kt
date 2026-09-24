package id.bca.bcamobile.data.onboarding.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExtractedKtpDto(
    val nik: String = "",
    @SerialName("nama_lengkap") val namaLengkap: String = "",
    @SerialName("tempat_lahir") val tempatLahir: String = "",
    @SerialName("tanggal_lahir") val tanggalLahir: String = "",
    @SerialName("jenis_kelamin") val jenisKelamin: String = "",
    val alamat: String = "",
    @SerialName("rt_rw") val rtRw: String = "",
    val kelurahan: String = "",
    val kecamatan: String = "",
    val kota: String = "",
    val provinsi: String = "",
    val agama: String = "",
    @SerialName("status_perkawinan") val statusPerkawinan: String = "",
)

@Serializable
data class PhotoQualityDto(
    val sharpness: String? = null,
    @SerialName("glare_detected") val glareDetected: Boolean = false,
    @SerialName("all_corners_visible") val allCornersVisible: Boolean = true,
)

@Serializable
data class OcrResponse(
    @SerialName("ocr_id") val ocrId: String,
    @SerialName("accuracy_percent") val accuracyPercent: Double = 0.0,
    val extracted: ExtractedKtpDto = ExtractedKtpDto(),
    @SerialName("dukcapil_match") val dukcapilMatch: Boolean = false,
    @SerialName("photo_quality") val photoQuality: PhotoQualityDto? = null,
    @SerialName("current_step") val currentStep: String? = null,
)
