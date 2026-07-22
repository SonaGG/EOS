package gg.sona.eos.ecom

public class EcomEntitlement(
    public val entitlementName: String,
    public val entitlementId: String,
    public val catalogItemId: String,
    public val serverIndex: Int,
    public val redeemed: Boolean,
    public val endTimestamp: Long,
)
