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
package gg.sona.eos.achievements

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.NotificationHandle
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.allocCStringArray
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
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = AchievementsQueryDefinitionsOptions(localUserId ?: ProductUserId.Invalid)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Achievements_QueryDefinitions",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
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
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val targetUserId = ProductUserId(data.getInt64(16))
            val localUserId = ProductUserId(data.getInt64(24))
            future.complete(QueryPlayerAchievementsResult(result, targetUserId, localUserId))
        })
        val options = AchievementsQueryPlayerAchievementsOptions(targetUserId, localUserId ?: ProductUserId.Invalid)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Achievements_QueryPlayerAchievements",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
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
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val userId = ProductUserId(data.getInt64(16))
            val count = data.getInt32(24).toLong() and 0xffffffffL
            future.complete(UnlockAchievementsResult(result, userId, count.toInt()))
        })
        val options = AchievementsUnlockAchievementsOptions(userId, achievementIds)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Achievements_UnlockAchievements",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
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
        val invoker = EosCallback { data ->
            val userId = ProductUserId(data.getInt64(16))
            val achievementId = data.getInt64(24).let { addr ->
                if (addr == 0L) "" else
                    MemorySegment.ofAddress(addr).reinterpret(Long.MAX_VALUE).getString(0)
            }
            val unlockTime = data.getInt64(32)
            callback(AchievementUnlockedInfo(userId, achievementId, unlockTime))
        }
        val handle = CallbackStubs.register(invoker)
        val options = AchievementsAddNotifyAchievementsUnlockedV2Options()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Achievements_AddNotifyAchievementsUnlockedV2",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyAchievementsUnlocked(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_Achievements_RemoveNotifyAchievementsUnlockedV2",
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

/** A single stat threshold within an achievement definition. */
public class StatThreshold(public val name: String, public val threshold: Int) {
    internal companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

/** A single player stat-info record within a player achievement. */
public class PlayerStatInfo(public val name: String, public val currentValue: Int, public val thresholdValue: Int) {
    internal companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
        )
    }
}

public class AchievementDefinition(
    public val id: String,
    public val unlockedDisplayName: String,
    public val unlockedDescription: String,
    public val lockedDisplayName: String,
    public val lockedDescription: String,
    public val flavorText: String,
    public val unlockedIconUrl: String,
    public val lockedIconUrl: String,
    public val isHidden: Boolean,
    public val statThresholds: List<StatThreshold>,
)

public class PlayerAchievement(
    public val id: String,
    public val progress: Double,
    public val unlockTime: Long,
    public val statInfo: List<PlayerStatInfo>,
    public val displayName: String,
    public val description: String,
    public val iconUrl: String,
    public val flavorText: String,
)

public class QueryPlayerAchievementsResult(
    public val result: EosResult,
    public val targetUserId: ProductUserId,
    public val localUserId: ProductUserId,
)

public class UnlockAchievementsResult(
    public val result: EosResult,
    public val userId: ProductUserId,
    public val unlockedCount: Int,
)

public class AchievementUnlockedInfo(
    public val userId: ProductUserId,
    public val achievementId: String,
    public val unlockTime: Long,
)

// region Struct writers

internal class AchievementsQueryDefinitionsOptions(var localUserId: ProductUserId) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        // The deprecated EpicAccountId and HiddenAchievementIds are null.
        seg.setInt64(16, 0L)
        seg.setInt64(24, 0L)
        seg.setInt32(32, 0)
        return seg
    }

    companion object {
        const val API_LATEST = 3
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

internal class AchievementsGetAchievementDefinitionCountOptions : StructWriter {
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

internal class AchievementsCopyAchievementDefinitionByIndexOptions(var index: Int) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt32(8, index)
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_INT
        )
    }
}

internal class AchievementsCopyAchievementDefinitionByIdOptions(var id: String) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(id).address())
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS
        )
    }
}

internal class AchievementsQueryPlayerAchievementsOptions(
    var targetUserId: ProductUserId,
    var localUserId: ProductUserId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, targetUserId.raw)
        seg.setInt64(16, localUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}

internal class AchievementsGetPlayerAchievementCountOptions(var userId: ProductUserId) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, userId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG
        )
    }
}

internal class AchievementsCopyPlayerAchievementByIndexOptions(
    var targetUserId: ProductUserId,
    var index: Int,
    var localUserId: ProductUserId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, targetUserId.raw)
        seg.setInt32(16, index)
        seg.setInt64(24, localUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
        )
    }
}

internal class AchievementsCopyPlayerAchievementByAchievementIdOptions(
    var targetUserId: ProductUserId,
    var achievementId: String,
    var localUserId: ProductUserId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, targetUserId.raw)
        seg.setInt64(16, arena.allocCString(achievementId).address())
        seg.setInt64(24, localUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
        )
    }
}

internal class AchievementsUnlockAchievementsOptions(
    var userId: ProductUserId,
    var achievementIds: List<String>,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, userId.raw)
        val arr = arena.allocCStringArray(achievementIds)
        seg.setInt64(16, arr.address())
        seg.setInt32(24, achievementIds.size)
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

internal class AchievementsAddNotifyAchievementsUnlockedV2Options : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4)
        )
    }
}

// endregion
