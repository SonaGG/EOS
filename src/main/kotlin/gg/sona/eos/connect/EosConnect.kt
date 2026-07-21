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

import gg.sona.eos.NotificationHandle
import gg.sona.eos.internal.setInt8
import gg.sona.eos.internal.setInt16
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import gg.sona.eos.internal.setFloat
import gg.sona.eos.internal.setDouble
import gg.sona.eos.internal.setBool
import gg.sona.eos.internal.getInt8
import gg.sona.eos.internal.getInt16
import gg.sona.eos.internal.getInt32
import gg.sona.eos.internal.getInt64
import gg.sona.eos.internal.getFloat
import gg.sona.eos.internal.getDouble
import gg.sona.eos.internal.getBool

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.common.ContinuanceToken
import gg.sona.eos.common.EosExternalCredentialType
import gg.sona.eos.common.EosLoginStatus
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.allocCStringArray
import gg.sona.eos.internal.allocHandleArray
import gg.sona.eos.internal.withCallArena
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * Connect interface for external account login and account mapping.
 */
public class EosConnect internal constructor(private val platform: EosPlatform) {

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
    public fun login(
        credentialType: EosExternalCredentialType,
        token: String,
        displayName: String? = null,
        nsaIdToken: String? = null,
    ): CompletableFuture<LoginResult> {
        val future = CompletableFuture<LoginResult>()
        val invoker = EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val localUserId = ProductUserId(data.getInt64(24))
            val ct = ContinuanceToken(data.getInt64(40))
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
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun logout(localUserId: ProductUserId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val invoker = EosCallback { data -> future.complete(EosResult.fromValue(data.getInt32(8))) }
        val handle = CallbackStubs.register(invoker)
        val options = ConnectLogoutOptions(localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Connect_Logout",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun createUser(continuanceToken: ContinuanceToken): CompletableFuture<LoginResult> {
        val future = CompletableFuture<LoginResult>()
        val invoker = EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val localUserId = ProductUserId(data.getInt64(24))
            future.complete(LoginResult(result, localUserId, ContinuanceToken(0L)))
        }
        val handle = CallbackStubs.register(invoker)
        val options = ConnectCreateUserOptions(continuanceToken)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Connect_CreateUser",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
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
    public fun linkAccount(
        localUserId: ProductUserId,
        continuanceToken: ContinuanceToken,
    ): CompletableFuture<LoginResult> {
        val future = CompletableFuture<LoginResult>()
        val invoker = EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val localUserId = ProductUserId(data.getInt64(24))
            future.complete(LoginResult(result, localUserId, ContinuanceToken(0L)))
        }
        val handle = CallbackStubs.register(invoker)
        val options = ConnectLinkAccountOptions(localUserId, continuanceToken)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Connect_LinkAccount",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun createDeviceId(): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val invoker = EosCallback { data -> future.complete(EosResult.fromValue(data.getInt32(8))) }
        val handle = CallbackStubs.register(invoker)
        val options = ConnectCreateDeviceIdOptions()
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Connect_CreateDeviceId",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun deleteDeviceId(): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val invoker = EosCallback { data -> future.complete(EosResult.fromValue(data.getInt32(8))) }
        val handle = CallbackStubs.register(invoker)
        val options = ConnectDeleteDeviceIdOptions()
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Connect_DeleteDeviceId",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun getLoggedInUsersCount(): Int {
        val fn = Native.downcall(
            "EOS_Connect_GetLoggedInUsersCount",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(handle()) as Int
    }

    public fun getLoggedInUserByIndex(index: Int): ProductUserId {
        val fn = Native.downcall(
            "EOS_Connect_GetLoggedInUserByIndex",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT)
        )
        return ProductUserId(fn.invokeExact(handle(), index) as Long)
    }

    public fun getLoginStatus(localUserId: ProductUserId): EosLoginStatus {
        val fn = Native.downcall(
            "EOS_Connect_GetLoginStatus",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return EosLoginStatus.fromValue(fn.invokeExact(handle(), localUserId.raw) as Int)
    }

    public fun addNotifyLoginStatusChanged(callback: (LoginStatusChangedInfo) -> Unit): NotificationHandle {
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(16))
            val prev = EosLoginStatus.fromValue(data.getInt32(24))
            val curr = EosLoginStatus.fromValue(data.getInt32(28))
            callback(LoginStatusChangedInfo(localUserId, prev, curr))
        }
        val handle = CallbackStubs.register(invoker)
        val options = ConnectAddNotifyLoginStatusChangedOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Connect_AddNotifyLoginStatusChanged",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyLoginStatusChanged(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_Connect_RemoveNotifyLoginStatusChanged",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }
}

/** Connect login result. */
public class LoginResult(
    public val result: EosResult,
    public val localUserId: ProductUserId,
    public val continuanceToken: ContinuanceToken = ContinuanceToken(0L),
)

/** Connect login status change info. */
public class LoginStatusChangedInfo(
    public val localUserId: ProductUserId,
    public val previousStatus: EosLoginStatus,
    public val currentStatus: EosLoginStatus,
)

internal class ConnectCredentials(
    var token: String,
    var type: EosExternalCredentialType,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(token).address())
        seg.setInt32(16, type.value)
        return seg
    }

    companion object {
        // EOS_CONNECT_CREDENTIALS_API_LATEST
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}

internal class ConnectUserLoginInfo(
    var displayName: String?,
    var nsaIdToken: String?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(displayName).address())
        seg.setInt64(16, arena.allocCString(nsaIdToken).address())
        return seg
    }

    companion object {
        // EOS_CONNECT_USERLOGININFO_API_LATEST
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
        )
    }
}

internal class ConnectLoginOptions(
    var credentials: ConnectCredentials,
    var userLoginInfo: ConnectUserLoginInfo?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        val credentialsSeg = credentials.writeTo(arena)
        val userLoginInfoAddress = userLoginInfo?.writeTo(arena)?.address() ?: 0L
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, credentialsSeg.address())
        seg.setInt64(16, userLoginInfoAddress)
        return seg
    }

    companion object {
        // EOS_CONNECT_LOGIN_API_LATEST
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
        )
    }
}

internal class ConnectLogoutOptions(var localUserId: ProductUserId) : StructWriter {
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

internal class ConnectCreateUserOptions(var continuanceToken: ContinuanceToken) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, continuanceToken.raw)
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

internal class ConnectLinkAccountOptions(
    var localUserId: ProductUserId,
    var continuanceToken: ContinuanceToken,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, continuanceToken.raw)
        return seg
    }

    companion object {
        // EOS_CONNECT_LINKACCOUNT_API_LATEST
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}

internal class ConnectCreateDeviceIdOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}

internal class ConnectDeleteDeviceIdOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}

internal class ConnectAddNotifyLoginStatusChangedOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}
