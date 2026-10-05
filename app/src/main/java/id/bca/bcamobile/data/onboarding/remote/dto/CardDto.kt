package id.bca.bcamobile.data.onboarding.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Katalog kartu Paspor per produk.
 * Kontrak: `bca-mobile-api/docs/08-PILIH-KARTU-API-SPEC.md` §4.
 */
@Serializable
data class CardFeesDto(
    @SerialName("monthly_admin") val monthlyAdmin: Long = 0,
    @SerialName("card_issuance") val cardIssuance: Long = 0,
    @SerialName("card_replacement") val cardReplacement: Long = 0,
)

@Serializable
data class CardLimitsDto(
    @SerialName("cash_withdrawal") val cashWithdrawal: Long = 0,
    @SerialName("transfer_bca") val transferBca: Long = 0,
    @SerialName("transfer_interbank") val transferInterbank: Long = 0,
    @SerialName("debit_purchase") val debitPurchase: Long = 0,
)

@Serializable
data class CardAvailabilityDto(
    val status: String = "",
    @SerialName("reason_key") val reasonKey: String? = null,
)

@Serializable
data class CardDeliveryDto(
    @SerialName("physical_card_available") val physicalCardAvailable: Boolean = false,
    @SerialName("estimated_days_min") val estimatedDaysMin: Int? = null,
    @SerialName("estimated_days_max") val estimatedDaysMax: Int? = null,
    @SerialName("branch_pickup_available") val branchPickupAvailable: Boolean = false,
)

@Serializable
data class CardEligibilityDto(
    @SerialName("min_age") val minAge: Int = 0,
    @SerialName("min_initial_deposit") val minInitialDeposit: Long = 0,
)

@Serializable
data class CardDto(
    @SerialName("card_type") val cardType: String = "",
    val name: String = "",
    val network: String = "",
    @SerialName("tier_key") val tierKey: String = "",
    val style: String = "",
    @SerialName("badge_key") val badgeKey: String? = null,
    @SerialName("is_popular") val isPopular: Boolean = false,
    @SerialName("display_order") val displayOrder: Int = 0,
    val fees: CardFeesDto? = null,
    val limits: CardLimitsDto? = null,
    val availability: CardAvailabilityDto? = null,
    val delivery: CardDeliveryDto? = null,
    val eligibility: CardEligibilityDto? = null,
)

@Serializable
data class CardCatalogResponse(
    @SerialName("catalog_version") val catalogVersion: String = "",
    @SerialName("product_type") val productType: String = "",
    @SerialName("default_card_type") val defaultCardType: String? = null,
    val currency: String = "",
    val cards: List<CardDto> = emptyList(),
)

/** Ringkasan kartu yang menempel pada sesi — dibalas `sessions`, `card`, dan `submit`. */
@Serializable
data class SessionCardDto(
    @SerialName("card_type") val cardType: String = "",
    val name: String = "",
    val style: String = "",
    val fees: CardFeesDto? = null,
    @SerialName("catalog_version") val catalogVersion: String? = null,
)

@Serializable
data class SetCardRequest(
    @SerialName("card_type") val cardType: String,
    @SerialName("card_catalog_version") val cardCatalogVersion: String? = null,
)

@Serializable
data class SetCardResponse(
    val card: SessionCardDto? = null,
    @SerialName("current_step") val currentStep: String? = null,
)
