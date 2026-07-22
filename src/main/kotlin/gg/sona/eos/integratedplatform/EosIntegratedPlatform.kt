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
package gg.sona.eos.integratedplatform

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.NotificationHandle
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.*
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * Integrated Platform interface. Used to bridge EOS to a native platform
 * (Steam, PSN, Xbox Live, etc.) so EOS can route friend/inventory/identity
 * calls appropriately.
 */
class EosIntegratedPlatform internal constructor(private val platform: EosPlatform) {

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
    fun setUserLoginStatus(
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

    fun addNotifyUserLoginStatusChanged(
        callback: (UserLoginStatusChangedInfo) -> Unit,
    ): NotificationHandle {
        // EOS_IntegratedPlatform_UserLoginStatusChangedCallbackInfo: ClientData@0, PlatformType@8,
        // LocalPlatformUserId@16, AccountId@24, ProductUserId@32, PreviousLoginStatus@40, CurrentLoginStatus@44
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(32))
            val previous = readString(data, 16)
            // NOTE: struct only has a single LocalPlatformUserId string field (no separate
            // "current" string); there is no confidently-matching field/offset for this second
            // read, so it is left unchanged.
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
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    fun removeNotifyUserLoginStatusChanged(handle: NotificationHandle) {
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
    fun setUserPreLogoutCallback(
        callback: (UserPreLogoutInfo) -> Unit,
    ): NotificationHandle {
        // EOS_IntegratedPlatform_UserPreLogoutCallbackInfo: ClientData@0, PlatformType@8,
        // LocalPlatformUserId@16, AccountId@24, ProductUserId@32
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(32))
            val platform = readString(data, 16)
            callback(UserPreLogoutInfo(localUserId, platform))
        }
        val handle = CallbackStubs.register(invoker)
        val options = IntegratedPlatformSetUserPreLogoutCallbackOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_IntegratedPlatform_SetUserPreLogoutCallback",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    fun clearUserPreLogoutCallback(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_IntegratedPlatform_ClearUserPreLogoutCallback",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    fun finalizeDeferredUserLogout(localUserId: ProductUserId): EosResult =
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