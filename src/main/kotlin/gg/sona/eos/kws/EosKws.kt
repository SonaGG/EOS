/*
 * Copyright 2024 sona
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
package gg.sona.eos.kws

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.NotificationHandle
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
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
 * Kids Web Services interface (KWS). Wraps the SuperAwesome age-gate
 * integration.
 */
public class EosKws internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetKWSInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /**
     * Query the client's country and age permissions based on their IP.
     * Use this to decide whether to display the age gate.
     */
    public fun queryAgeGate(): CompletableFuture<QueryAgeGateResult> {
        val future = CompletableFuture<QueryAgeGateResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val countryCode = data.getInt64(24).let { addr ->
                if (addr == 0L) "" else
                    MemorySegment.ofAddress(addr).reinterpret(Long.MAX_VALUE).getString(0)
            }
            val ageOfConsent = data.getInt32(32).toLong() and 0xffffffffL
            future.complete(QueryAgeGateResult(result, countryCode, ageOfConsent.toInt()))
        })
        val options = KwsQueryAgeGateOptions()
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_KWS_QueryAgeGate",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /**
     * Create a KWS user with date of birth and parent email. The user is
     * associated with the supplied [ProductUserId].
     */
    public fun createUser(
        localUserId: ProductUserId,
        dateOfBirth: String, // ISO 8601 YYYY-MM-DD
        parentEmail: String,
    ): CompletableFuture<CreateUserResult> {
        val future = CompletableFuture<CreateUserResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val localUserId = ProductUserId(data.getInt64(24))
            val kwsUserId = data.getInt64(32).let { addr ->
                if (addr == 0L) "" else
                    MemorySegment.ofAddress(addr).reinterpret(Long.MAX_VALUE).getString(0)
            }
            val isMinor = data.getInt32(40) != 0
            future.complete(CreateUserResult(result, localUserId, kwsUserId, isMinor))
        })
        val options = KwsCreateUserOptions(localUserId, dateOfBirth, parentEmail)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_KWS_CreateUser",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /** Query the current permission state for a user. */
    public fun queryPermissions(localUserId: ProductUserId): CompletableFuture<QueryPermissionsResult> {
        val future = CompletableFuture<QueryPermissionsResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val localUserId = ProductUserId(data.getInt64(24))
            val kwsUserId = data.getInt64(32).let { addr ->
                if (addr == 0L) "" else
                    MemorySegment.ofAddress(addr).reinterpret(Long.MAX_VALUE).getString(0)
            }
            val dateOfBirth = data.getInt64(40).let { addr ->
                if (addr == 0L) "" else
                    MemorySegment.ofAddress(addr).reinterpret(Long.MAX_VALUE).getString(0)
            }
            val isMinor = data.getInt32(48) != 0
            val parentEmail = data.getInt64(56).let { addr ->
                if (addr == 0L) "" else
                    MemorySegment.ofAddress(addr).reinterpret(Long.MAX_VALUE).getString(0)
            }
            future.complete(
                QueryPermissionsResult(result, localUserId, kwsUserId, dateOfBirth, isMinor, parentEmail)
            )
        })
        val options = KwsQueryPermissionsOptions(localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_KWS_QueryPermissions",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /** Update the parent email for a user. */
    public fun updateParentEmail(
        localUserId: ProductUserId,
        parentEmail: String,
    ): CompletableFuture<ProductUserId> {
        val future = CompletableFuture<ProductUserId>()
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val localUserId = ProductUserId(data.getInt64(24))
            if (result == EosResult.Success) future.complete(localUserId) else future.completeExceptionally(
                gg.sona.eos.EosException(result)
            )
        })
        val options = KwsUpdateParentEmailOptions(localUserId, parentEmail)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_KWS_UpdateParentEmail",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /**
     * Request new permissions for a user. Each [permissionKeys] entry must
     * match a permission configured with KWS. Up to 16 keys may be requested.
     */
    public fun requestPermissions(
        localUserId: ProductUserId,
        permissionKeys: List<String>,
    ): CompletableFuture<ProductUserId> {
        require(permissionKeys.size <= MAX_PERMISSIONS) {
            "cannot request more than $MAX_PERMISSIONS permissions at once"
        }
        val future = CompletableFuture<ProductUserId>()
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val localUserId = ProductUserId(data.getInt64(24))
            if (result == EosResult.Success) future.complete(localUserId) else future.completeExceptionally(
                gg.sona.eos.EosException(result)
            )
        })
        val options = KwsRequestPermissionsOptions(localUserId, permissionKeys)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_KWS_RequestPermissions",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /** Get the number of cached permissions for a user. */
    public fun getPermissionsCount(localUserId: ProductUserId): Int {
        val options = KwsGetPermissionsCountOptions(localUserId)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_KWS_GetPermissionsCount",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        }
    }

    /**
     * Copy a cached permission by index. The returned [KwsPermissionStatus]
     * owns a C-side allocation that is freed before the function returns;
     * only the Kotlin view is exposed.
     */
    public fun copyPermissionByIndex(
        localUserId: ProductUserId,
        index: Int,
    ): KwsPermissionStatus? = withCallArena { arena ->
        val options = KwsCopyPermissionByIndexOptions(localUserId, index)
        val outPtr = arena.allocate(ValueLayout.ADDRESS)
        val result = EosResult.fromValue(
            Native.invoke(
                "EOS_KWS_CopyPermissionByIndex",
                listOf(handle(), options.writeTo(arena), outPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        if (result != EosResult.Success) return@withCallArena null
        val seg = outPtr.get(ValueLayout.ADDRESS, 0)
        if (seg.address() == 0L) return@withCallArena null
        val name = seg.getInt64(8).let { addr ->
            if (addr == 0L) "" else
                MemorySegment.ofAddress(addr).reinterpret(Long.MAX_VALUE).getString(0)
        }
        val status = EosKwsPermissionStatus.fromValue(seg.getInt32(16))
        val releaseFn = Native.downcall(
            "EOS_KWS_PermissionStatus_Release",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        )
        releaseFn.invokeExact(seg)
        KwsPermissionStatus(name, status)
    }

    /**
     * Look up a cached permission by name. Returns null if the permission
     * is unknown for the user.
     */
    public fun getPermissionByKey(
        localUserId: ProductUserId,
        key: String,
    ): EosKwsPermissionStatus? = withCallArena { arena ->
        val options = KwsGetPermissionByKeyOptions(localUserId, key)
        val outPtr = arena.allocate(ValueLayout.JAVA_INT)
        val result = EosResult.fromValue(
            Native.invoke(
                "EOS_KWS_GetPermissionByKey",
                listOf(handle(), options.writeTo(arena), outPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        if (result != EosResult.Success) null else EosKwsPermissionStatus.fromValue(outPtr.get(ValueLayout.JAVA_INT, 0))
    }

    /**
     * Register for notifications when KWS permissions change for any
     * logged-in local user. The returned [NotificationHandle] must be
     * passed to [removeNotifyPermissionsUpdateReceived] when no longer
     * needed.
     */
    public fun addNotifyPermissionsUpdateReceived(
        callback: (PermissionsUpdateInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(16))
            val kwsUserId = data.getInt64(24).let { addr ->
                if (addr == 0L) "" else
                    MemorySegment.ofAddress(addr).reinterpret(Long.MAX_VALUE).getString(0)
            }
            val dateOfBirth = data.getInt64(32).let { addr ->
                if (addr == 0L) "" else
                    MemorySegment.ofAddress(addr).reinterpret(Long.MAX_VALUE).getString(0)
            }
            val isMinor = data.getInt32(40) != 0
            val parentEmail = data.getInt64(48).let { addr ->
                if (addr == 0L) "" else
                    MemorySegment.ofAddress(addr).reinterpret(Long.MAX_VALUE).getString(0)
            }
            callback(PermissionsUpdateInfo(localUserId, kwsUserId, dateOfBirth, isMinor, parentEmail))
        }
        val handle = CallbackStubs.register(invoker)
        val options = KwsAddNotifyPermissionsUpdateReceivedOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_KWS_AddNotifyPermissionsUpdateReceived",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyPermissionsUpdateReceived(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_KWS_RemoveNotifyPermissionsUpdateReceived",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public companion object {
        public const val MAX_PERMISSIONS: Int = 16
        public const val MAX_PERMISSION_LENGTH: Int = 32
    }
}

/** Status of a KWS permission grant. */
public enum class EosKwsPermissionStatus(val value: Int) {
    Granted(0),
    Rejected(1),
    Pending(2);

    public companion object {
        internal fun fromValue(v: Int): EosKwsPermissionStatus =
            entries.firstOrNull { it.value == v } ?: Granted
    }
}

public class KwsPermissionStatus(public val name: String, public val status: EosKwsPermissionStatus)

public class QueryAgeGateResult(
    public val result: EosResult,
    public val countryCode: String,
    public val ageOfConsent: Int,
)

public class CreateUserResult(
    public val result: EosResult,
    public val localUserId: ProductUserId,
    public val kwsUserId: String,
    public val isMinor: Boolean,
)

public class QueryPermissionsResult(
    public val result: EosResult,
    public val localUserId: ProductUserId,
    public val kwsUserId: String,
    public val dateOfBirth: String,
    public val isMinor: Boolean,
    public val parentEmail: String,
)

public class PermissionsUpdateInfo(
    public val localUserId: ProductUserId,
    public val kwsUserId: String,
    public val dateOfBirth: String,
    public val isMinor: Boolean,
    public val parentEmail: String,
)

// region Struct writers

internal class KwsQueryAgeGateOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4)
        )
    }
}

internal class KwsCreateUserOptions(
    var localUserId: ProductUserId,
    var dateOfBirth: String,
    var parentEmail: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(dateOfBirth).address())
        seg.setInt64(24, arena.allocCString(parentEmail).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
        )
    }
}

internal class KwsQueryPermissionsOptions(var localUserId: ProductUserId) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
        )
    }
}

internal class KwsUpdateParentEmailOptions(
    var localUserId: ProductUserId,
    var parentEmail: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(parentEmail).address())
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

internal class KwsRequestPermissionsOptions(
    var localUserId: ProductUserId,
    var permissionKeys: List<String>,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt32(16, permissionKeys.size)
        val arr = arena.allocate(ValueLayout.ADDRESS, permissionKeys.size.toLong())
        permissionKeys.forEachIndexed { i, k ->
            arr.setAtIndex(ValueLayout.ADDRESS, i.toLong(), arena.allocCString(k))
        }
        seg.setInt64(24, arr.address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
        )
    }
}

internal class KwsGetPermissionsCountOptions(var localUserId: ProductUserId) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
        )
    }
}

internal class KwsCopyPermissionByIndexOptions(
    var localUserId: ProductUserId,
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

internal class KwsGetPermissionByKeyOptions(
    var localUserId: ProductUserId,
    var key: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(key).address())
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

internal class KwsAddNotifyPermissionsUpdateReceivedOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4)
        )
    }
}

// endregion
