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

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.NotificationHandle
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientAction
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientActionReason
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientAuthStatus
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientFlags
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientHandle
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientInput
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientPlatform
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientType
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.EventType
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.GameRoundCompetitionType
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.PlayerMovementState
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.PlayerTakeDamageResult
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.PlayerTakeDamageSource
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.PlayerTakeDamageType
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.Quat
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.Vec3f
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.*
import java.lang.foreign.FunctionDescriptor
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
            // EOS_AntiCheatCommon_OnMessageToClientCallbackInfo: ClientData@0, ClientHandle@8, MessageData@16, MessageDataSizeBytes@24
            val clientHandle = ClientHandle(data.getInt64(8))
            val dataPtr = data.get(ValueLayout.ADDRESS, 16)
            val dataSize = data.getInt32(24).toLong() and 0xffffffffL
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
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
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
            // EOS_AntiCheatCommon_OnClientActionRequiredCallbackInfo: ClientData@0, ClientHandle@8, ClientAction@16, ActionReasonCode@20, ActionReasonDetailsString@24
            val clientHandle = ClientHandle(data.getInt64(8))
            val action = ClientAction.fromValue(data.getInt32(16))
            val reason = ClientActionReason.fromValue(data.getInt32(20))
            val reasonPtr = data.get(ValueLayout.ADDRESS, 24)
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
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
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
            // EOS_AntiCheatCommon_OnClientAuthStatusChangedCallbackInfo: ClientData@0, ClientHandle@8, ClientAuthStatus@16
            val clientHandle = ClientHandle(data.getInt64(8))
            val status = ClientAuthStatus.fromValue(data.getInt32(16))
            callback(ClientAuthStatusChangedInfo(clientHandle, status))
        }
        val handle = CallbackStubs.register(invoker)
        val options = AntiCheatServerAddNotifyClientAuthStatusChangedOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_AntiCheatServer_AddNotifyClientAuthStatusChanged",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
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










































// endregion
