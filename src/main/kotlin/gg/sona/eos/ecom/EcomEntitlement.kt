package gg.sona.eos.ecom

class EcomEntitlement(
    val entitlementName: String,
    val entitlementId: String,
    val catalogItemId: String,
    val serverIndex: Int,
    val redeemed: Boolean,
    val endTimestamp: Long,
)
