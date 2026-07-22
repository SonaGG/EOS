package gg.sona.eos.ecom

class EcomCatalogOffer(
    val id: String,
    val title: String,
    val description: String,
    val longDescription: String,
    val technicalDetails: String,
    val currencyCode: String,
    val numericPrice: Int,
    val originalNumericPrice: Int,
    val currentPrice64: Long,
    val originalPrice64: Long,
    val discountPercent: Int,
    val expirationDate: Long,
    val availableForPurchase: Boolean,
    val availableForEntitlement: Boolean,
    val itemCount: Int,
)
