package gg.sona.eos.ecom

public class EcomCatalogOffer(
    public val id: String,
    public val title: String,
    public val description: String,
    public val longDescription: String,
    public val technicalDetails: String,
    public val currencyCode: String,
    public val numericPrice: Int,
    public val originalNumericPrice: Int,
    public val currentPrice64: Long,
    public val originalPrice64: Long,
    public val discountPercent: Int,
    public val expirationDate: Long,
    public val availableForPurchase: Boolean,
    public val availableForEntitlement: Boolean,
    public val itemCount: Int,
)
