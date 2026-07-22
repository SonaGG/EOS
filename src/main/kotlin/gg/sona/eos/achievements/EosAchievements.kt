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
package gg.sona.eos.achievements

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.NotificationHandle
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.getInt32
import gg.sona.eos.internal.getInt64
import gg.sona.eos.internal.withCallArena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * Achievements interface.
 */
public class EosAchievements internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetAchievementsInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    public fun queryDefinitions(
        localUserId: ProductUserId? = null,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Achievements_OnQueryDefinitionsCompleteCallbackInfo: ResultCode@0, ClientData@8
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = AchievementsQueryDefinitionsOptions(localUserId ?: ProductUserId.Invalid)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Achievements_QueryDefinitions",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun getAchievementDefinitionCount(): Int {
        val options = AchievementsGetAchievementDefinitionCountOptions()
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Achievements_GetAchievementDefinitionCount",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        }
    }

    public fun copyAchievementDefinitionByIndex(index: Int): AchievementDefinition? =
        withCallArena { arena ->
            val options = AchievementsCopyAchievementDefinitionByIndexOptions(index)
            val outPtr = arena.allocate(ValueLayout.ADDRESS)
            val result = EosResult.fromValue(
                Native.invoke(
                    "EOS_Achievements_CopyAchievementDefinitionV2ByIndex",
                    listOf(handle(), options.writeTo(arena), outPtr),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (result != EosResult.Success) return@withCallArena null
            val seg = outPtr.get(ValueLayout.ADDRESS, 0)
            if (seg.address() == 0L) return@withCallArena null
            val def = readDefinition(seg)
            Native.downcall(
                "EOS_Achievements_DefinitionV2_Release",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            ).invokeExact(seg)
            def
        }

    public fun copyAchievementDefinitionById(id: String): AchievementDefinition? = withCallArena { arena ->
        val options = AchievementsCopyAchievementDefinitionByIdOptions(id)
        val outPtr = arena.allocate(ValueLayout.ADDRESS)
        val result = EosResult.fromValue(
            Native.invoke(
                "EOS_Achievements_CopyAchievementDefinitionV2ByAchievementId",
                listOf(handle(), options.writeTo(arena), outPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        if (result != EosResult.Success) return@withCallArena null
        val seg = outPtr.get(ValueLayout.ADDRESS, 0)
        if (seg.address() == 0L) return@withCallArena null
        val def = readDefinition(seg)
        Native.downcall(
            "EOS_Achievements_DefinitionV2_Release",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        ).invokeExact(seg)
        def
    }

    public fun queryPlayerAchievements(
        targetUserId: ProductUserId,
        localUserId: ProductUserId? = null,
    ): CompletableFuture<QueryPlayerAchievementsResult> {
        val future = CompletableFuture<QueryPlayerAchievementsResult>()
        // EOS_Achievements_OnQueryPlayerAchievementsCompleteCallbackInfo: ResultCode@0, ClientData@8, TargetUserId@16, LocalUserId@24
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(0))
            val targetUserId = ProductUserId(data.getInt64(16))
            val localUserId = ProductUserId(data.getInt64(24))
            future.complete(QueryPlayerAchievementsResult(result, targetUserId, localUserId))
        })
        val options = AchievementsQueryPlayerAchievementsOptions(targetUserId, localUserId ?: ProductUserId.Invalid)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Achievements_QueryPlayerAchievements",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun getPlayerAchievementCount(userId: ProductUserId): Int {
        val options = AchievementsGetPlayerAchievementCountOptions(userId)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Achievements_GetPlayerAchievementCount",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        }
    }

    public fun copyPlayerAchievementByIndex(
        targetUserId: ProductUserId,
        index: Int,
        localUserId: ProductUserId? = null,
    ): PlayerAchievement? = withCallArena { arena ->
        val options = AchievementsCopyPlayerAchievementByIndexOptions(
            targetUserId, index, localUserId ?: ProductUserId.Invalid
        )
        val outPtr = arena.allocate(ValueLayout.ADDRESS)
        val result = EosResult.fromValue(
            Native.invoke(
                "EOS_Achievements_CopyPlayerAchievementByIndex",
                listOf(handle(), options.writeTo(arena), outPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        if (result != EosResult.Success) return@withCallArena null
        val seg = outPtr.get(ValueLayout.ADDRESS, 0)
        if (seg.address() == 0L) return@withCallArena null
        val pa = readPlayerAchievement(seg)
        Native.downcall(
            "EOS_Achievements_PlayerAchievement_Release",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        ).invokeExact(seg)
        pa
    }

    public fun copyPlayerAchievementByAchievementId(
        targetUserId: ProductUserId,
        achievementId: String,
        localUserId: ProductUserId? = null,
    ): PlayerAchievement? = withCallArena { arena ->
        val options = AchievementsCopyPlayerAchievementByAchievementIdOptions(
            targetUserId, achievementId, localUserId ?: ProductUserId.Invalid
        )
        val outPtr = arena.allocate(ValueLayout.ADDRESS)
        val result = EosResult.fromValue(
            Native.invoke(
                "EOS_Achievements_CopyPlayerAchievementByAchievementId",
                listOf(handle(), options.writeTo(arena), outPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        if (result != EosResult.Success) return@withCallArena null
        val seg = outPtr.get(ValueLayout.ADDRESS, 0)
        if (seg.address() == 0L) return@withCallArena null
        val pa = readPlayerAchievement(seg)
        Native.downcall(
            "EOS_Achievements_PlayerAchievement_Release",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        ).invokeExact(seg)
        pa
    }

    /** Unlock one or more achievements. */
    public fun unlockAchievements(
        userId: ProductUserId,
        achievementIds: List<String>,
    ): CompletableFuture<UnlockAchievementsResult> {
        val future = CompletableFuture<UnlockAchievementsResult>()
        // EOS_Achievements_OnUnlockAchievementsCompleteCallbackInfo: ResultCode@0, ClientData@8, UserId@16, AchievementsCount@24
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(0))
            val userId = ProductUserId(data.getInt64(16))
            val count = data.getInt32(24).toLong() and 0xffffffffL
            future.complete(UnlockAchievementsResult(result, userId, count.toInt()))
        })
        val options = AchievementsUnlockAchievementsOptions(userId, achievementIds)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Achievements_UnlockAchievements",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /**
     * Register a callback for achievement-unlocked notifications. The
     * returned [NotificationHandle] must be passed to
     * [removeNotifyAchievementsUnlocked] when no longer needed.
     */
    public fun addNotifyAchievementsUnlocked(
        callback: (AchievementUnlockedInfo) -> Unit,
    ): NotificationHandle {
        // EOS_Achievements_OnAchievementsUnlockedCallbackV2Info: ClientData@0, UserId@8, AchievementId@16, UnlockTime@24
        val invoker = EosCallback { data ->
            val userId = ProductUserId(data.getInt64(8))
            val achievementId = data.getInt64(16).let { addr ->
                if (addr == 0L) "" else
                    MemorySegment.ofAddress(addr).reinterpret(Long.MAX_VALUE).getString(0)
            }
            val unlockTime = data.getInt64(24)
            callback(AchievementUnlockedInfo(userId, achievementId, unlockTime))
        }
        val handle = CallbackStubs.register(invoker)
        val options = AchievementsAddNotifyAchievementsUnlockedV2Options()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Achievements_AddNotifyAchievementsUnlockedV2",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyAchievementsUnlocked(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_Achievements_RemoveNotifyAchievementsUnlocked",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    private fun readString(seg: MemorySegment, offset: Long): String {
        val ptr = seg.get(ValueLayout.ADDRESS, offset)
        if (ptr.address() == 0L) return ""
        return ptr.reinterpret(Long.MAX_VALUE).getString(0)
    }

    private fun readDefinition(seg: MemorySegment): AchievementDefinition {
        // Field offsets in EOS_Achievements_DefinitionV2 v2:
        //   0  ApiVersion (int32)
        //   4  padding
        //   8  AchievementId (char*)
        //   16 UnlockedDisplayName
        //   24 UnlockedDescription
        //   32 LockedDisplayName
        //   40 LockedDescription
        //   48 FlavorText
        //   56 UnlockedIconURL
        //   64 LockedIconURL
        //   72 bIsHidden (int32) - aligned to 8
        //   76 padding
        //   80 StatThresholdsCount (uint32)
        //   84 padding
        //   88 StatThresholds (array*)
        val statThresholdsCount = seg.getInt32(80).toLong() and 0xffffffffL
        val thresholdsPtr = seg.get(ValueLayout.ADDRESS, 88)
        val thresholds = if (thresholdsPtr.address() == 0L || statThresholdsCount == 0L) emptyList()
        else {
            val list = mutableListOf<StatThreshold>()
            for (i in 0 until statThresholdsCount.toInt()) {
                val item = MemorySegment.ofAddress(thresholdsPtr.address())
                    .asSlice(i.toLong() * StatThreshold.LAYOUT.byteSize(), StatThreshold.LAYOUT.byteSize())
                list.add(StatThreshold(
                    name = readString(item, 8),
                    threshold = item.getInt32(16),
                ))
            }
            list
        }
        return AchievementDefinition(
            id = readString(seg, 8),
            unlockedDisplayName = readString(seg, 16),
            unlockedDescription = readString(seg, 24),
            lockedDisplayName = readString(seg, 32),
            lockedDescription = readString(seg, 40),
            flavorText = readString(seg, 48),
            unlockedIconUrl = readString(seg, 56),
            lockedIconUrl = readString(seg, 64),
            isHidden = seg.getInt32(72) != 0,
            statThresholds = thresholds,
        )
    }

    private fun readPlayerAchievement(seg: MemorySegment): PlayerAchievement {
        val statInfoCount = seg.getInt32(40).toLong() and 0xffffffffL
        val statInfoPtr = seg.get(ValueLayout.ADDRESS, 48)
        val statInfo = if (statInfoPtr.address() == 0L || statInfoCount == 0L) emptyList()
        else {
            val list = mutableListOf<PlayerStatInfo>()
            for (i in 0 until statInfoCount.toInt()) {
                val item = MemorySegment.ofAddress(statInfoPtr.address())
                    .asSlice(i.toLong() * PlayerStatInfo.LAYOUT.byteSize(), PlayerStatInfo.LAYOUT.byteSize())
                list.add(PlayerStatInfo(
                    name = readString(item, 8),
                    currentValue = item.getInt32(16),
                    thresholdValue = item.getInt32(20),
                ))
            }
            list
        }
        return PlayerAchievement(
            id = readString(seg, 8),
            progress = seg.get(ValueLayout.JAVA_DOUBLE, 16),
            unlockTime = seg.getInt64(24),
            statInfo = statInfo,
            displayName = readString(seg, 56),
            description = readString(seg, 64),
            iconUrl = readString(seg, 72),
            flavorText = readString(seg, 80),
        )
    }

    public companion object {
        public const val UNLOCK_TIME_UNDEFINED: Long = -1L
    }
}