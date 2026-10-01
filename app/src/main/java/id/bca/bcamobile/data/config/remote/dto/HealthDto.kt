package id.bca.bcamobile.data.config.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeatureFlagsDto(
    @SerialName("ewallet_enabled") val eWalletEnabled: Boolean = true,
    @SerialName("qris_enabled") val qrisEnabled: Boolean = true,
    @SerialName("transfer_antar_bank_enabled") val interbankTransferEnabled: Boolean = true,
    @SerialName("virtual_account_enabled") val virtualAccountEnabled: Boolean = true,
)

@Serializable
data class HealthResponse(
    val service: String = "",
    val database: String = "",
    val cache: String = "",
    @SerialName("maintenance_mode") val maintenanceMode: Boolean = false,
    @SerialName("maintenance_message") val maintenanceMessage: String? = null,
    @SerialName("minimum_app_version") val minimumAppVersion: String = "",
    @SerialName("current_app_version") val currentAppVersion: String = "",
    @SerialName("force_update") val forceUpdate: Boolean = false,
    @SerialName("feature_flags") val featureFlags: FeatureFlagsDto? = null,
)
