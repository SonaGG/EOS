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
package gg.sona.eos.anticheat.client

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
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientAction
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientActionReason
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientAuthStatus
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientFlags
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientHandle
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientInput
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientPlatform
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientType
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

/**
 * Anti-Cheat Client interface.
 *
 * The anti-cheat client runs inside the game process. It verifies the integrity
 * of game files, monitors running processes, and reports any anomalies to the
 * anti-cheat service or to a dedicated server.
 *
 * Two modes are supported:
 *  - [EosAntiCheatClientMode.ClientServer]: The client reports to a dedicated
 *    server (anti-cheat server interface). The server decides on enforcement.
 *  - [EosAntiCheatClientMode.PeerToPeer]: The client reports to other clients
 *    in the same session. Useful for fully peer-to-peer games.
 */
public class EosAntiCheatClient internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetAntiCheatClientInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /**
     * Begin an anti-cheat session. After this call returns successfully, the
     * client is ready to exchange anti-cheat messages with a game server or
     * peer(s).
     */
    public fun beginSession(localUserId: ProductUserId, mode: EosAntiCheatClientMode): EosResult =
        withCallArena { arena ->
            val options = AntiCheatClientBeginSessionOptions(localUserId, mode)
            EosResult.fromValue(
                Native.invoke(
                    "EOS_AntiCheatClient_BeginSession",
                    listOf(handle(), options.writeTo(arena)),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
        }

    /** End the current anti-cheat session. */
    public fun endSession(): EosResult = withCallArena { arena ->
        val options = AntiCheatClientEndSessionOptions()
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatClient_EndSession",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    /**
     * Add an integrity catalog and certificate pair from outside the game
     * directory, e.g. for mods that load from elsewhere.
     */
    public fun addExternalIntegrityCatalog(pathToBinFile: String): EosResult = withCallArena { arena ->
        val options = AntiCheatClientAddExternalIntegrityCatalogOptions(pathToBinFile)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatClient_AddExternalIntegrityCatalog",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    /**
     * Get the build id of the loaded anti-cheat client module. Useful for
     * analytics or troubleshooting.
     */
    public fun getModuleBuildId(): UInt? = withCallArena { arena ->
        val outPtr = arena.allocate(ValueLayout.JAVA_INT)
        val options = AntiCheatClientGetModuleBuildIdOptions()
        val result = EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatClient_GetModuleBuildId",
                listOf(handle(), options.writeTo(arena), outPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        if (result != EosResult.Success) null else
            outPtr.get(ValueLayout.JAVA_INT, 0).toUInt()
    }

    // region ClientServer mode

    /**
     * Notify the anti-cheat client that a new message must be dispatched to
     * the game server. The returned payload must be transmitted to the server
     * using your own networking layer and then delivered to the server anti-cheat
     * instance via [gg.sona.eos.anticheat.server.EosAntiCheatServer.receiveMessageFromClient].
     *
     * Only valid in [EosAntiCheatClientMode.ClientServer] mode.
     */
    public fun addNotifyMessageToServer(
        callback: (MessageToServerInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val dataPtr = data.get(ValueLayout.ADDRESS, 16)
            val dataSize = data.getInt32(24).toLong() and 0xffffffffL
            val bytes = if (dataPtr.address() == 0L || dataSize == 0L) ByteArray(0)
            else {
                val arr = ByteArray(dataSize.toInt())
                MemorySegment.ofArray(arr).copyFrom(MemorySegment.ofAddress(dataPtr.address()).reinterpret(dataSize))
                arr
            }
            callback(MessageToServerInfo(bytes))
        }
        val handle = CallbackStubs.register(invoker)
        val options = AntiCheatClientAddNotifyMessageToServerOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_AntiCheatClient_AddNotifyMessageToServer",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyMessageToServer(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_AntiCheatClient_RemoveNotifyMessageToServer",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    /** Receive an anti-cheat message from the game server. ClientServer mode only. */
    public fun receiveMessageFromServer(data: ByteArray): EosResult {
        require(data.size <= MAX_MESSAGE_SIZE) { "message exceeds MAX_MESSAGE_SIZE" }
        return withCallArena { arena ->
            val buf = arena.allocate(data.size.toLong())
            buf.copyFrom(MemorySegment.ofArray(data))
            val options = AntiCheatClientReceiveMessageFromServerOptions(data.size, buf)
            EosResult.fromValue(
                Native.invoke(
                    "EOS_AntiCheatClient_ReceiveMessageFromServer",
                    listOf(handle(), options.writeTo(arena)),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
        }
    }

    /**
     * Calculates the required output buffer size to encrypt a message of
     * [dataLengthBytes] bytes via [protectMessage]. The result is stable for
     * a given SDK version.
     */
    public fun getProtectMessageOutputLength(dataLengthBytes: Int): Int = withCallArena { arena ->
        val outPtr = arena.allocate(ValueLayout.JAVA_INT)
        val options = AntiCheatClientGetProtectMessageOutputLengthOptions(dataLengthBytes)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatClient_GetProtectMessageOutputLength",
                listOf(handle(), options.writeTo(arena), outPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        outPtr.get(ValueLayout.JAVA_INT, 0)
    }

    /**
     * Encrypt an arbitrary message to be sent to the game server. The
     * returned [ProtectedMessage.outBuffer] is sized to the result of
     * [getProtectMessageOutputLength] and contains the ciphertext.
     */
    public fun protectMessage(data: ByteArray): ProtectedMessage = withCallArena { arena ->
        val inBuf = arena.allocate(data.size.toLong())
        inBuf.copyFrom(MemorySegment.ofArray(data))
        val outSize = getProtectMessageOutputLength(data.size)
        val outBuf = arena.allocate(outSize.toLong())
        val outWrittenPtr = arena.allocate(ValueLayout.JAVA_INT)
        val options = AntiCheatClientProtectMessageOptions(data.size, inBuf, outSize)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatClient_ProtectMessage",
                listOf(handle(), options.writeTo(arena), outBuf, outWrittenPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        val written = outWrittenPtr.get(ValueLayout.JAVA_INT, 0)
        val arr = ByteArray(written)
        MemorySegment.ofArray(arr).copyFrom(MemorySegment.ofAddress(outBuf.address()).reinterpret(written.toLong()))
        ProtectedMessage(arr)
    }

    /** Decrypt a message received from the game server. */
    public fun unprotectMessage(data: ByteArray): ByteArray = withCallArena { arena ->
        val inBuf = arena.allocate(data.size.toLong())
        inBuf.copyFrom(MemorySegment.ofArray(data))
        // Conservative estimate: decrypted output is always smaller than input.
        val outSize = data.size
        val outBuf = arena.allocate(outSize.toLong())
        val outWrittenPtr = arena.allocate(ValueLayout.JAVA_INT)
        val options = AntiCheatClientUnprotectMessageOptions(data.size, inBuf, outSize)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatClient_UnprotectMessage",
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

    // endregion

    // region PeerToPeer mode

    public fun addNotifyMessageToPeer(callback: (MessageToPeerInfo) -> Unit): NotificationHandle {
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
            callback(MessageToPeerInfo(clientHandle, bytes))
        }
        val handle = CallbackStubs.register(invoker)
        val options = AntiCheatClientAddNotifyMessageToPeerOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_AntiCheatClient_AddNotifyMessageToPeer",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyMessageToPeer(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_AntiCheatClient_RemoveNotifyMessageToPeer",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun addNotifyPeerActionRequired(
        callback: (PeerActionRequiredInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val clientHandle = ClientHandle(data.getInt64(16))
            val action = ClientAction.fromValue(data.getInt32(24))
            val reasonCode = ClientActionReason.fromValue(data.getInt32(28))
            val reasonStrPtr = data.get(ValueLayout.ADDRESS, 32)
            val reasonString = if (reasonStrPtr.address() == 0L) "" else
                reasonStrPtr.reinterpret(Long.MAX_VALUE).getString(0)
            callback(PeerActionRequiredInfo(clientHandle, action, reasonCode, reasonString))
        }
        val handle = CallbackStubs.register(invoker)
        val options = AntiCheatClientAddNotifyPeerActionRequiredOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_AntiCheatClient_AddNotifyPeerActionRequired",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyPeerActionRequired(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_AntiCheatClient_RemoveNotifyPeerActionRequired",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun addNotifyPeerAuthStatusChanged(
        callback: (PeerAuthStatusChangedInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val clientHandle = ClientHandle(data.getInt64(16))
            val status = ClientAuthStatus.fromValue(data.getInt32(24))
            callback(PeerAuthStatusChangedInfo(clientHandle, status))
        }
        val handle = CallbackStubs.register(invoker)
        val options = AntiCheatClientAddNotifyPeerAuthStatusChangedOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_AntiCheatClient_AddNotifyPeerAuthStatusChanged",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyPeerAuthStatusChanged(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_AntiCheatClient_RemoveNotifyPeerAuthStatusChanged",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun registerPeer(
        peerHandle: ClientHandle,
        clientType: ClientType,
        clientPlatform: ClientPlatform = ClientPlatform.Unknown,
        authenticationTimeoutSeconds: Int = 60,
        ipAddress: String? = null,
        peerProductUserId: ProductUserId = ProductUserId.Invalid,
    ): EosResult = withCallArena { arena ->
        val options = AntiCheatClientRegisterPeerOptions(
            peerHandle, clientType, clientPlatform, authenticationTimeoutSeconds,
            ipAddress, peerProductUserId,
        )
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatClient_RegisterPeer",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun unregisterPeer(peerHandle: ClientHandle): EosResult = withCallArena { arena ->
        val options = AntiCheatClientUnregisterPeerOptions(peerHandle)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatClient_UnregisterPeer",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun receiveMessageFromPeer(peerHandle: ClientHandle, data: ByteArray): EosResult = withCallArena { arena ->
        val buf = arena.allocate(data.size.toLong())
        buf.copyFrom(MemorySegment.ofArray(data))
        val options = AntiCheatClientReceiveMessageFromPeerOptions(peerHandle, data.size, buf)
        EosResult.fromValue(
            Native.invoke(
                "EOS_AntiCheatClient_ReceiveMessageFromPeer",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun addNotifyClientIntegrityViolated(
        callback: (ClientIntegrityViolatedInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val violationType = EosAntiCheatClientViolationType.fromValue(data.getInt32(16))
            val msgPtr = data.get(ValueLayout.ADDRESS, 24)
            val message = if (msgPtr.address() == 0L) "" else
                msgPtr.reinterpret(Long.MAX_VALUE).getString(0)
            callback(ClientIntegrityViolatedInfo(violationType, message))
        }
        val handle = CallbackStubs.register(invoker)
        val options = AntiCheatClientAddNotifyClientIntegrityViolatedOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_AntiCheatClient_AddNotifyClientIntegrityViolated",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyClientIntegrityViolated(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_AntiCheatClient_RemoveNotifyClientIntegrityViolated",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    // endregion

    public companion object {
        public const val MAX_MESSAGE_SIZE: Int = 512
        public const val MIN_AUTHENTICATION_TIMEOUT: Int = 40
        public const val MAX_AUTHENTICATION_TIMEOUT: Int = 120
        public const val PEER_SELF_RAW: Long = -1L
    }
}

/** Anti-Cheat operating mode. */
public enum class EosAntiCheatClientMode(val value: Int) {
    Invalid(0),
    ClientServer(1),
    PeerToPeer(2);

    public companion object {
        public fun fromValue(v: Int): EosAntiCheatClientMode = entries.firstOrNull { it.value == v } ?: Invalid
    }
}

/** Type of a client-side integrity violation. */
public enum class EosAntiCheatClientViolationType(val value: Int) {
    Invalid(0),
    IntegrityCatalogNotFound(1),
    IntegrityCatalogError(2),
    IntegrityCatalogCertificateRevoked(3),
    IntegrityCatalogMissingMainExecutable(4),
    GameFileMismatch(5),
    RequiredGameFileNotFound(6),
    UnknownGameFileForbidden(7),
    SystemFileUntrusted(8),
    ForbiddenModuleLoaded(9),
    CorruptedMemory(10),
    ForbiddenToolDetected(11),
    InternalAntiCheatViolation(12),
    CorruptedNetworkMessageFlow(13),
    VirtualMachineNotAllowed(14),
    ForbiddenSystemConfiguration(15);

    public companion object {
        public fun fromValue(v: Int): EosAntiCheatClientViolationType =
            entries.firstOrNull { it.value == v } ?: Invalid
    }
}

public class MessageToServerInfo(public val data: ByteArray)
public class MessageToPeerInfo(public val peerHandle: ClientHandle, public val data: ByteArray)
public class PeerActionRequiredInfo(
    public val peerHandle: ClientHandle,
    public val action: ClientAction,
    public val reasonCode: ClientActionReason,
    public val reasonString: String,
)
public class PeerAuthStatusChangedInfo(
    public val peerHandle: ClientHandle,
    public val newStatus: ClientAuthStatus,
)
public class ClientIntegrityViolatedInfo(
    public val violationType: EosAntiCheatClientViolationType,
    public val message: String,
)
public class ProtectedMessage(public val data: ByteArray)

// region Struct writers

internal class AntiCheatClientBeginSessionOptions(
    var localUserId: ProductUserId,
    var mode: EosAntiCheatClientMode,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 3)
        seg.setInt64(8, localUserId.raw)
        seg.setInt32(16, mode.value)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT,
        )
    }
}

internal class AntiCheatClientEndSessionOptions : StructWriter {
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

internal class AntiCheatClientAddExternalIntegrityCatalogOptions(var path: String) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, arena.allocCString(path).address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
        )
    }
}

internal class AntiCheatClientGetModuleBuildIdOptions : StructWriter {
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

internal class AntiCheatClientAddNotifyMessageToServerOptions : StructWriter {
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

internal class AntiCheatClientReceiveMessageFromServerOptions(
    var dataLengthBytes: Int,
    var data: MemorySegment,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt32(8, dataLengthBytes)
        seg.setInt64(16, data.address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS,
        )
    }
}

internal class AntiCheatClientGetProtectMessageOutputLengthOptions(var dataLengthBytes: Int) : StructWriter {
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

internal class AntiCheatClientProtectMessageOptions(
    var dataLengthBytes: Int,
    var data: MemorySegment,
    var outBufferSizeBytes: Int,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt32(8, dataLengthBytes)
        seg.setInt64(16, data.address())
        seg.setInt32(24, outBufferSizeBytes)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

internal class AntiCheatClientUnprotectMessageOptions(
    var dataLengthBytes: Int,
    var data: MemorySegment,
    var outBufferSizeBytes: Int,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt32(8, dataLengthBytes)
        seg.setInt64(16, data.address())
        seg.setInt32(24, outBufferSizeBytes)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

internal class AntiCheatClientAddNotifyMessageToPeerOptions : StructWriter {
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

internal class AntiCheatClientAddNotifyPeerActionRequiredOptions : StructWriter {
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

internal class AntiCheatClientAddNotifyPeerAuthStatusChangedOptions : StructWriter {
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

internal class AntiCheatClientAddNotifyClientIntegrityViolatedOptions : StructWriter {
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

internal class AntiCheatClientRegisterPeerOptions(
    var peerHandle: ClientHandle,
    var clientType: ClientType,
    var clientPlatform: ClientPlatform,
    var authenticationTimeoutSeconds: Int,
    var ipAddress: String?,
    var peerProductUserId: ProductUserId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 3)
        seg.setInt64(8, peerHandle.raw)
        seg.setInt32(16, clientType.value)
        seg.setInt32(20, clientPlatform.value)
        seg.setInt32(24, authenticationTimeoutSeconds)
        seg.setInt64(32, 0L) // deprecated AccountId
        seg.setInt64(40, arena.allocCString(ipAddress).address())
        seg.setInt64(48, peerProductUserId.raw)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
            MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
        )
    }
}

internal class AntiCheatClientUnregisterPeerOptions(var peerHandle: ClientHandle) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, peerHandle.raw)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
        )
    }
}

internal class AntiCheatClientReceiveMessageFromPeerOptions(
    var peerHandle: ClientHandle,
    var dataLengthBytes: Int,
    var data: MemorySegment,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, peerHandle.raw)
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

// endregion
