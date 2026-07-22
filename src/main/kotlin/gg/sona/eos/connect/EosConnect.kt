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
package gg.sona.eos.connect

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.NotificationHandle
import gg.sona.eos.common.ContinuanceToken
import gg.sona.eos.common.EosExternalCredentialType
import gg.sona.eos.common.EosLoginStatus
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.*
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * Connect interface for external account login and account mapping.
 */
class EosConnect internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetConnectInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /**
     * Log in using the credentials of an external account provider (Steam,
     * PSN, Apple, etc.).
     *
     * [displayName] and [nsaIdToken] populate the optional `UserLoginInfo`
     * struct; [displayName] is required for Amazon, Apple, Google, Nintendo,
     * Oculus, and Device ID logins, and [nsaIdToken] is required on Nintendo
     * Switch for any credential type other than [EosExternalCredentialType.NintendoNsaIdToken].
     */
    fun login(
        credentialType: EosExternalCredentialType,
        token: String,
        displayName: String? = null,
        nsaIdToken: String? = null,
    ): CompletableFuture<LoginResult> {
        val future = CompletableFuture<LoginResult>()
        val invoker = EosCallback { data ->
            // EOS_Connect_LoginCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16, ContinuanceToken@24
            val result = EosResult.fromValue(data.getInt32(0))
            val localUserId = ProductUserId(data.getInt64(16))
            val ct = ContinuanceToken(data.getInt64(24))
            future.complete(LoginResult(result, localUserId, ct))
        }
        val handle = CallbackStubs.register(invoker)
        val options = ConnectLoginOptions(
            credentials = ConnectCredentials(token, credentialType),
            userLoginInfo = if (displayName != null || nsaIdToken != null) {
                ConnectUserLoginInfo(displayName, nsaIdToken)
            } else {
                null
            },
        )
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Connect_Login",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun logout(localUserId: ProductUserId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Connect_LogoutCallbackInfo: ResultCode@0
        val invoker = EosCallback { data -> future.complete(EosResult.fromValue(data.getInt32(0))) }
        val handle = CallbackStubs.register(invoker)
        val options = ConnectLogoutOptions(localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Connect_Logout",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun createUser(continuanceToken: ContinuanceToken): CompletableFuture<LoginResult> {
        val future = CompletableFuture<LoginResult>()
        val invoker = EosCallback { data ->
            // EOS_Connect_CreateUserCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16
            val result = EosResult.fromValue(data.getInt32(0))
            val localUserId = ProductUserId(data.getInt64(16))
            future.complete(LoginResult(result, localUserId, ContinuanceToken(0L)))
        }
        val handle = CallbackStubs.register(invoker)
        val options = ConnectCreateUserOptions(continuanceToken)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Connect_CreateUser",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /**
     * Link an external account to the current logged-in product user. Use
     * after a Login returned [EosResult.InvalidUser] - the returned continuance
     * token can be used with createUser OR (if a product user is already
     * logged in) with linkAccount.
     */
    fun linkAccount(
        localUserId: ProductUserId,
        continuanceToken: ContinuanceToken,
    ): CompletableFuture<LoginResult> {
        val future = CompletableFuture<LoginResult>()
        val invoker = EosCallback { data ->
            // EOS_Connect_LinkAccountCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16
            val result = EosResult.fromValue(data.getInt32(0))
            val localUserId = ProductUserId(data.getInt64(16))
            future.complete(LoginResult(result, localUserId, ContinuanceToken(0L)))
        }
        val handle = CallbackStubs.register(invoker)
        val options = ConnectLinkAccountOptions(localUserId, continuanceToken)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Connect_LinkAccount",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun createDeviceId(): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Connect_CreateDeviceIdCallbackInfo: ResultCode@0
        val invoker = EosCallback { data -> future.complete(EosResult.fromValue(data.getInt32(0))) }
        val handle = CallbackStubs.register(invoker)
        val options = ConnectCreateDeviceIdOptions()
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Connect_CreateDeviceId",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun deleteDeviceId(): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Connect_DeleteDeviceIdCallbackInfo: ResultCode@0
        val invoker = EosCallback { data -> future.complete(EosResult.fromValue(data.getInt32(0))) }
        val handle = CallbackStubs.register(invoker)
        val options = ConnectDeleteDeviceIdOptions()
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Connect_DeleteDeviceId",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun getLoggedInUsersCount(): Int {
        val fn = Native.downcall(
            "EOS_Connect_GetLoggedInUsersCount",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(handle()) as Int
    }

    fun getLoggedInUserByIndex(index: Int): ProductUserId {
        val fn = Native.downcall(
            "EOS_Connect_GetLoggedInUserByIndex",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT)
        )
        return ProductUserId(fn.invokeExact(handle(), index) as Long)
    }

    fun getLoginStatus(localUserId: ProductUserId): EosLoginStatus {
        val fn = Native.downcall(
            "EOS_Connect_GetLoginStatus",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return EosLoginStatus.fromValue(fn.invokeExact(handle(), localUserId.raw) as Int)
    }

    fun addNotifyLoginStatusChanged(callback: (LoginStatusChangedInfo) -> Unit): NotificationHandle {
        val invoker = EosCallback { data ->
            // EOS_Connect_LoginStatusChangedCallbackInfo: ClientData@0, LocalUserId@8, PreviousStatus@16, CurrentStatus@20
            val localUserId = ProductUserId(data.getInt64(8))
            val prev = EosLoginStatus.fromValue(data.getInt32(16))
            val curr = EosLoginStatus.fromValue(data.getInt32(20))
            callback(LoginStatusChangedInfo(localUserId, prev, curr))
        }
        val handle = CallbackStubs.register(invoker)
        val options = ConnectAddNotifyLoginStatusChangedOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Connect_AddNotifyLoginStatusChanged",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    fun removeNotifyLoginStatusChanged(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_Connect_RemoveNotifyLoginStatusChanged",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }
}