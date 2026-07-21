/*
 * Copyright 2026 Sona Softworks LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gg.sona.eos.ecom

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.allocCStringArray
import gg.sona.eos.internal.allocHandleArray
import gg.sona.eos.internal.getInt32
import gg.sona.eos.internal.getInt64
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import gg.sona.eos.internal.withCallArena
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * Ecom interface. Exposes catalog queries, ownership checks, entitlement
 * queries, and the checkout flow.
 */
public class EosEcom internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetEcomInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    // region Ownership

    /**
     * Query the ownership status for a list of catalog item IDs.
     * Up to [MAX_OWNERSHIP_CATALOG_IDS] IDs may be queried at once.
     */
    public fun queryOwnership(
        localUserId: EpicAccountId,
        catalogItemIds: List<String>,
        sandboxIds: List<String>? = null,
    ): CompletableFuture<EosResult> {
        require(catalogItemIds.size <= MAX_OWNERSHIP_CATALOG_IDS) {
            "cannot query more than $MAX_OWNERSHIP_CATALOG_IDS items at once"
        }
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = EcomQueryOwnershipOptions(localUserId, catalogItemIds, sandboxIds)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Ecom_QueryOwnership",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun queryOwnershipBySandboxIds(
        localUserId: EpicAccountId,
        sandboxIds: List<String>,
        catalogItemIds: List<String>,
    ): CompletableFuture<QueryOwnershipBySandboxIdsResult> {
        require(sandboxIds.size <= MAX_OWNERSHIP_SANDBOX_IDS) {
            "cannot query more than $MAX_OWNERSHIP_SANDBOX_IDS sandbox ids at once"
        }
        require(catalogItemIds.size <= MAX_OWNERSHIP_CATALOG_IDS) {
            "cannot query more than $MAX_OWNERSHIP_CATALOG_IDS items at once"
        }
        val future = CompletableFuture<QueryOwnershipBySandboxIdsResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val localUserId = EpicAccountId(data.getInt64(16))
            val sandboxIdCount = data.getInt32(24).toLong() and 0xffffffffL
            future.complete(QueryOwnershipBySandboxIdsResult(result, localUserId, sandboxIdCount.toInt()))
        })
        val options = EcomQueryOwnershipBySandboxIdsOptions(localUserId, sandboxIds, catalogItemIds)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Ecom_QueryOwnershipBySandboxIds",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun queryOwnershipToken(
        localUserId: EpicAccountId,
        catalogItemIds: List<String>,
    ): CompletableFuture<QueryOwnershipTokenResult> {
        require(catalogItemIds.size <= MAX_OWNERSHIP_TOKEN_CATALOG_IDS) {
            "cannot query more than $MAX_OWNERSHIP_TOKEN_CATALOG_IDS items at once"
        }
        val future = CompletableFuture<QueryOwnershipTokenResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val localUserId = EpicAccountId(data.getInt64(16))
            val tokenPtr = data.get(ValueLayout.ADDRESS, 24)
            val token = if (tokenPtr.address() == 0L) "" else
                tokenPtr.reinterpret(Long.MAX_VALUE).getString(0)
            future.complete(QueryOwnershipTokenResult(result, localUserId, token))
        })
        val options = EcomQueryOwnershipTokenOptions(localUserId, catalogItemIds)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Ecom_QueryOwnershipToken",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    // endregion

    // region Entitlements

    public fun queryEntitlements(
        localUserId: EpicAccountId? = null,
        entitlementNames: List<String>? = null,
        includeRedeemed: Boolean = false,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = EcomQueryEntitlementsOptions(
            localUserId, entitlementNames, includeRedeemed
        )
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Ecom_QueryEntitlements",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun queryEntitlementToken(
        localUserId: EpicAccountId,
        entitlementNames: List<String>? = null,
    ): CompletableFuture<QueryEntitlementTokenResult> {
        val future = CompletableFuture<QueryEntitlementTokenResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val localUserId = EpicAccountId(data.getInt64(16))
            val tokenPtr = data.get(ValueLayout.ADDRESS, 24)
            val token = if (tokenPtr.address() == 0L) "" else
                tokenPtr.reinterpret(Long.MAX_VALUE).getString(0)
            future.complete(QueryEntitlementTokenResult(result, localUserId, token))
        })
        val options = EcomQueryEntitlementTokenOptions(localUserId, entitlementNames)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Ecom_QueryEntitlementToken",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun getEntitlementsCount(localUserId: EpicAccountId): Int {
        val options = EcomGetEntitlementsCountOptions(localUserId)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Ecom_GetEntitlementsCount",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        }
    }

    public fun getEntitlementsByNameCount(localUserId: EpicAccountId, name: String): Int {
        val options = EcomGetEntitlementsByNameCountOptions(localUserId, name)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Ecom_GetEntitlementsByNameCount",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        }
    }

    public fun copyEntitlementByIndex(localUserId: EpicAccountId, index: Int): EcomEntitlement? =
        copyEntitlement(
            "EOS_Ecom_CopyEntitlementByIndex",
            EcomCopyEntitlementByIndexOptions(localUserId, index),
        )

    public fun copyEntitlementByNameAndIndex(
        localUserId: EpicAccountId,
        name: String,
        index: Int,
    ): EcomEntitlement? = copyEntitlement(
        "EOS_Ecom_CopyEntitlementByNameAndIndex",
        EcomCopyEntitlementByNameAndIndexOptions(localUserId, name, index),
    )

    public fun copyEntitlementById(localUserId: EpicAccountId, id: String): EcomEntitlement? =
        copyEntitlement(
            "EOS_Ecom_CopyEntitlementById",
            EcomCopyEntitlementByIdOptions(localUserId, id),
        )

    private fun copyEntitlement(
        functionName: String,
        options: StructWriter,
    ): EcomEntitlement? = withCallArena { arena ->
        val outPtr = arena.allocate(ValueLayout.ADDRESS)
        val result = EosResult.fromValue(
            Native.invoke(
                functionName,
                listOf(handle(), options.writeTo(arena), outPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        if (result != EosResult.Success) return@withCallArena null
        val seg = outPtr.get(ValueLayout.ADDRESS, 0)
        if (seg.address() == 0L) return@withCallArena null
        val ent = EcomEntitlement(
            entitlementName = readString(seg, 8),
            entitlementId = readString(seg, 16),
            catalogItemId = readString(seg, 24),
            serverIndex = seg.getInt32(32),
            redeemed = seg.getInt32(36) != 0,
            endTimestamp = seg.getInt64(40),
        )
        Native.downcall(
            "EOS_Ecom_Entitlement_Release",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        ).invokeExact(seg)
        ent
    }

    // endregion

    // region Catalog offers

    public fun queryOffers(
        localUserId: EpicAccountId? = null,
        overrideCatalogNamespace: String? = null,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = EcomQueryOffersOptions(localUserId, overrideCatalogNamespace)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Ecom_QueryOffers",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun getOfferCount(localUserId: EpicAccountId? = null): Int {
        val options = EcomGetOfferCountOptions(localUserId)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Ecom_GetOfferCount",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        }
    }

    public fun copyOfferByIndex(localUserId: EpicAccountId?, index: Int): EcomCatalogOffer? =
        withCallArena { arena ->
            val outPtr = arena.allocate(ValueLayout.ADDRESS)
            val options = EcomCopyOfferByIndexOptions(localUserId, index)
            val result = EosResult.fromValue(
                Native.invoke(
                    "EOS_Ecom_CopyOfferByIndex",
                    listOf(handle(), options.writeTo(arena), outPtr),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (result != EosResult.Success) return@withCallArena null
            val seg = outPtr.get(ValueLayout.ADDRESS, 0)
            if (seg.address() == 0L) return@withCallArena null
            val offer = EcomCatalogOffer(
                id = readString(seg, 8),
                title = readString(seg, 16),
                description = readString(seg, 24),
                longDescription = readString(seg, 32),
                technicalDetails = readString(seg, 40),
                currencyCode = readString(seg, 48),
                numericPrice = seg.getInt32(56),
                originalNumericPrice = seg.getInt32(60),
                currentPrice64 = seg.getInt64(64),
                originalPrice64 = seg.getInt64(72),
                discountPercent = seg.getInt32(80),
                expirationDate = seg.getInt64(88),
                availableForPurchase = seg.getInt32(96) != 0,
                availableForEntitlement = seg.getInt32(100) != 0,
                itemCount = (seg.getInt32(104).toLong() and 0xffffffffL).toInt(),
            )
            Native.downcall(
                "EOS_Ecom_CatalogOffer_Release",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            ).invokeExact(seg)
            offer
        }

    public fun copyOfferById(localUserId: EpicAccountId?, offerId: String): EcomCatalogOffer? =
        withCallArena { arena ->
            val outPtr = arena.allocate(ValueLayout.ADDRESS)
            val options = EcomCopyOfferByIdOptions(localUserId, offerId)
            val result = EosResult.fromValue(
                Native.invoke(
                    "EOS_Ecom_CopyOfferById",
                    listOf(handle(), options.writeTo(arena), outPtr),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (result != EosResult.Success) return@withCallArena null
            val seg = outPtr.get(ValueLayout.ADDRESS, 0)
            if (seg.address() == 0L) return@withCallArena null
            val offer = EcomCatalogOffer(
                id = readString(seg, 8),
                title = readString(seg, 16),
                description = readString(seg, 24),
                longDescription = readString(seg, 32),
                technicalDetails = readString(seg, 40),
                currencyCode = readString(seg, 48),
                numericPrice = seg.getInt32(56),
                originalNumericPrice = seg.getInt32(60),
                currentPrice64 = seg.getInt64(64),
                originalPrice64 = seg.getInt64(72),
                discountPercent = seg.getInt32(80),
                expirationDate = seg.getInt64(88),
                availableForPurchase = seg.getInt32(96) != 0,
                availableForEntitlement = seg.getInt32(100) != 0,
                itemCount = (seg.getInt32(104).toLong() and 0xffffffffL).toInt(),
            )
            Native.downcall(
                "EOS_Ecom_CatalogOffer_Release",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            ).invokeExact(seg)
            offer
        }

    // endregion

    // region Checkout / redeem

    public fun checkout(
        localUserId: EpicAccountId,
        entries: List<CheckoutEntry>,
        preOrderPurchase: Boolean = false,
    ): CompletableFuture<CheckoutResult> {
        require(entries.size <= MAX_CHECKOUT_ENTRIES) {
            "cannot checkout more than $MAX_CHECKOUT_ENTRIES entries at once"
        }
        val future = CompletableFuture<CheckoutResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val localUserId = EpicAccountId(data.getInt64(16))
            val txIdPtr = data.get(ValueLayout.ADDRESS, 24)
            val transactionId = if (txIdPtr.address() == 0L) "" else
                txIdPtr.reinterpret(Long.MAX_VALUE).getString(0)
            future.complete(CheckoutResult(result, localUserId, transactionId))
        })
        val options = EcomCheckoutOptions(localUserId, entries, preOrderPurchase)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Ecom_Checkout",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun redeemEntitlements(
        localUserId: EpicAccountId,
        entitlementIds: List<String>,
    ): CompletableFuture<EosResult> {
        require(entitlementIds.size <= REDEEM_MAX_ENTITLEMENT_IDS) {
            "cannot redeem more than $REDEEM_MAX_ENTITLEMENT_IDS entitlements at once"
        }
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = EcomRedeemEntitlementsOptions(localUserId, entitlementIds)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Ecom_RedeemEntitlements",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    // endregion

    private fun readString(seg: MemorySegment, offset: Long): String {
        val ptr = seg.get(ValueLayout.ADDRESS, offset)
        if (ptr.address() == 0L) return ""
        return ptr.reinterpret(Long.MAX_VALUE).getString(0)
    }

    public companion object {
        public const val MAX_OWNERSHIP_CATALOG_IDS: Int = 400
        public const val MAX_OWNERSHIP_SANDBOX_IDS: Int = 10
        public const val MAX_OWNERSHIP_TOKEN_CATALOG_IDS: Int = 32
        public const val MAX_QUERY_ENTITLEMENT_IDS: Int = 256
        public const val MAX_QUERY_ENTITLEMENT_TOKEN_IDS: Int = 32
        public const val MAX_CHECKOUT_ENTRIES: Int = 10
        public const val REDEEM_MAX_ENTITLEMENT_IDS: Int = 32
        public const val ENTITLEMENT_ID_MAX_LENGTH: Int = 32
        public const val CATALOG_ITEM_ID_MAX_LENGTH: Int = 32
        public const val CATALOG_OFFER_ID_MAX_LENGTH: Int = 32
        public const val TRANSACTION_ID_MAX_LENGTH: Int = 64
        public const val ENTITLEMENT_END_TIMESTAMP_UNDEFINED: Long = -1L
    }
}

/** Ownership status. */
public enum class EosOwnershipStatus(val value: Int) {
    NotOwned(0),
    Owned(1);

    public companion object {
        internal fun fromValue(v: Int): EosOwnershipStatus = entries.firstOrNull { it.value == v } ?: NotOwned
    }
}

/** Type of catalog item. */
public enum class EosEcomItemType(val value: Int) {
    Durable(0),
    Consumable(1),
    Other(2);

    public companion object {
        internal fun fromValue(v: Int): EosEcomItemType = entries.firstOrNull { it.value == v } ?: Other
    }
}

/** Orientation preference for the mobile checkout page. */
public enum class EosCheckoutOrientation(val value: Int) {
    Default(0),
    Portrait(1),
    Landscape(2);

    public companion object {
        internal fun fromValue(v: Int): EosCheckoutOrientation = entries.firstOrNull { it.value == v } ?: Default
    }
}

public class EcomEntitlement(
    public val entitlementName: String,
    public val entitlementId: String,
    public val catalogItemId: String,
    public val serverIndex: Int,
    public val redeemed: Boolean,
    public val endTimestamp: Long,
)

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

public class CheckoutEntry(public val offerId: String)

public class QueryOwnershipBySandboxIdsResult(
    public val result: EosResult,
    public val localUserId: EpicAccountId,
    public val sandboxIdCount: Int,
)

public class QueryOwnershipTokenResult(
    public val result: EosResult,
    public val localUserId: EpicAccountId,
    public val token: String,
)

public class QueryEntitlementTokenResult(
    public val result: EosResult,
    public val localUserId: EpicAccountId,
    public val token: String,
)

public class CheckoutResult(
    public val result: EosResult,
    public val localUserId: EpicAccountId,
    public val transactionId: String,
)

// region Struct writers

internal class EcomQueryOwnershipOptions(
    var localUserId: EpicAccountId,
    var catalogItemIds: List<String>,
    var sandboxIds: List<String>?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        val catalogArr = arena.allocCStringArray(catalogItemIds)
        seg.setInt64(16, catalogArr.address())
        seg.setInt32(24, catalogItemIds.size)
        seg.setInt32(28, 0) // bIncludeRedeemed, not used by QueryOwnership
        val sbIds = sandboxIds
        if (sbIds != null) {
            val sbArr = arena.allocCStringArray(sbIds)
            seg.setInt64(32, sbArr.address())
            seg.setInt32(40, sbIds.size)
        } else {
            seg.setInt64(32, 0L)
            seg.setInt32(40, 0)
        }
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

internal class EcomQueryOwnershipBySandboxIdsOptions(
    var localUserId: EpicAccountId,
    var sandboxIds: List<String>,
    var catalogItemIds: List<String>,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        val sbArr = arena.allocCStringArray(sandboxIds)
        seg.setInt64(16, sbArr.address())
        seg.setInt32(24, sandboxIds.size)
        val catalogArr = arena.allocCStringArray(catalogItemIds)
        seg.setInt64(32, catalogArr.address())
        seg.setInt32(40, catalogItemIds.size)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

internal class EcomQueryOwnershipTokenOptions(
    var localUserId: EpicAccountId,
    var catalogItemIds: List<String>,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        val arr = arena.allocCStringArray(catalogItemIds)
        seg.setInt64(16, arr.address())
        seg.setInt32(24, catalogItemIds.size)
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

internal class EcomQueryEntitlementsOptions(
    var localUserId: EpicAccountId?,
    var entitlementNames: List<String>?,
    var includeRedeemed: Boolean,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId?.raw ?: 0L)
        val en = entitlementNames
        if (en != null) {
            val arr = arena.allocCStringArray(en)
            seg.setInt64(16, arr.address())
            seg.setInt32(24, en.size)
        } else {
            seg.setInt64(16, 0L)
            seg.setInt32(24, 0)
        }
        seg.setInt32(28, if (includeRedeemed) 1 else 0)
        return seg
    }

    companion object {
        const val API_LATEST = 3
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
        )
    }
}

internal class EcomQueryEntitlementTokenOptions(
    var localUserId: EpicAccountId,
    var entitlementNames: List<String>?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        val en = entitlementNames
        if (en != null) {
            val arr = arena.allocCStringArray(en)
            seg.setInt64(16, arr.address())
            seg.setInt32(24, en.size)
        } else {
            seg.setInt64(16, 0L)
            seg.setInt32(24, 0)
        }
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

internal class EcomGetEntitlementsCountOptions(var localUserId: EpicAccountId) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG
        )
    }
}

internal class EcomGetEntitlementsByNameCountOptions(
    var localUserId: EpicAccountId,
    var name: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(name).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class EcomCopyEntitlementByIndexOptions(
    var localUserId: EpicAccountId,
    var index: Int,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt32(16, index)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT,
        )
    }
}

internal class EcomCopyEntitlementByNameAndIndexOptions(
    var localUserId: EpicAccountId,
    var name: String,
    var index: Int,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(name).address())
        seg.setInt32(24, index)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

internal class EcomCopyEntitlementByIdOptions(
    var localUserId: EpicAccountId,
    var id: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(id).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class EcomQueryOffersOptions(
    var localUserId: EpicAccountId?,
    var overrideCatalogNamespace: String?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId?.raw ?: 0L)
        seg.setInt64(16, arena.allocCString(overrideCatalogNamespace).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class EcomGetOfferCountOptions(var localUserId: EpicAccountId?) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId?.raw ?: 0L)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG
        )
    }
}

internal class EcomCopyOfferByIndexOptions(
    var localUserId: EpicAccountId?,
    var index: Int,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId?.raw ?: 0L)
        seg.setInt32(16, index)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT,
        )
    }
}

internal class EcomCopyOfferByIdOptions(
    var localUserId: EpicAccountId?,
    var offerId: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId?.raw ?: 0L)
        seg.setInt64(16, arena.allocCString(offerId).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class EcomCheckoutEntry(public val offerId: String) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(offerId).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS
        )
    }
}

internal class EcomCheckoutOptions(
    var localUserId: EpicAccountId,
    var entries: List<CheckoutEntry>,
    var preOrderPurchase: Boolean,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        val entrySegs = entries.map { EcomCheckoutEntry(it.offerId).writeTo(arena) }
        val arr = arena.allocate(EcomCheckoutEntry.LAYOUT, entrySegs.size.toLong())
        entrySegs.forEachIndexed { i, s ->
            val dst = arr.asSlice(i.toLong() * EcomCheckoutEntry.LAYOUT.byteSize(),
                EcomCheckoutEntry.LAYOUT.byteSize())
            MemorySegment.copy(s, 0, dst, 0, EcomCheckoutEntry.LAYOUT.byteSize())
        }
        seg.setInt64(16, arr.address())
        seg.setInt32(24, entrySegs.size)
        seg.setInt32(28, if (preOrderPurchase) 1 else 0)
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
        )
    }
}

internal class EcomRedeemEntitlementsOptions(
    var localUserId: EpicAccountId,
    var entitlementIds: List<String>,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        val arr = arena.allocCStringArray(entitlementIds)
        seg.setInt64(16, arr.address())
        seg.setInt32(24, entitlementIds.size)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

// endregion
