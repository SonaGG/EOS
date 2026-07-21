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
package gg.sona.eos.integratedplatform

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

/**
 * Integrated Platform interface. Used to bridge EOS to a native platform
 * (Steam, PSN, Xbox Live, etc.) so EOS can route friend/inventory/identity
 * calls appropriately.
 */
public class EosIntegratedPlatform internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetIntegratedPlatformInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /**
     * Tell EOS about a user's login status on a third-party platform.
     * The actual login is performed by your integration code.
     */
    public fun setUserLoginStatus(
        localUserId: ProductUserId,
        integratedPlatform: String,
        loginStatus: EosIntegratedPlatformLoginStatus,
    ): EosResult = withCallArena { arena ->
        val options = IntegratedPlatformSetUserLoginStatusOptions(
            integratedPlatform, localUserId, loginStatus
        )
        EosResult.fromValue(
            Native.invoke(
                "EOS_IntegratedPlatform_SetUserLoginStatus",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun addNotifyUserLoginStatusChanged(
        callback: (UserLoginStatusChangedInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(16))
            val previous = readString(data, 24)
            val current = readString(data, 32)
            val previousStatus = EosIntegratedPlatformLoginStatus.fromValue(data.getInt32(40))
            val currentStatus = EosIntegratedPlatformLoginStatus.fromValue(data.getInt32(44))
            callback(UserLoginStatusChangedInfo(localUserId, previous, current, previousStatus, currentStatus))
        }
        val handle = CallbackStubs.register(invoker)
        val options = IntegratedPlatformAddNotifyUserLoginStatusChangedOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_IntegratedPlatform_AddNotifyUserLoginStatusChanged",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyUserLoginStatusChanged(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_IntegratedPlatform_RemoveNotifyUserLoginStatusChanged",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    /**
     * Register a callback to be invoked before a user is logged out. This
     * allows the integration to clean up before the user is gone.
     */
    public fun setUserPreLogoutCallback(
        callback: (UserPreLogoutInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(16))
            val platform = readString(data, 24)
            callback(UserPreLogoutInfo(localUserId, platform))
        }
        val handle = CallbackStubs.register(invoker)
        val options = IntegratedPlatformSetUserPreLogoutCallbackOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_IntegratedPlatform_SetUserPreLogoutCallback",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun clearUserPreLogoutCallback(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_IntegratedPlatform_ClearUserPreLogoutCallback",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun finalizeDeferredUserLogout(localUserId: ProductUserId): EosResult =
        withCallArena { arena ->
            val options = IntegratedPlatformFinalizeDeferredUserLogoutOptions(localUserId)
            EosResult.fromValue(
                Native.invoke(
                    "EOS_IntegratedPlatform_FinalizeDeferredUserLogout",
                    listOf(handle(), options.writeTo(arena)),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
        }

    private fun readString(seg: MemorySegment, offset: Long): String {
        val ptr = seg.get(ValueLayout.ADDRESS, offset)
        if (ptr.address() == 0L) return ""
        return ptr.reinterpret(Long.MAX_VALUE).getString(0)
    }
}

/** Login status on an integrated platform. */
public enum class EosIntegratedPlatformLoginStatus(val value: Int) {
    NotLoggedIn(0),
    LoggedIn(1);

    public companion object {
        internal fun fromValue(v: Int): EosIntegratedPlatformLoginStatus =
            entries.firstOrNull { it.value == v } ?: NotLoggedIn
    }
}

public class UserLoginStatusChangedInfo(
    public val localUserId: ProductUserId,
    public val previousPlatform: String,
    public val currentPlatform: String,
    public val previousStatus: EosIntegratedPlatformLoginStatus,
    public val currentStatus: EosIntegratedPlatformLoginStatus,
)

public class UserPreLogoutInfo(
    public val localUserId: ProductUserId,
    public val platform: String,
)

internal class IntegratedPlatformSetUserLoginStatusOptions(
    var integratedPlatform: String,
    var localUserId: ProductUserId,
    var loginStatus: EosIntegratedPlatformLoginStatus,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(integratedPlatform).address())
        seg.setInt64(16, localUserId.raw)
        seg.setInt32(24, loginStatus.value)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT,
        )
    }
}

internal class IntegratedPlatformAddNotifyUserLoginStatusChangedOptions : StructWriter {
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

internal class IntegratedPlatformSetUserPreLogoutCallbackOptions : StructWriter {
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

internal class IntegratedPlatformFinalizeDeferredUserLogoutOptions(
    var localUserId: ProductUserId,
) : StructWriter {
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
