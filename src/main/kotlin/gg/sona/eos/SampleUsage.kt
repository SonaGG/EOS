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
package gg.sona.eos

import gg.sona.eos.anticheat.common.EosAntiCheatCommon
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientPlatform
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientType
import gg.sona.eos.common.EosExternalCredentialType
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.logging.EosLogCategory
import gg.sona.eos.logging.EosLogLevel
import gg.sona.eos.logging.EosLogging
import gg.sona.eos.p2p.EosP2PSocketId
import gg.sona.eos.p2p.EosPacketReliability
import gg.sona.eos.p2p.PeerConnectionEstablishedInfo
import gg.sona.eos.rtc.DisconnectedInfo
import gg.sona.eos.rtc.EosRtcParticipantStatus
import gg.sona.eos.rtc.JoinRoomResult
import gg.sona.eos.rtc.ParticipantStatusChangedInfo

/**
 * A worked example showing the typical lifecycle of an EOS game client and
 * the priority subsystems (P2P, RTC, Anti-Cheat). This file is provided for
 * reference; the actual game would split this across the main loop and
 * individual feature modules.
 */
@Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")
object SampleUsage {

    /**
     * Typical game-client bootstrap. Call once at process start before
     * loading any other engine systems.
     */
    fun bootstrapClient(productName: String, productVersion: String) {
        // 1. Initialize the SDK. The result code indicates success.
        val initResult = Eos.initialize(
            EosInitializeOptions.create(productName, productVersion)
        )
        check(initResult == EosResult.Success) { "EOS init failed: $initResult" }

        // 2. Optional: receive SDK log messages.
        EosLogging.setCallback { msg ->
            // Forward to your logger.
            println("[EOS ${msg.category}] ${msg.level}: ${msg.message}")
        }
        EosLogging.setLogLevel(EosLogCategory.AllCategories, EosLogLevel.Verbose)

        // 3. Create the platform instance. Hold on to it for the lifetime
        //    of the game.
        val platform = Eos.createPlatform(
            EosPlatformOptions.create(
                productId = "11111111111111111111111111111111",
                sandboxId = "22222222222222222222222222222222",
                deploymentId = "33333333333333333333333333333333",
                clientCredentials = EosClientCredentials.of(
                    clientId = "xyz",
                    clientSecret = "abc",
                ),
            )
        )

        // 4. Log in via Connect (external account). The credentials used
        //    here are for an Epic Account Services login; for Steam/PSN/etc
        //    use the matching EosExternalCredentialType.
        val loginResult = platform.connect.login(
            credentialType = EosExternalCredentialType.Epic,
            token = "<your-token-here>",
        ).join()
        val userId = loginResult.localUserId

        // 5. Set up the main loop. The platform must be ticked regularly.
        val tickThread = Thread {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    platform.tick()
                    Thread.sleep(10)
                }
            } finally {
                platform.close()
                Eos.shutdown()
            }
        }
        tickThread.start()
    }

    /**
     * Demonstrates the P2P API: send a packet and listen for connection
     * events.
     */
    fun p2pExample(platform: EosPlatform, self: ProductUserId, other: ProductUserId) {
        val socket = EosP2PSocketId("gameplay-channel")

        // Listen for established connections.
        val onEstablished = platform.p2p.addNotifyPeerConnectionEstablished(
            localUserId = self,
            socketId = socket,
        ) { info: PeerConnectionEstablishedInfo ->
            println("Connection established with ${info.remoteUserId}: ${info.networkType}")
        }

        // Send a packet. EOS will establish a connection on demand.
        platform.p2p.sendPacket(
            localUserId = self,
            remoteUserId = other,
            socketId = socket,
            channel = 0,
            data = byteArrayOf(0x1, 0x2, 0x3, 0x4),
            reliability = EosPacketReliability.ReliableOrdered,
        )

        // Clean up.
        platform.p2p.removeNotifyPeerConnectionEstablished(onEstablished)
    }

    /**
     * Demonstrates the RTC API: join a voice room, listen for participant
     * status changes, and clean up.
     */
    fun rtcExample(
        platform: EosPlatform,
        self: ProductUserId,
        roomName: String,
        clientBaseUrl: String,
        participantToken: String,
    ) {
        val join: JoinRoomResult = platform.rtc.joinRoom(
            localUserId = self,
            roomName = roomName,
            clientBaseUrl = clientBaseUrl,
            participantToken = participantToken,
        ).join()

        // Listen for other participants.
        val onParticipants = platform.rtc.addNotifyParticipantStatusChanged(
            localUserId = self,
            roomName = roomName,
        ) { info: ParticipantStatusChangedInfo ->
            when (info.status) {
                EosRtcParticipantStatus.Joined ->
                    println("User ${info.participantId} joined room ${info.roomName}")

                EosRtcParticipantStatus.Left ->
                    println("User ${info.participantId} left room ${info.roomName}")
            }
        }

        val onDisconnected = platform.rtc.addNotifyDisconnected(
            localUserId = self,
            roomName = roomName,
        ) { info: DisconnectedInfo ->
            println("Disconnected from ${info.roomName}: ${info.result}")
        }

        // ... game loop runs ...

        // Clean up.
        platform.rtc.removeNotifyParticipantStatusChanged(onParticipants)
        platform.rtc.removeNotifyDisconnected(onDisconnected)
        platform.rtc.leaveRoom(self, roomName).join()
    }

    /**
     * Demonstrates the Anti-Cheat Client API in dedicated-server mode.
     * Sends anti-cheat messages to the server and listens for integrity
     * violations.
     */
    fun antiCheatClientExample(platform: EosPlatform, self: ProductUserId) {
        val mode = gg.sona.eos.anticheat.client.EosAntiCheatClientMode.ClientServer
        val beginResult = platform.antiCheatClient.beginSession(self, mode)
        check(beginResult == EosResult.Success) { "beginSession failed: $beginResult" }

        val onServerMessage = platform.antiCheatClient.addNotifyMessageToServer { info ->
            // Forward info.data to the game server.
        }

        val onIntegrityViolation = platform.antiCheatClient.addNotifyClientIntegrityViolated { info ->
            // Display info.message to the player.
        }

        // ... play game ...

        // Clean up.
        platform.antiCheatClient.removeNotifyMessageToServer(onServerMessage)
        platform.antiCheatClient.removeNotifyClientIntegrityViolated(onIntegrityViolation)
        platform.antiCheatClient.endSession()
    }

    /**
     * Demonstrates the Anti-Cheat Server API. Runs in a dedicated server
     * process. Receives anti-cheat messages from clients and logs gameplay
     * events.
     */
    fun antiCheatServerExample(platform: EosPlatform, localUser: ProductUserId) {
        val beginResult = platform.antiCheatServer.beginSession(
            registerTimeoutSeconds = 60,
            serverName = "dedicated-1",
            enableGameplayData = true,
            localUserId = localUser,
        )
        check(beginResult == EosResult.Success) { "server beginSession failed: $beginResult" }

        // Receive messages from a connected client.
        val onClientMessage = platform.antiCheatServer.addNotifyMessageToClient { info ->
            // Forward info.data to the client identified by info.clientHandle.
        }

        // Register a connected client.
        val playerHandle = EosAntiCheatCommon.ClientHandle(0xDEADBEEFL)
        platform.antiCheatServer.registerClient(
            clientHandle = playerHandle,
            clientType = ClientType.ProtectedClient,
            clientPlatform = ClientPlatform.Windows,
            userId = localUser,
        )

        // Log a player tick.
        platform.antiCheatServer.logPlayerTick(
            playerHandle = playerHandle,
            position = EosAntiCheatCommon.Vec3f(0f, 0f, 100f),
            viewRotation = EosAntiCheatCommon.Quat(1f, 0f, 0f, 0f),
            isViewZoomed = false,
            health = 100f,
        )

        // ... handle other events ...

        // Clean up.
        platform.antiCheatServer.removeNotifyMessageToClient(onClientMessage)
        platform.antiCheatServer.unregisterClient(playerHandle)
        platform.antiCheatServer.endSession()
    }
}
