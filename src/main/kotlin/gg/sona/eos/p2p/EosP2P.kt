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
package gg.sona.eos.p2p

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.NotificationHandle
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.*
import java.lang.foreign.FunctionDescriptor
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
        // EOS_P2P_OnQueryNATTypeCompleteInfo: ResultCode@0, ClientData@8, NATType@16
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(0))
            val nat = EosNatType.fromValue(data.getInt32(16))
            future.complete(QueryNatTypeResult(result, nat))
        })
        val options = P2PQueryNATTypeOptions()
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_P2P_QueryNATType",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
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
        // EOS_P2P_OnIncomingConnectionRequestInfo: ClientData@0, LocalUserId@8, RemoteUserId@16, SocketId@24
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(8))
            val remoteUserId = ProductUserId(data.getInt64(16))
            val socketPtr = data.get(ValueLayout.ADDRESS, 24)
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
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
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
        // EOS_P2P_OnPeerConnectionEstablishedInfo: ClientData@0, LocalUserId@8, RemoteUserId@16, SocketId@24, ConnectionType@32, NetworkType@36
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(8))
            val remoteUserId = ProductUserId(data.getInt64(16))
            val socketPtr = data.get(ValueLayout.ADDRESS, 24)
            val socketName = if (socketPtr.address() == 0L) "" else
                socketPtr.reinterpret(Long.MAX_VALUE).getString(0)
            val type = EosConnectionEstablishedType.fromValue(data.getInt32(32))
            val netType = EosNetworkConnectionType.fromValue(data.getInt32(36))
            callback(PeerConnectionEstablishedInfo(localUserId, remoteUserId, EosP2PSocketId(socketName), type, netType))
        }
        val handle = CallbackStubs.register(invoker)
        val options = P2PAddNotifyPeerConnectionEstablishedOptions(localUserId, socketId)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_P2P_AddNotifyPeerConnectionEstablished",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
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
        // EOS_P2P_OnPeerConnectionInterruptedInfo: ClientData@0, LocalUserId@8, RemoteUserId@16, SocketId@24
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(8))
            val remoteUserId = ProductUserId(data.getInt64(16))
            val socketPtr = data.get(ValueLayout.ADDRESS, 24)
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
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
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
        // EOS_P2P_OnRemoteConnectionClosedInfo: ClientData@0, LocalUserId@8, RemoteUserId@16, SocketId@24, Reason@32
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(8))
            val remoteUserId = ProductUserId(data.getInt64(16))
            val socketPtr = data.get(ValueLayout.ADDRESS, 24)
            val socketName = if (socketPtr.address() == 0L) "" else
                socketPtr.reinterpret(Long.MAX_VALUE).getString(0)
            val reason = EosConnectionClosedReason.fromValue(data.getInt32(32))
            callback(PeerConnectionClosedInfo(localUserId, remoteUserId, EosP2PSocketId(socketName), reason))
        }
        val handle = CallbackStubs.register(invoker)
        val options = P2PAddNotifyPeerConnectionClosedOptions(localUserId, socketId)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_P2P_AddNotifyPeerConnectionClosed",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
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
        // EOS_P2P_OnIncomingPacketQueueFullInfo: ClientData@0, PacketQueueMaxSizeBytes@8, PacketQueueCurrentSizeBytes@16, OverflowPacketLocalUserId@24, OverflowPacketChannel@32, OverflowPacketSizeBytes@36
        val invoker = EosCallback { data ->
            val maxSize = data.getInt64(8)
            val currentSize = data.getInt64(16)
            val localUserId = ProductUserId(data.getInt64(24))
            val channel = data.get(ValueLayout.JAVA_BYTE, 32).toInt() and 0xff
            val packetSize = data.getInt32(36).toLong() and 0xffffffffL
            callback(IncomingPacketQueueFullInfo(maxSize, currentSize, localUserId, channel, packetSize))
        }
        val handle = CallbackStubs.register(invoker)
        val options = P2PAddNotifyIncomingPacketQueueFullOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_P2P_AddNotifyIncomingPacketQueueFull",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
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