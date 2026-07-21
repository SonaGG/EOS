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
package gg.sona.eos.anticheat.server

import gg.sona.eos.NotificationHandle
import gg.sona.eos.internal.setInt8
import gg.sona.eos.internal.setInt16
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
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientAction
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientActionReason
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientAuthStatus
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientFlags
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientHandle
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientInput
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientPlatform
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientType
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.EventParamType
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.EventType
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.GameRoundCompetitionType
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.PlayerMovementState
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.PlayerTakeDamageResult
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.PlayerTakeDamageSource
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.PlayerTakeDamageType
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.Quat
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.Vec3f
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setFloat
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import gg.sona.eos.internal.withCallArena
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * Anti-Cheat Server interface.
 *
 * The anti-cheat server runs in a trusted process (typically a dedicated
 * game server). It receives anti-cheat messages from clients, decides on
 * enforcement, and can log gameplay events for the Cerberus anti-cheat
 * feature.
 */
public class EosAntiCheatServer internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetAntiCheatServerInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /**
     * Begin the gameplay session. Notification callbacks must be configured
     * with [addNotifyMessageToClient] and [addNotifyClientActionRequired]
     * before calling this function.
     */
    public fun beginSession(
        registerTimeoutSeconds: Int = 60,
        serverName: String? = null,
        enableGameplayData: Boolean = true,
        localUserId: ProductUserId? = null,
    ): EosResult = withCallArena { arena ->
        val options = AntiCheatServerBeginSessionOptions(
            registerTimeoutSeconds, serverName, enableGameplayData, localUserId,
        )
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_BeginSession",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    /** End the gameplay session. */
    public fun endSession(): EosResult = withCallArena { arena ->
        val options = AntiCheatServerEndSessionOptions()
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_EndSession",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun addNotifyMessageToClient(callback: (MessageToClientInfo) -> Unit): NotificationHandle {
        val invoker = EosCallback { data ->
            val clientHandle = ClientHandle(data.getInt64(16))
            val dataPtr = data.get(ValueLayout.ADDRESS, 24)
            val dataSize = data.getInt32(32).toLong() and 0xffffffffL
            val bytes = if (dataPtr.address() == 0L || dataSize == 0L) ByteArray(0)
            else {
                val arr = ByteArray(dataSize.toInt())
                MemorySegment.ofArray(arr).copyFrom(MemorySegment.ofAddress(dataPtr.address()).reinterpret(dataSize))
                arr
            }
            callback(MessageToClientInfo(clientHandle, bytes))
        }
        val handle = CallbackStubs.register(invoker)
        val options = AntiCheatServerAddNotifyMessageToClientOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_AntiCheatServer_AddNotifyMessageToClient",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyMessageToClient(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_AntiCheatServer_RemoveNotifyMessageToClient",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun addNotifyClientActionRequired(
        callback: (ClientActionRequiredInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val clientHandle = ClientHandle(data.getInt64(16))
            val action = ClientAction.fromValue(data.getInt32(24))
            val reason = ClientActionReason.fromValue(data.getInt32(28))
            val reasonPtr = data.get(ValueLayout.ADDRESS, 32)
            val reasonString = if (reasonPtr.address() == 0L) "" else
                reasonPtr.reinterpret(Long.MAX_VALUE).getString(0)
            callback(ClientActionRequiredInfo(clientHandle, action, reason, reasonString))
        }
        val handle = CallbackStubs.register(invoker)
        val options = AntiCheatServerAddNotifyClientActionRequiredOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_AntiCheatServer_AddNotifyClientActionRequired",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyClientActionRequired(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_AntiCheatServer_RemoveNotifyClientActionRequired",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun addNotifyClientAuthStatusChanged(
        callback: (ClientAuthStatusChangedInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val clientHandle = ClientHandle(data.getInt64(16))
            val status = ClientAuthStatus.fromValue(data.getInt32(24))
            callback(ClientAuthStatusChangedInfo(clientHandle, status))
        }
        val handle = CallbackStubs.register(invoker)
        val options = AntiCheatServerAddNotifyClientAuthStatusChangedOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_AntiCheatServer_AddNotifyClientAuthStatusChanged",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyClientAuthStatusChanged(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_AntiCheatServer_RemoveNotifyClientAuthStatusChanged",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun registerClient(
        clientHandle: ClientHandle,
        clientType: ClientType,
        clientPlatform: ClientPlatform = ClientPlatform.Unknown,
        ipAddress: String? = null,
        userId: ProductUserId,
    ): EosResult = withCallArena { arena ->
        val options = AntiCheatServerRegisterClientOptions(
            clientHandle, clientType, clientPlatform, ipAddress, userId,
        )
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_RegisterClient",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun unregisterClient(clientHandle: ClientHandle): EosResult = withCallArena { arena ->
        val options = AntiCheatServerUnregisterClientOptions(clientHandle)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_UnregisterClient",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun receiveMessageFromClient(clientHandle: ClientHandle, data: ByteArray): EosResult =
        withCallArena { arena ->
            val buf = arena.allocate(data.size.toLong())
            buf.copyFrom(MemorySegment.ofArray(data))
            val options = AntiCheatServerReceiveMessageFromClientOptions(clientHandle, data.size, buf)
            EosResult.fromValue(
                Native.invoke(
                    "EOS_AntiCheatServer_ReceiveMessageFromClient",
                    listOf(handle(), options.writeTo(arena)),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
        }

    /** Sets or updates client details including admin status and input device. */
    public fun setClientDetails(
        clientHandle: ClientHandle,
        flags: Set<ClientFlags> = emptySet(),
        inputMethod: ClientInput = ClientInput.Unknown,
    ): EosResult = withCallArena { arena ->
        val flagsInt = flags.fold(0) { acc, f -> acc or f.value }
        val options = AntiCheatServerSetClientDetailsOptions(clientHandle, flagsInt, inputMethod)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_SetClientDetails",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    /** Sets or updates a game session identifier attached to subsequent data. */
    public fun setGameSessionId(gameSessionId: String): EosResult = withCallArena { arena ->
        val options = AntiCheatServerSetGameSessionIdOptions(gameSessionId)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_SetGameSessionId",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    /**
     * Indicate that a client is legitimately known to be temporarily unable
     * to communicate (e.g. loading a new level). The bIsNetworkActive flag
     * must be set back to true when the user returns to normal gameplay.
     */
    public fun setClientNetworkState(clientHandle: ClientHandle, isNetworkActive: Boolean): EosResult =
        withCallArena { arena ->
            val options = AntiCheatServerSetClientNetworkStateOptions(clientHandle, isNetworkActive)
            EosResult.fromValue(
                Native.invoke(
                    "EOS_AntiCheatServer_SetClientNetworkState",
                    listOf(handle(), options.writeTo(arena)),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
        }

    /**
     * Calculate the required output buffer size to encrypt a message of
     * [dataLengthBytes] via [protectMessage].
     */
    public fun getProtectMessageOutputLength(dataLengthBytes: Int): Int = withCallArena { arena ->
        val outPtr = arena.allocate(ValueLayout.JAVA_INT)
        val options = AntiCheatServerGetProtectMessageOutputLengthOptions(dataLengthBytes)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_GetProtectMessageOutputLength",
                listOf(handle(), options.writeTo(arena), outPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        outPtr.get(ValueLayout.JAVA_INT, 0)
    }

    public fun protectMessage(clientHandle: ClientHandle, data: ByteArray): ByteArray = withCallArena { arena ->
        val inBuf = arena.allocate(data.size.toLong())
        inBuf.copyFrom(MemorySegment.ofArray(data))
        val outSize = getProtectMessageOutputLength(data.size)
        val outBuf = arena.allocate(outSize.toLong())
        val outWrittenPtr = arena.allocate(ValueLayout.JAVA_INT)
        val options = AntiCheatServerProtectMessageOptions(clientHandle, data.size, inBuf, outSize)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_ProtectMessage",
                listOf(handle(), options.writeTo(arena), outBuf, outWrittenPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        val written = outWrittenPtr.get(ValueLayout.JAVA_INT, 0)
        val arr = ByteArray(written)
        MemorySegment.ofArray(arr).copyFrom(MemorySegment.ofAddress(outBuf.address()).reinterpret(written.toLong()))
        arr
    }

    public fun unprotectMessage(clientHandle: ClientHandle, data: ByteArray): ByteArray = withCallArena { arena ->
        val inBuf = arena.allocate(data.size.toLong())
        inBuf.copyFrom(MemorySegment.ofArray(data))
        val outSize = data.size
        val outBuf = arena.allocate(outSize.toLong())
        val outWrittenPtr = arena.allocate(ValueLayout.JAVA_INT)
        val options = AntiCheatServerUnprotectMessageOptions(clientHandle, data.size, inBuf, outSize)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_UnprotectMessage",
                listOf(handle(), options.writeTo(arena), outBuf, outWrittenPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        val written = outWrittenPtr.get(ValueLayout.JAVA_INT, 0)
        val arr = ByteArray(written)
        MemorySegment.ofArray(arr).copyFrom(MemorySegment.ofAddress(outBuf.address()).reinterpret(written.toLong()))
        arr
    }

    // region Cerberus gameplay event logging

    /**
     * Register a custom gameplay event. Must be called before [beginSession]
     * for the first time.
     */
    public fun registerEvent(
        eventId: UInt,
        eventName: String,
        eventType: EventType,
        paramDefs: List<EventParamDef>,
    ): EosResult = withCallArena { arena ->
        val paramSegs = paramDefs.map { it.writeTo(arena) }
        val paramArr = arena.allocate(ValueLayout.ADDRESS, paramSegs.size.toLong())
        paramSegs.forEachIndexed { i, s -> paramArr.setAtIndex(ValueLayout.ADDRESS, i.toLong(), s) }
        val options = AntiCheatServerRegisterEventOptions(
            eventId, eventName, eventType, paramSegs.size, paramArr,
        )
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_RegisterEvent",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun logEvent(
        clientHandle: ClientHandle,
        eventId: UInt,
        params: List<EventParamPair>,
    ): EosResult = withCallArena { arena ->
        val paramSegs = params.map { it.writeTo(arena) }
        val paramArr = arena.allocate(ValueLayout.ADDRESS, paramSegs.size.toLong())
        paramSegs.forEachIndexed { i, s -> paramArr.setAtIndex(ValueLayout.ADDRESS, i.toLong(), s) }
        val options = AntiCheatServerLogEventOptions(
            clientHandle, eventId, paramSegs.size, paramArr,
        )
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_LogEvent",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun logGameRoundStart(
        sessionIdentifier: String? = null,
        levelName: String? = null,
        modeName: String? = null,
        roundTimeSeconds: UInt = 0u,
        competitionType: GameRoundCompetitionType = GameRoundCompetitionType.None,
    ): EosResult = withCallArena { arena ->
        val options = AntiCheatServerLogGameRoundStartOptions(
            sessionIdentifier, levelName, modeName, roundTimeSeconds, competitionType,
        )
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_LogGameRoundStart",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun logGameRoundEnd(winningTeamId: UInt = 0u): EosResult = withCallArena { arena ->
        val options = AntiCheatServerLogGameRoundEndOptions(winningTeamId)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_LogGameRoundEnd",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun logPlayerSpawn(
        playerHandle: ClientHandle,
        teamId: UInt = 0u,
        characterId: UInt = 0u,
    ): EosResult = withCallArena { arena ->
        val options = AntiCheatServerLogPlayerSpawnOptions(playerHandle, teamId, characterId)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_LogPlayerSpawn",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun logPlayerDespawn(playerHandle: ClientHandle): EosResult = withCallArena { arena ->
        val options = AntiCheatServerLogPlayerDespawnOptions(playerHandle)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_LogPlayerDespawn",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun logPlayerRevive(
        revivedPlayerHandle: ClientHandle,
        reviverPlayerHandle: ClientHandle,
    ): EosResult = withCallArena { arena ->
        val options = AntiCheatServerLogPlayerReviveOptions(revivedPlayerHandle, reviverPlayerHandle)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_LogPlayerRevive",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun logPlayerTick(
        playerHandle: ClientHandle,
        position: Vec3f,
        viewRotation: Quat,
        isViewZoomed: Boolean,
        health: Float,
        movementState: PlayerMovementState = PlayerMovementState.None,
        viewPosition: Vec3f? = null,
    ): EosResult = withCallArena { arena ->
        val options = AntiCheatServerLogPlayerTickOptions(
            playerHandle, position, viewRotation, isViewZoomed, health, movementState, viewPosition,
        )
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_LogPlayerTick",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun logPlayerUseWeapon(
        playerHandle: ClientHandle,
        position: Vec3f,
        viewRotation: Quat,
        isViewZoomed: Boolean,
        isMeleeAttack: Boolean,
        weaponName: String,
    ): EosResult = withCallArena { arena ->
        val useWeaponData = AntiCheatServerLogPlayerUseWeaponData(
            playerHandle, position, viewRotation, isViewZoomed, isMeleeAttack, weaponName,
        )
        val options = AntiCheatServerLogPlayerUseWeaponOptions(useWeaponData)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_LogPlayerUseWeapon",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun logPlayerUseAbility(
        playerHandle: ClientHandle,
        abilityId: UInt,
        abilityDurationMs: UInt = 0u,
        abilityCooldownMs: UInt = 0u,
    ): EosResult = withCallArena { arena ->
        val options = AntiCheatServerLogPlayerUseAbilityOptions(
            playerHandle, abilityId, abilityDurationMs, abilityCooldownMs,
        )
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_LogPlayerUseAbility",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun logPlayerTakeDamage(
        victimPlayerHandle: ClientHandle,
        victimPosition: Vec3f,
        victimViewRotation: Quat,
        attackerPlayerHandle: ClientHandle,
        attackerPosition: Vec3f?,
        attackerViewRotation: Quat?,
        isHitscanAttack: Boolean,
        hasLineOfSight: Boolean,
        isCriticalHit: Boolean,
        damageTaken: Float,
        healthRemaining: Float,
        damageSource: PlayerTakeDamageSource = PlayerTakeDamageSource.None,
        damageType: PlayerTakeDamageType = PlayerTakeDamageType.None,
        damageResult: PlayerTakeDamageResult = PlayerTakeDamageResult.None,
        damagePosition: Vec3f? = null,
        attackerViewPosition: Vec3f? = null,
    ): EosResult = withCallArena { arena ->
        val options = AntiCheatServerLogPlayerTakeDamageOptions(
            victimPlayerHandle, victimPosition, victimViewRotation,
            attackerPlayerHandle, attackerPosition, attackerViewRotation,
            isHitscanAttack, hasLineOfSight, isCriticalHit,
            damageTaken, healthRemaining,
            damageSource, damageType, damageResult,
            damagePosition, attackerViewPosition,
        )
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatServer_LogPlayerTakeDamage",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    // endregion

    public companion object {
        public const val MAX_MESSAGE_SIZE: Int = 512
        public const val MIN_REGISTER_TIMEOUT: Int = 10
        public const val MAX_REGISTER_TIMEOUT: Int = 120
    }
}

/** A custom event parameter definition. */
public class EventParamDef(public val name: String, public val type: EventParamType) {
    internal fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(MemoryLayout.structLayout(ValueLayout.ADDRESS, ValueLayout.JAVA_INT))
        seg.setInt64(0, arena.allocCString(name).address())
        seg.setInt32(8, type.value)
        return seg
    }
}

/** A custom event parameter value. */
public sealed class EventParamPair(public val type: EventParamType) {
    public class ClientHandleValue(public val value: gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientHandle) :
        EventParamPair(EventParamType.ClientHandle)
    public class StringValue(public val value: String) : EventParamPair(EventParamType.String)
    public class UInt32Value(public val value: UInt) : EventParamPair(EventParamType.UInt32)
    public class Int32Value(public val value: Int) : EventParamPair(EventParamType.Int32)
    public class UInt64Value(public val value: ULong) : EventParamPair(EventParamType.UInt64)
    public class Int64Value(public val value: Long) : EventParamPair(EventParamType.Int64)
    public class Vector3fValue(public val value: Vec3f) : EventParamPair(EventParamType.Vector3f)
    public class QuatValue(public val value: Quat) : EventParamPair(EventParamType.Quat)
    public class FloatValue(public val value: Float) : EventParamPair(EventParamType.Float)

    internal fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG,
        ))
        seg.setInt32(0, type.value)
        when (this) {
            is ClientHandleValue -> seg.setInt64(8, value.raw)
            is StringValue -> seg.setInt64(8, arena.allocCString(value).address())
            is UInt32Value -> seg.setInt32(8, value.toInt())
            is Int32Value -> seg.setInt32(8, value)
            is UInt64Value -> seg.setInt64(8, value.toLong())
            is Int64Value -> seg.setInt64(8, value)
            is Vector3fValue -> {
                val v = arena.allocate(MemoryLayout.structLayout(
                    ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
                ))
                v.set(ValueLayout.JAVA_FLOAT, 0, value.x)
                v.set(ValueLayout.JAVA_FLOAT, 4, value.y)
                v.set(ValueLayout.JAVA_FLOAT, 8, value.z)
                seg.setInt64(8, v.address())
            }
            is QuatValue -> {
                val q = arena.allocate(MemoryLayout.structLayout(
                    ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
                ))
                q.set(ValueLayout.JAVA_FLOAT, 0, value.w)
                q.set(ValueLayout.JAVA_FLOAT, 4, value.x)
                q.set(ValueLayout.JAVA_FLOAT, 8, value.y)
                q.set(ValueLayout.JAVA_FLOAT, 12, value.z)
                seg.setInt64(8, q.address())
            }
            is FloatValue -> seg.set(ValueLayout.JAVA_FLOAT, 8, value)
        }
        return seg
    }
}

public class MessageToClientInfo(public val clientHandle: ClientHandle, public val data: ByteArray)
public class ClientActionRequiredInfo(
    public val clientHandle: ClientHandle,
    public val action: ClientAction,
    public val reasonCode: ClientActionReason,
    public val reasonString: String,
)
public class ClientAuthStatusChangedInfo(
    public val clientHandle: ClientHandle,
    public val newStatus: ClientAuthStatus,
)

// region Struct writers

internal class AntiCheatServerBeginSessionOptions(
    var registerTimeoutSeconds: Int,
    var serverName: String?,
    var enableGameplayData: Boolean,
    var localUserId: ProductUserId?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 3)
        seg.setInt32(8, registerTimeoutSeconds)
        seg.setInt64(16, arena.allocCString(serverName).address())
        seg.setInt32(24, if (enableGameplayData) 1 else 0)
        seg.setInt64(32, localUserId?.raw ?: 0L)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG,
        )
    }
}

internal class AntiCheatServerEndSessionOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}

internal class AntiCheatServerAddNotifyMessageToClientOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}

internal class AntiCheatServerAddNotifyClientActionRequiredOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}

internal class AntiCheatServerAddNotifyClientAuthStatusChangedOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}

internal class AntiCheatServerRegisterClientOptions(
    var clientHandle: ClientHandle,
    var clientType: ClientType,
    var clientPlatform: ClientPlatform,
    var ipAddress: String?,
    var userId: ProductUserId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 3)
        seg.setInt64(8, clientHandle.raw)
        seg.setInt32(16, clientType.value)
        seg.setInt32(20, clientPlatform.value)
        seg.setInt64(24, 0L) // deprecated
        seg.setInt64(32, arena.allocCString(ipAddress).address())
        seg.setInt64(40, userId.raw)
        seg.setInt32(48, 0)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
            MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}

internal class AntiCheatServerUnregisterClientOptions(var clientHandle: ClientHandle) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, clientHandle.raw)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
        )
    }
}

internal class AntiCheatServerReceiveMessageFromClientOptions(
    var clientHandle: ClientHandle,
    var dataLengthBytes: Int,
    var data: MemorySegment,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, clientHandle.raw)
        seg.setInt32(16, dataLengthBytes)
        seg.setInt64(24, data.address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS,
        )
    }
}

internal class AntiCheatServerSetClientDetailsOptions(
    var clientHandle: ClientHandle,
    var clientFlags: Int,
    var clientInputMethod: ClientInput,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, clientHandle.raw)
        seg.setInt32(16, clientFlags)
        seg.setInt32(20, clientInputMethod.value)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
        )
    }
}

internal class AntiCheatServerSetGameSessionIdOptions(var gameSessionId: String) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, arena.allocCString(gameSessionId).address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
        )
    }
}

internal class AntiCheatServerSetClientNetworkStateOptions(
    var clientHandle: ClientHandle,
    var isNetworkActive: Boolean,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, clientHandle.raw)
        seg.setInt32(16, if (isNetworkActive) 1 else 0)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT,
        )
    }
}

internal class AntiCheatServerGetProtectMessageOutputLengthOptions(var dataLengthBytes: Int) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt32(8, dataLengthBytes)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_INT,
        )
    }
}

internal class AntiCheatServerProtectMessageOptions(
    var clientHandle: ClientHandle,
    var dataLengthBytes: Int,
    var data: MemorySegment,
    var outBufferSizeBytes: Int,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, clientHandle.raw)
        seg.setInt32(16, dataLengthBytes)
        seg.setInt64(24, data.address())
        seg.setInt32(32, outBufferSizeBytes)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

internal class AntiCheatServerUnprotectMessageOptions(
    var clientHandle: ClientHandle,
    var dataLengthBytes: Int,
    var data: MemorySegment,
    var outBufferSizeBytes: Int,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, clientHandle.raw)
        seg.setInt32(16, dataLengthBytes)
        seg.setInt64(24, data.address())
        seg.setInt32(32, outBufferSizeBytes)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

internal class AntiCheatServerRegisterEventOptions(
    var eventId: UInt,
    var eventName: String,
    var eventType: EventType,
    var paramDefsCount: Int,
    var paramDefs: MemorySegment,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt32(8, eventId.toInt())
        seg.setInt64(16, arena.allocCString(eventName).address())
        seg.setInt32(24, eventType.value)
        seg.setInt32(28, paramDefsCount)
        seg.setInt64(32, paramDefs.address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
            MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS,
        )
    }
}

internal class AntiCheatServerLogEventOptions(
    var clientHandle: ClientHandle,
    var eventId: UInt,
    var paramsCount: Int,
    var params: MemorySegment,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, clientHandle.raw)
        seg.setInt32(16, eventId.toInt())
        seg.setInt32(20, paramsCount)
        seg.setInt64(24, params.address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
        )
    }
}

internal class AntiCheatServerLogGameRoundStartOptions(
    var sessionIdentifier: String?,
    var levelName: String?,
    var modeName: String?,
    var roundTimeSeconds: UInt,
    var competitionType: GameRoundCompetitionType,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 2)
        seg.setInt64(8, arena.allocCString(sessionIdentifier).address())
        seg.setInt64(16, arena.allocCString(levelName).address())
        seg.setInt64(24, arena.allocCString(modeName).address())
        seg.setInt32(32, roundTimeSeconds.toInt())
        seg.setInt32(36, competitionType.value)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
        )
    }
}

internal class AntiCheatServerLogGameRoundEndOptions(var winningTeamId: UInt) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt32(8, winningTeamId.toInt())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_INT,
        )
    }
}

internal class AntiCheatServerLogPlayerSpawnOptions(
    var spawnedPlayerHandle: ClientHandle,
    var teamId: UInt,
    var characterId: UInt,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, spawnedPlayerHandle.raw)
        seg.setInt32(16, teamId.toInt())
        seg.setInt32(20, characterId.toInt())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
        )
    }
}

internal class AntiCheatServerLogPlayerDespawnOptions(var despawnedPlayerHandle: ClientHandle) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, despawnedPlayerHandle.raw)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
        )
    }
}

internal class AntiCheatServerLogPlayerReviveOptions(
    var revivedPlayerHandle: ClientHandle,
    var reviverPlayerHandle: ClientHandle,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, revivedPlayerHandle.raw)
        seg.setInt64(16, reviverPlayerHandle.raw)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}

internal class AntiCheatServerLogPlayerTickOptions(
    var playerHandle: ClientHandle,
    var playerPosition: Vec3f,
    var playerViewRotation: Quat,
    var isPlayerViewZoomed: Boolean,
    var playerHealth: Float,
    var playerMovementState: PlayerMovementState,
    var playerViewPosition: Vec3f?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 3)
        seg.setInt64(8, playerHandle.raw)
        val positionSeg = arena.allocate(MemoryLayout.structLayout(
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
        ))
        positionSeg.set(ValueLayout.JAVA_FLOAT, 0, playerPosition.x)
        positionSeg.set(ValueLayout.JAVA_FLOAT, 4, playerPosition.y)
        positionSeg.set(ValueLayout.JAVA_FLOAT, 8, playerPosition.z)
        seg.setInt64(16, positionSeg.address())
        val viewRotSeg = arena.allocate(MemoryLayout.structLayout(
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
        ))
        viewRotSeg.set(ValueLayout.JAVA_FLOAT, 0, playerViewRotation.w)
        viewRotSeg.set(ValueLayout.JAVA_FLOAT, 4, playerViewRotation.x)
        viewRotSeg.set(ValueLayout.JAVA_FLOAT, 8, playerViewRotation.y)
        viewRotSeg.set(ValueLayout.JAVA_FLOAT, 12, playerViewRotation.z)
        seg.setInt64(24, viewRotSeg.address())
        seg.setInt32(32, if (isPlayerViewZoomed) 1 else 0)
        seg.setFloat(36, playerHealth)
        seg.setInt32(40, playerMovementState.value)
        if (playerViewPosition != null) {
            val pos = playerViewPosition!!
            val vpSeg = arena.allocate(MemoryLayout.structLayout(
                ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
            ))
            vpSeg.set(ValueLayout.JAVA_FLOAT, 0, pos.x)
            vpSeg.set(ValueLayout.JAVA_FLOAT, 4, pos.y)
            vpSeg.set(ValueLayout.JAVA_FLOAT, 8, pos.z)
            seg.setInt64(48, vpSeg.address())
        } else {
            seg.setInt64(48, 0L)
        }
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_INT,
            MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS,
        )
    }
}

internal class AntiCheatServerLogPlayerUseWeaponData(
    var playerHandle: ClientHandle,
    var playerPosition: Vec3f,
    var playerViewRotation: Quat,
    var isPlayerViewZoomed: Boolean,
    var isMeleeAttack: Boolean,
    var weaponName: String,
) {
    fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt64(0, playerHandle.raw)
        val positionSeg = arena.allocate(MemoryLayout.structLayout(
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
        ))
        positionSeg.set(ValueLayout.JAVA_FLOAT, 0, playerPosition.x)
        positionSeg.set(ValueLayout.JAVA_FLOAT, 4, playerPosition.y)
        positionSeg.set(ValueLayout.JAVA_FLOAT, 8, playerPosition.z)
        seg.setInt64(8, positionSeg.address())
        val viewRotSeg = arena.allocate(MemoryLayout.structLayout(
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
        ))
        viewRotSeg.set(ValueLayout.JAVA_FLOAT, 0, playerViewRotation.w)
        viewRotSeg.set(ValueLayout.JAVA_FLOAT, 4, playerViewRotation.x)
        viewRotSeg.set(ValueLayout.JAVA_FLOAT, 8, playerViewRotation.y)
        viewRotSeg.set(ValueLayout.JAVA_FLOAT, 12, playerViewRotation.z)
        seg.setInt64(16, viewRotSeg.address())
        seg.setInt32(24, if (isPlayerViewZoomed) 1 else 0)
        seg.setInt32(28, if (isMeleeAttack) 1 else 0)
        seg.setInt64(32, arena.allocCString(weaponName).address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
        )
    }
}

internal class AntiCheatServerLogPlayerUseWeaponOptions(
    var useWeaponData: AntiCheatServerLogPlayerUseWeaponData,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 2)
        seg.setInt64(8, useWeaponData.writeTo(arena).address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
        )
    }
}

internal class AntiCheatServerLogPlayerUseAbilityOptions(
    var playerHandle: ClientHandle,
    var abilityId: UInt,
    var abilityDurationMs: UInt,
    var abilityCooldownMs: UInt,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, playerHandle.raw)
        seg.setInt32(16, abilityId.toInt())
        seg.setInt32(20, abilityDurationMs.toInt())
        seg.setInt32(24, abilityCooldownMs.toInt())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
        )
    }
}

internal class AntiCheatServerLogPlayerTakeDamageOptions(
    var victimPlayerHandle: ClientHandle,
    var victimPlayerPosition: Vec3f,
    var victimPlayerViewRotation: Quat,
    var attackerPlayerHandle: ClientHandle,
    var attackerPlayerPosition: Vec3f?,
    var attackerPlayerViewRotation: Quat?,
    var isHitscanAttack: Boolean,
    var hasLineOfSight: Boolean,
    var isCriticalHit: Boolean,
    var damageTaken: Float,
    var healthRemaining: Float,
    var damageSource: PlayerTakeDamageSource,
    var damageType: PlayerTakeDamageType,
    var damageResult: PlayerTakeDamageResult,
    var damagePosition: Vec3f?,
    var attackerPlayerViewPosition: Vec3f?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 4)
        seg.setInt64(8, victimPlayerHandle.raw)
        seg.setInt64(16, writeVec3(arena, victimPlayerPosition).address())
        seg.setInt64(24, writeQuat(arena, victimPlayerViewRotation).address())
        seg.setInt64(32, attackerPlayerHandle.raw)
        seg.setInt64(40, attackerPlayerPosition?.let { writeVec3(arena, it).address() } ?: 0L)
        seg.setInt64(48, attackerPlayerViewRotation?.let { writeQuat(arena, it).address() } ?: 0L)
        seg.setInt32(56, if (isHitscanAttack) 1 else 0)
        seg.setInt32(60, if (hasLineOfSight) 1 else 0)
        seg.setInt32(64, if (isCriticalHit) 1 else 0)
        seg.setInt32(68, 0) // deprecated
        seg.setFloat(72, damageTaken)
        seg.setFloat(76, healthRemaining)
        seg.setInt32(80, damageSource.value)
        seg.setInt32(84, damageType.value)
        seg.setInt32(88, damageResult.value)
        seg.setInt64(96, 0L) // use weapon data pointer (omitted)
        seg.setInt32(104, 0)
        seg.setInt64(112, damagePosition?.let { writeVec3(arena, it).address() } ?: 0L)
        seg.setInt64(120, attackerPlayerViewPosition?.let { writeVec3(arena, it).address() } ?: 0L)
        return seg
    }

    private fun writeVec3(arena: Arena, v: Vec3f): MemorySegment {
        val seg = arena.allocate(MemoryLayout.structLayout(
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
        ))
        seg.set(ValueLayout.JAVA_FLOAT, 0, v.x)
        seg.set(ValueLayout.JAVA_FLOAT, 4, v.y)
        seg.set(ValueLayout.JAVA_FLOAT, 8, v.z)
        return seg
    }

    private fun writeQuat(arena: Arena, q: Quat): MemorySegment {
        val seg = arena.allocate(MemoryLayout.structLayout(
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
        ))
        seg.set(ValueLayout.JAVA_FLOAT, 0, q.w)
        seg.set(ValueLayout.JAVA_FLOAT, 4, q.x)
        seg.set(ValueLayout.JAVA_FLOAT, 8, q.y)
        seg.set(ValueLayout.JAVA_FLOAT, 12, q.z)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
            MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
            MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS, ValueLayout.ADDRESS,
        )
    }
}

// endregion
