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
package gg.sona.eos.userinfo

import gg.sona.eos.NotificationHandle
import gg.sona.eos.internal.setInt8
import gg.sona.eos.internal.setInt16
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
import gg.sona.eos.common.EosExternalAccountType
import gg.sona.eos.common.EosOnlinePlatform
import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import gg.sona.eos.internal.withCallArena
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/** UserInfo interface for fetching user information such as display name. */
public class EosUserInfo internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetUserInfoInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    public fun queryUserInfo(localUserId: EpicAccountId, targetUserId: EpicAccountId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_UserInfo_QueryUserInfoCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16, TargetUserId@24
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = UserInfoQueryUserInfoOptions(localUserId, targetUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_UserInfo_QueryUserInfo",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun queryUserInfoByDisplayName(localUserId: EpicAccountId, displayName: String): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_UserInfo_QueryUserInfoByDisplayNameCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16, TargetUserId@24, DisplayName@32
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = UserInfoQueryUserInfoByDisplayNameOptions(localUserId, displayName)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_UserInfo_QueryUserInfoByDisplayName",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun queryUserInfoByExternalAccount(
        localUserId: EpicAccountId,
        externalAccountType: EosExternalAccountType,
        externalAccountId: String,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_UserInfo_QueryUserInfoByExternalAccountCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16, ExternalAccountId@24, AccountType@32, TargetUserId@40
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = UserInfoQueryUserInfoByExternalAccountOptions(localUserId, externalAccountType, externalAccountId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_UserInfo_QueryUserInfoByExternalAccount",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun getLocalPlatformType(): EosOnlinePlatform = withCallArena { arena ->
        val options = UserInfoGetLocalPlatformTypeOptions()
        EosOnlinePlatform.fromValue(
            Native.invoke(
                "EOS_UserInfo_GetLocalPlatformType",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }
}

/** `EOS_UserInfo_GetLocalPlatformTypeOptions`: ApiVersion@0. */
internal class UserInfoGetLocalPlatformTypeOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        return seg
    }

    companion object {
        // EOS_USERINFO_GETLOCALPLATFORMTYPE_API_LATEST
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}

internal class UserInfoQueryUserInfoOptions(
    var localUserId: EpicAccountId,
    var targetUserId: EpicAccountId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, targetUserId.raw)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}

internal class UserInfoQueryUserInfoByDisplayNameOptions(
    var localUserId: EpicAccountId,
    var displayName: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(displayName).address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class UserInfoQueryUserInfoByExternalAccountOptions(
    var localUserId: EpicAccountId,
    var externalAccountType: EosExternalAccountType,
    var externalAccountId: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt32(16, externalAccountType.ordinal)
        seg.setInt64(24, arena.allocCString(externalAccountId).address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
        )
    }
}
