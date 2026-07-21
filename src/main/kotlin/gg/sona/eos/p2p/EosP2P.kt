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
package gg.sona.eos.p2p

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

/**
 * P2P (peer-to-peer) interface. Used to send and receive arbitrary packets
 * between users, with NAT punching and Epic relay fallback. Up to 32 socket
 * ids can be opened with a single remote user.
 */
public class EosP2P internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetP2PInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /**
     * Send a packet to a peer. If no connection exists, one is established on
     * the fly. An [EosResult.Success] only means the packet was queued for
     * sending, not that it was actually delivered.
     */
    public fun sendPacket(
        localUserId: ProductUserId,
        remoteUserId: ProductUserId,
        socketId: EosP2PSocketId,
        channel: Int,
        data: ByteArray,
        reliability: EosPacketReliability,
        allowDelayedDelivery: Boolean = true,
        disableAutoAcceptConnection: Boolean = false,
    ): EosResult {
        require(data.size <= MAX_PACKET_SIZE) { "P2P packet exceeds maximum size of $MAX_PACKET_SIZE" }
        require(channel in 0..255) { "channel must be in 0..255" }
        return withCallArena { arena ->
            val bytes = arena.allocate(data.size.toLong())
            bytes.copyFrom(MemorySegment.ofArray(data))
            val socketSeg = socketId.writeTo(arena)
            val options = P2PSendPacketOptions(
                localUserId, remoteUserId, socketSeg, channel.toByte(),
                bytes, data.size, allowDelayedDelivery, reliability, disableAutoAcceptConnection,
            )
            EosResult.fromValue(
                Native.invoke(
                    "EOS_P2P_SendPacket",
                    listOf(handle(), options.writeTo(arena)),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
        }
    }

    /**
     * Get the size of the next packet waiting to be received for the given
     * user. Returns null if no packet is available.
     */
    public fun getNextReceivedPacketSize(
        localUserId: ProductUserId,
        channel: Int? = null,
    ): Int? = withCallArena { arena ->
        val sizePtr = arena.allocate(ValueLayout.JAVA_INT)
        val options = P2PGetNextReceivedPacketSizeOptions(localUserId, channel)
        val result = EosResult.fromValue(
            Native.invoke(
                "EOS_P2P_GetNextReceivedPacketSize",
                listOf(handle(), options.writeTo(arena), sizePtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        if (result == EosResult.NotFound || result == EosResult.Success) {
            sizePtr.get(ValueLayout.JAVA_INT, 0)
        } else null
    }

    /**
     * Receive the next packet for the local user. Returns null if no packet
     * is available.
     */
    public fun receivePacket(
        localUserId: ProductUserId,
        maxDataSize: Int = MAX_PACKET_SIZE,
        channel: Int? = null,
    ): P2PReceivedPacket? = withCallArena { arena ->
        val options = P2PReceivePacketOptions(localUserId, maxDataSize, channel)
        val peerIdPtr = arena.allocate(ValueLayout.JAVA_LONG)
        val socketIdPtr = arena.allocate(ValueLayout.ADDRESS)
        val channelPtr = arena.allocate(ValueLayout.JAVA_BYTE)
        val dataBuf = arena.allocate(maxDataSize.toLong())
        val bytesWrittenPtr = arena.allocate(ValueLayout.JAVA_INT)
        val result = EosResult.fromValue(
            Native.invoke(
                "EOS_P2P_ReceivePacket",
                listOf(handle(), options.writeTo(arena), peerIdPtr, socketIdPtr, channelPtr, dataBuf, bytesWrittenPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        if (result != EosResult.Success) return@withCallArena null
        val peerId = ProductUserId(peerIdPtr.get(ValueLayout.JAVA_LONG, 0))
        val socketIdPtrVal = socketIdPtr.get(ValueLayout.ADDRESS, 0)
        val socketName = if (socketIdPtrVal.address() == 0L) "" else
            socketIdPtrVal.reinterpret(Long.MAX_VALUE).getString(0)
        val receivedChannel = channelPtr.get(ValueLayout.JAVA_BYTE, 0).toInt() and 0xff
        val bytesWritten = bytesWrittenPtr.get(ValueLayout.JAVA_INT, 0)
        val bytes = ByteArray(bytesWritten)
        MemorySegment.ofArray(bytes).copyFrom(MemorySegment.ofAddress(dataBuf.address()).reinterpret(bytesWritten.toLong()))
        P2PReceivedPacket(peerId, EosP2PSocketId(socketName), receivedChannel, bytes)
    }

    /**
     * Accept (or request) a connection with a peer. Subsequent packets to that
     * peer on the same socket will flow without further accept calls.
     */
    public fun acceptConnection(
        localUserId: ProductUserId,
        remoteUserId: ProductUserId,
        socketId: EosP2PSocketId,
    ): EosResult = withCallArena { arena ->
        val socketSeg = socketId.writeTo(arena)
        val options = P2PAcceptConnectionOptions(localUserId, remoteUserId, socketSeg)
        EosResult.fromValue(
            Native.invoke(
                "EOS_P2P_AcceptConnection",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    /**
     * Close the connection with a peer on a specific socket id. If the
     * connection has other open sockets, the underlying physical socket is
     * preserved.
     */
    public fun closeConnection(
        localUserId: ProductUserId,
        remoteUserId: ProductUserId,
        socketId: EosP2PSocketId,
    ): EosResult = withCallArena { arena ->
        val socketSeg = socketId.writeTo(arena)
        val options = P2PCloseConnectionOptions(localUserId, remoteUserId, socketSeg)
        EosResult.fromValue(
            Native.invoke(
                "EOS_P2P_CloseConnection",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun closeConnections(localUserId: ProductUserId, socketId: EosP2PSocketId): EosResult =
        withCallArena { arena ->
            val socketSeg = socketId.writeTo(arena)
            val options = P2PCloseConnectionsOptions(localUserId, socketSeg)
            EosResult.fromValue(
                Native.invoke(
                    "EOS_P2P_CloseConnections",
                    listOf(handle(), options.writeTo(arena)),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
        }

    public fun queryNATType(): CompletableFuture<QueryNatTypeResult> {
        val future = CompletableFuture<QueryNatTypeResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val nat = EosNatType.fromValue(data.getInt32(24))
            future.complete(QueryNatTypeResult(result, nat))
        })
        val options = P2PQueryNATTypeOptions()
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_P2P_QueryNATType",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun getNATType(): EosNatType? = withCallArena { arena ->
        val outPtr = arena.allocate(ValueLayout.JAVA_INT)
        val options = P2PGetNATTypeOptions()
        val result = EosResult.fromValue(
            Native.invoke(
                "EOS_P2P_GetNATType",
                listOf(handle(), options.writeTo(arena), outPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        if (result != EosResult.Success) null else EosNatType.fromValue(outPtr.get(ValueLayout.JAVA_INT, 0))
    }

    public fun setRelayControl(relayControl: EosRelayControl): EosResult = withCallArena { arena ->
        val options = P2PSetRelayControlOptions(relayControl)
        EosResult.fromValue(
            Native.invoke(
                "EOS_P2P_SetRelayControl",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun getRelayControl(): EosRelayControl = withCallArena { arena ->
        val outPtr = arena.allocate(ValueLayout.JAVA_INT)
        val options = P2PGetRelayControlOptions()
        EosResult.fromValue(
            Native.invoke(
                "EOS_P2P_GetRelayControl",
                listOf(handle(), options.writeTo(arena), outPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        EosRelayControl.fromValue(outPtr.get(ValueLayout.JAVA_INT, 0))
    }

    public fun setPortRange(port: Int, additionalPorts: Int): EosResult = withCallArena { arena ->
        val options = P2PSetPortRangeOptions(port.toShort(), additionalPorts.toShort())
        EosResult.fromValue(
            Native.invoke(
                "EOS_P2P_SetPortRange",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun getPortRange(): PortRange = withCallArena { arena ->
        val portPtr = arena.allocate(ValueLayout.JAVA_SHORT)
        val additionalPtr = arena.allocate(ValueLayout.JAVA_SHORT)
        val options = P2PGetPortRangeOptions()
        EosResult.fromValue(
            Native.invoke(
                "EOS_P2P_GetPortRange",
                listOf(handle(), options.writeTo(arena), portPtr, additionalPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        PortRange(portPtr.get(ValueLayout.JAVA_SHORT, 0).toInt() and 0xffff,
                  additionalPtr.get(ValueLayout.JAVA_SHORT, 0).toInt() and 0xffff)
    }

    public fun setPacketQueueSize(incomingMaxBytes: Long, outgoingMaxBytes: Long): EosResult =
        withCallArena { arena ->
            val options = P2PSetPacketQueueSizeOptions(incomingMaxBytes, outgoingMaxBytes)
            EosResult.fromValue(
                Native.invoke(
                    "EOS_P2P_SetPacketQueueSize",
                    listOf(handle(), options.writeTo(arena)),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
        }

    public fun getPacketQueueInfo(): PacketQueueInfo = withCallArena { arena ->
        val outPtr = arena.allocate(PacketQueueInfo.LAYOUT)
        val options = P2PGetPacketQueueInfoOptions()
        EosResult.fromValue(
            Native.invoke(
                "EOS_P2P_GetPacketQueueInfo",
                listOf(handle(), options.writeTo(arena), outPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        PacketQueueInfo(
            incomingMaxSizeBytes = outPtr.getInt64(0),
            incomingCurrentSizeBytes = outPtr.getInt64(8),
            incomingCurrentPacketCount = outPtr.getInt64(16),
            outgoingMaxSizeBytes = outPtr.getInt64(24),
            outgoingCurrentSizeBytes = outPtr.getInt64(32),
            outgoingCurrentPacketCount = outPtr.getInt64(40),
        )
    }

    public fun addNotifyPeerConnectionRequest(
        localUserId: ProductUserId,
        socketId: EosP2PSocketId?,
        callback: (PeerConnectionRequestInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(16))
            val remoteUserId = ProductUserId(data.getInt64(24))
            val socketPtr = data.get(ValueLayout.ADDRESS, 32)
            val socketName = if (socketPtr.address() == 0L) "" else
                socketPtr.reinterpret(Long.MAX_VALUE).getString(0)
            callback(PeerConnectionRequestInfo(localUserId, remoteUserId, EosP2PSocketId(socketName)))
        }
        val handle = CallbackStubs.register(invoker)
        val options = P2PAddNotifyPeerConnectionRequestOptions(localUserId, socketId)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_P2P_AddNotifyPeerConnectionRequest",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyPeerConnectionRequest(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_P2P_RemoveNotifyPeerConnectionRequest",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun addNotifyPeerConnectionEstablished(
        localUserId: ProductUserId,
        socketId: EosP2PSocketId?,
        callback: (PeerConnectionEstablishedInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(16))
            val remoteUserId = ProductUserId(data.getInt64(24))
            val socketPtr = data.get(ValueLayout.ADDRESS, 32)
            val socketName = if (socketPtr.address() == 0L) "" else
                socketPtr.reinterpret(Long.MAX_VALUE).getString(0)
            val type = EosConnectionEstablishedType.fromValue(data.getInt32(40))
            val netType = EosNetworkConnectionType.fromValue(data.getInt32(44))
            callback(PeerConnectionEstablishedInfo(localUserId, remoteUserId, EosP2PSocketId(socketName), type, netType))
        }
        val handle = CallbackStubs.register(invoker)
        val options = P2PAddNotifyPeerConnectionEstablishedOptions(localUserId, socketId)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_P2P_AddNotifyPeerConnectionEstablished",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyPeerConnectionEstablished(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_P2P_RemoveNotifyPeerConnectionEstablished",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun addNotifyPeerConnectionInterrupted(
        localUserId: ProductUserId,
        socketId: EosP2PSocketId?,
        callback: (PeerConnectionInterruptedInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(16))
            val remoteUserId = ProductUserId(data.getInt64(24))
            val socketPtr = data.get(ValueLayout.ADDRESS, 32)
            val socketName = if (socketPtr.address() == 0L) "" else
                socketPtr.reinterpret(Long.MAX_VALUE).getString(0)
            callback(PeerConnectionInterruptedInfo(localUserId, remoteUserId, EosP2PSocketId(socketName)))
        }
        val handle = CallbackStubs.register(invoker)
        val options = P2PAddNotifyPeerConnectionInterruptedOptions(localUserId, socketId)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_P2P_AddNotifyPeerConnectionInterrupted",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyPeerConnectionInterrupted(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_P2P_RemoveNotifyPeerConnectionInterrupted",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun addNotifyPeerConnectionClosed(
        localUserId: ProductUserId,
        socketId: EosP2PSocketId?,
        callback: (PeerConnectionClosedInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(16))
            val remoteUserId = ProductUserId(data.getInt64(24))
            val socketPtr = data.get(ValueLayout.ADDRESS, 32)
            val socketName = if (socketPtr.address() == 0L) "" else
                socketPtr.reinterpret(Long.MAX_VALUE).getString(0)
            val reason = EosConnectionClosedReason.fromValue(data.getInt32(40))
            callback(PeerConnectionClosedInfo(localUserId, remoteUserId, EosP2PSocketId(socketName), reason))
        }
        val handle = CallbackStubs.register(invoker)
        val options = P2PAddNotifyPeerConnectionClosedOptions(localUserId, socketId)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_P2P_AddNotifyPeerConnectionClosed",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyPeerConnectionClosed(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_P2P_RemoveNotifyPeerConnectionClosed",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun addNotifyIncomingPacketQueueFull(
        callback: (IncomingPacketQueueFullInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val maxSize = data.getInt64(16)
            val currentSize = data.getInt64(24)
            val localUserId = ProductUserId(data.getInt64(32))
            val channel = data.get(ValueLayout.JAVA_BYTE, 40).toInt() and 0xff
            val packetSize = data.getInt32(44).toLong() and 0xffffffffL
            callback(IncomingPacketQueueFullInfo(maxSize, currentSize, localUserId, channel, packetSize))
        }
        val handle = CallbackStubs.register(invoker)
        val options = P2PAddNotifyIncomingPacketQueueFullOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_P2P_AddNotifyIncomingPacketQueueFull",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyIncomingPacketQueueFull(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_P2P_RemoveNotifyIncomingPacketQueueFull",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun clearPacketQueue(
        localUserId: ProductUserId,
        remoteUserId: ProductUserId,
        socketId: EosP2PSocketId?,
    ): EosResult = withCallArena { arena ->
        val socketSeg = socketId?.writeTo(arena) ?: MemorySegment.NULL
        val options = P2PClearPacketQueueOptions(localUserId, remoteUserId, socketSeg)
        EosResult.fromValue(
            Native.invoke(
                "EOS_P2P_ClearPacketQueue",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public companion object {
        public const val MAX_PACKET_SIZE: Int = 1170
        public const val MAX_CONNECTIONS: Int = 32
        public const val SOCKET_NAME_SIZE: Int = 33
        public const val MAX_QUEUE_SIZE_UNLIMITED: Long = 0L
    }
}

/** Packet reliability mode. */
public enum class EosPacketReliability(val value: Int) {
    UnreliableUnordered(0),
    ReliableUnordered(1),
    ReliableOrdered(2);

    public companion object {
        public fun fromValue(v: Int): EosPacketReliability = entries.firstOrNull { it.value == v } ?: UnreliableUnordered
    }
}

/** NAT type as reported by the P2P subsystem. */
public enum class EosNatType(val value: Int) {
    Unknown(0),
    Open(1),
    Moderate(2),
    Strict(3);

    public companion object {
        public fun fromValue(v: Int): EosNatType = entries.firstOrNull { it.value == v } ?: Unknown
    }
}

/** Type of an established connection. */
public enum class EosConnectionEstablishedType(val value: Int) {
    NewConnection(0),
    Reconnection(1);

    public companion object {
        public fun fromValue(v: Int): EosConnectionEstablishedType = entries.firstOrNull { it.value == v } ?: NewConnection
    }
}

/** Network connection type for a P2P connection. */
public enum class EosNetworkConnectionType(val value: Int) {
    NoConnection(0),
    DirectConnection(1),
    RelayedConnection(2);

    public companion object {
        public fun fromValue(v: Int): EosNetworkConnectionType = entries.firstOrNull { it.value == v } ?: NoConnection
    }
}

/** Reason a P2P connection was closed. */
public enum class EosConnectionClosedReason(val value: Int) {
    Unknown(0),
    ClosedByLocalUser(1),
    ClosedByPeer(2),
    TimedOut(3),
    TooManyConnections(4),
    InvalidMessage(5),
    InvalidData(6),
    ConnectionFailed(7),
    ConnectionClosed(8),
    NegotiationFailed(9),
    UnexpectedError(10),
    ConnectionIgnored(11);

    public companion object {
        public fun fromValue(v: Int): EosConnectionClosedReason = entries.firstOrNull { it.value == v } ?: Unknown
    }
}

/** Relay server usage policy. */
public enum class EosRelayControl(val value: Int) {
    NoRelays(0),
    AllowRelays(1),
    ForceRelays(2);

    public companion object {
        public fun fromValue(v: Int): EosRelayControl = entries.firstOrNull { it.value == v } ?: AllowRelays
    }
}

/**
 * P2P Socket ID. Socket names must be 1-32 alphanumeric characters (A-Z, a-z,
 * 0-9, '-', '_', ' ', '+', '=', '.') and may be used as a secret to gate
 * which connections are accepted.
 */
public class EosP2PSocketId(public val name: String) {
    init {
        require(name.length in 0..32) { "P2P socket name must be 0-32 characters" }
    }

    internal fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        val bytes = name.toByteArray(Charsets.UTF_8)
        for (i in 0 until 32) {
            seg.set(ValueLayout.JAVA_BYTE, 4L + i, if (i < bytes.size) bytes[i] else 0)
        }
        return seg
    }

    public companion object {
        internal val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(0), ValueLayout.JAVA_BYTE
        ).withByteAlignment(1)
            // The fixed-size char[33] is laid out as 33 bytes
            .withName("EOS_P2P_SocketId")
    }
}

public class P2PReceivedPacket(
    public val remoteUserId: ProductUserId,
    public val socketId: EosP2PSocketId,
    public val channel: Int,
    public val data: ByteArray,
)

public class QueryNatTypeResult(
    public val result: EosResult,
    public val natType: EosNatType,
)

public class PortRange(public val port: Int, public val additionalPorts: Int)

public class PacketQueueInfo(
    public val incomingMaxSizeBytes: Long,
    public val incomingCurrentSizeBytes: Long,
    public val incomingCurrentPacketCount: Long,
    public val outgoingMaxSizeBytes: Long,
    public val outgoingCurrentSizeBytes: Long,
    public val outgoingCurrentPacketCount: Long,
) {
    internal companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}

public class PeerConnectionRequestInfo(
    public val localUserId: ProductUserId,
    public val remoteUserId: ProductUserId,
    public val socketId: EosP2PSocketId,
)

public class PeerConnectionEstablishedInfo(
    public val localUserId: ProductUserId,
    public val remoteUserId: ProductUserId,
    public val socketId: EosP2PSocketId,
    public val type: EosConnectionEstablishedType,
    public val networkType: EosNetworkConnectionType,
)

public class PeerConnectionInterruptedInfo(
    public val localUserId: ProductUserId,
    public val remoteUserId: ProductUserId,
    public val socketId: EosP2PSocketId,
)

public class PeerConnectionClosedInfo(
    public val localUserId: ProductUserId,
    public val remoteUserId: ProductUserId,
    public val socketId: EosP2PSocketId,
    public val reason: EosConnectionClosedReason,
)

public class IncomingPacketQueueFullInfo(
    public val maxSizeBytes: Long,
    public val currentSizeBytes: Long,
    public val localUserId: ProductUserId,
    public val channel: Int,
    public val packetSizeBytes: Long,
)

// region Internal struct writers

internal class P2PSendPacketOptions(
    var localUserId: ProductUserId,
    var remoteUserId: ProductUserId,
    var socketId: MemorySegment,
    var channel: Byte,
    var data: MemorySegment,
    var dataLengthBytes: Int,
    var allowDelayedDelivery: Boolean,
    var reliability: EosPacketReliability,
    var disableAutoAcceptConnection: Boolean,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 3)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, remoteUserId.raw)
        seg.setInt64(24, socketId.address())
        seg.set(ValueLayout.JAVA_BYTE, 32, channel)
        seg.setInt32(36, dataLengthBytes)
        seg.setInt64(40, data.address())
        seg.setInt32(48, if (allowDelayedDelivery) 1 else 0)
        seg.setInt32(52, reliability.value)
        seg.setInt32(56, if (disableAutoAcceptConnection) 1 else 0)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
            ValueLayout.JAVA_BYTE, MemoryLayout.paddingLayout(3),
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}

internal class P2PGetNextReceivedPacketSizeOptions(
    var localUserId: ProductUserId,
    var channel: Int?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 2)
        seg.setInt64(8, localUserId.raw)
        if (channel != null) {
            val chValue: Int = channel!!
            val ch = arena.allocate(ValueLayout.JAVA_BYTE)
            ch.set(ValueLayout.JAVA_BYTE, 0, chValue.toByte())
            seg.setInt64(16, ch.address())
        } else {
            seg.setInt64(16, 0L)
        }
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class P2PReceivePacketOptions(
    var localUserId: ProductUserId,
    var maxDataSizeBytes: Int,
    var channel: Int?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 2)
        seg.setInt64(8, localUserId.raw)
        seg.setInt32(16, maxDataSizeBytes)
        if (channel != null) {
            val chValue: Int = channel!!
            val ch = arena.allocate(ValueLayout.JAVA_BYTE)
            ch.set(ValueLayout.JAVA_BYTE, 0, chValue.toByte())
            seg.setInt64(24, ch.address())
        } else {
            seg.setInt64(24, 0L)
        }
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

internal class P2PAcceptConnectionOptions(
    var localUserId: ProductUserId,
    var remoteUserId: ProductUserId,
    var socketId: MemorySegment,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, remoteUserId.raw)
        seg.setInt64(24, socketId.address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class P2PCloseConnectionOptions(
    var localUserId: ProductUserId,
    var remoteUserId: ProductUserId,
    var socketId: MemorySegment,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, remoteUserId.raw)
        seg.setInt64(24, socketId.address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class P2PCloseConnectionsOptions(
    var localUserId: ProductUserId,
    var socketId: MemorySegment,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, socketId.address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class P2PQueryNATTypeOptions : StructWriter {
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

internal class P2PGetNATTypeOptions : StructWriter {
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

internal class P2PSetRelayControlOptions(var relayControl: EosRelayControl) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt32(8, relayControl.value)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}

internal class P2PGetRelayControlOptions : StructWriter {
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

internal class P2PSetPortRangeOptions(
    var port: Short,
    var maxAdditionalPorts: Short,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.set(ValueLayout.JAVA_SHORT, 4, port)
        seg.set(ValueLayout.JAVA_SHORT, 6, maxAdditionalPorts)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_SHORT, ValueLayout.JAVA_SHORT,
        )
    }
}

internal class P2PGetPortRangeOptions : StructWriter {
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

internal class P2PSetPacketQueueSizeOptions(
    var incomingMaxSizeBytes: Long,
    var outgoingMaxSizeBytes: Long,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, incomingMaxSizeBytes)
        seg.setInt64(16, outgoingMaxSizeBytes)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}

internal class P2PGetPacketQueueInfoOptions : StructWriter {
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

internal class P2PAddNotifyPeerConnectionRequestOptions(
    var localUserId: ProductUserId,
    var socketId: EosP2PSocketId?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, socketId?.writeTo(arena)?.address() ?: 0L)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class P2PAddNotifyPeerConnectionEstablishedOptions(
    var localUserId: ProductUserId,
    var socketId: EosP2PSocketId?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, socketId?.writeTo(arena)?.address() ?: 0L)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class P2PAddNotifyPeerConnectionInterruptedOptions(
    var localUserId: ProductUserId,
    var socketId: EosP2PSocketId?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, socketId?.writeTo(arena)?.address() ?: 0L)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class P2PAddNotifyPeerConnectionClosedOptions(
    var localUserId: ProductUserId,
    var socketId: EosP2PSocketId?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, socketId?.writeTo(arena)?.address() ?: 0L)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class P2PAddNotifyIncomingPacketQueueFullOptions : StructWriter {
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

internal class P2PClearPacketQueueOptions(
    var localUserId: ProductUserId,
    var remoteUserId: ProductUserId,
    var socketId: MemorySegment,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, remoteUserId.raw)
        seg.setInt64(24, socketId.address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

// endregion
