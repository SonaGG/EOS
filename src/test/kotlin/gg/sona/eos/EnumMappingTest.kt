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
package gg.sona.eos

import gg.sona.eos.anticheat.common.EosAntiCheatCommon
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientPlatform
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientType
import gg.sona.eos.common.EosAttributeType
import gg.sona.eos.common.EosComparisonOp
import gg.sona.eos.common.EosExternalAccountType
import gg.sona.eos.common.EosExternalCredentialType
import gg.sona.eos.common.EosLoginStatus
import gg.sona.eos.common.EosOnlinePlatform
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.ecom.EosOwnershipStatus
import gg.sona.eos.lobby.EosLobbyMemberStatus
import gg.sona.eos.logging.EosLogCategory
import gg.sona.eos.logging.EosLogLevel
import gg.sona.eos.p2p.EosConnectionClosedReason
import gg.sona.eos.p2p.EosConnectionEstablishedType
import gg.sona.eos.p2p.EosNatType
import gg.sona.eos.p2p.EosNetworkConnectionType
import gg.sona.eos.p2p.EosPacketReliability
import gg.sona.eos.p2p.EosRelayControl
import gg.sona.eos.rtc.EosRtcParticipantStatus
import gg.sona.eos.rtc.EosRtcAudioInputStatus
import gg.sona.eos.rtc.EosRtcAudioOutputStatus
import gg.sona.eos.rtc.EosRtcAudioStatus
import gg.sona.eos.rtc.EosRtcDataStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure-Kotlin tests that exercise enum and value-class mappings without
 * loading the native library. These tests are fast and always run.
 */
class EnumMappingTest {

    // region EosResult

    @Test
    fun `EosResult fromValue handles known codes`() {
        assertEquals(EosResult.Success, EosResult.fromValue(0))
        assertEquals(EosResult.NoConnection, EosResult.fromValue(1))
        assertEquals(EosResult.InvalidUser, EosResult.fromValue(3))
        assertEquals(EosResult.RtcTooManyParticipants, EosResult.fromValue(13000))
    }

    @Test
    fun `EosResult fromValue maps unknown to UnexpectedError`() {
        assertEquals(EosResult.UnexpectedError, EosResult.fromValue(99_999_999))
        assertEquals(EosResult.UnexpectedError, EosResult.fromValue(-1))
    }

    @Test
    fun `EosResult all named entries are unique`() {
        val seen = HashSet<Int>()
        for (entry in EosResult.entries) {
            assertTrue(seen.add(entry.value), "Duplicate value ${entry.value} for $entry")
        }
    }

    // endregion

    // region Login / platform enums

    @Test
    fun `login status maps cleanly`() {
        assertEquals(EosLoginStatus.NotLoggedIn, EosLoginStatus.fromValue(0))
        assertEquals(EosLoginStatus.UsingLocalProfile, EosLoginStatus.fromValue(1))
        assertEquals(EosLoginStatus.LoggedIn, EosLoginStatus.fromValue(2))
    }

    @Test
    fun `attribute types are stable`() {
        assertEquals(EosAttributeType.Boolean, EosAttributeType.fromValue(0))
        assertEquals(EosAttributeType.Int64, EosAttributeType.fromValue(1))
        assertEquals(EosAttributeType.Double, EosAttributeType.fromValue(2))
        assertEquals(EosAttributeType.String, EosAttributeType.fromValue(3))
    }

    @Test
    fun `comparison operators map to integers in order`() {
        listOf(
            EosComparisonOp.Equal, EosComparisonOp.NotEqual,
            EosComparisonOp.GreaterThan, EosComparisonOp.GreaterThanOrEqual,
            EosComparisonOp.LessThan, EosComparisonOp.LessThanOrEqual,
            EosComparisonOp.Nearest, EosComparisonOp.AnyOf, EosComparisonOp.NotAnyOf,
        ).forEachIndexed { i, op ->
            assertEquals(i.toDouble(), op.value.toDouble(), "comparison $op should map to $i")
        }
    }

    @Test
    fun `external account and credential types are distinct`() {
        val accounts = EosExternalAccountType.entries.toSet()
        val credentials = EosExternalCredentialType.entries.toSet()
        assertEquals(15, accounts.size)
        assertEquals(20, credentials.size)
    }

    @Test
    fun `online platform mapping is correct`() {
        assertEquals(EosOnlinePlatform.Unknown, EosOnlinePlatform.fromValue(0))
        assertEquals(EosOnlinePlatform.Epic, EosOnlinePlatform.fromValue(100))
        assertEquals(EosOnlinePlatform.Steam, EosOnlinePlatform.fromValue(4000))
    }

    // endregion

    // region P2P

    @Test
    fun `P2P enums have stable integer values`() {
        assertEquals(0, EosPacketReliability.UnreliableUnordered.value)
        assertEquals(1, EosPacketReliability.ReliableUnordered.value)
        assertEquals(2, EosPacketReliability.ReliableOrdered.value)

        assertEquals(0, EosNatType.Unknown.value)
        assertEquals(1, EosNatType.Open.value)
        assertEquals(2, EosNatType.Moderate.value)
        assertEquals(3, EosNatType.Strict.value)

        assertEquals(0, EosConnectionEstablishedType.NewConnection.value)
        assertEquals(1, EosConnectionEstablishedType.Reconnection.value)

        assertEquals(0, EosNetworkConnectionType.NoConnection.value)
        assertEquals(1, EosNetworkConnectionType.DirectConnection.value)
        assertEquals(2, EosNetworkConnectionType.RelayedConnection.value)

        assertEquals(0, EosRelayControl.NoRelays.value)
        assertEquals(1, EosRelayControl.AllowRelays.value)
        assertEquals(2, EosRelayControl.ForceRelays.value)
    }

    @Test
    fun `P2P constants match the C SDK`() {
        assertEquals(1170, gg.sona.eos.p2p.EosP2P.MAX_PACKET_SIZE)
        assertEquals(32, gg.sona.eos.p2p.EosP2P.MAX_CONNECTIONS)
        assertEquals(33, gg.sona.eos.p2p.EosP2P.SOCKET_NAME_SIZE)
        assertEquals(0L, gg.sona.eos.p2p.EosP2P.MAX_QUEUE_SIZE_UNLIMITED)
    }

    @Test
    fun `connection closed reason mapping is well-defined`() {
        for (i in 0..11) {
            val reason = EosConnectionClosedReason.fromValue(i)
            assertEquals(i, reason.value, "reason $i should map to itself")
        }
    }

    // endregion

    // region RTC

    @Test
    fun `RTC enums have stable values`() {
        assertEquals(0, EosRtcParticipantStatus.Joined.value)
        assertEquals(1, EosRtcParticipantStatus.Left.value)

        assertEquals(0, EosRtcAudioStatus.Unsupported.value)
        assertEquals(1, EosRtcAudioStatus.Enabled.value)
        assertEquals(2, EosRtcAudioStatus.Disabled.value)
        assertEquals(3, EosRtcAudioStatus.AdminDisabled.value)
        assertEquals(4, EosRtcAudioStatus.NotListeningDisabled.value)

        assertEquals(0, EosRtcAudioInputStatus.Idle.value)
        assertEquals(1, EosRtcAudioInputStatus.Recording.value)
        assertEquals(4, EosRtcAudioInputStatus.Failed.value)

        assertEquals(0, EosRtcAudioOutputStatus.Idle.value)
        assertEquals(1, EosRtcAudioOutputStatus.Playing.value)
        assertEquals(2, EosRtcAudioOutputStatus.Failed.value)

        assertEquals(0, EosRtcDataStatus.Unsupported.value)
        assertEquals(1, EosRtcDataStatus.Enabled.value)
        assertEquals(2, EosRtcDataStatus.Disabled.value)
    }

    // endregion

    // region Anti-Cheat

    @Test
    fun `anti-cheat client types are exposed`() {
        assertEquals(0, ClientType.ProtectedClient.value)
        assertEquals(1, ClientType.UnprotectedClient.value)
        assertEquals(2, ClientType.AiBot.value)
    }

    @Test
    fun `anti-cheat client platforms are exposed`() {
        assertEquals(0, ClientPlatform.Unknown.value)
        assertEquals(1, ClientPlatform.Windows.value)
        assertEquals(2, ClientPlatform.Mac.value)
        assertEquals(3, ClientPlatform.Linux.value)
        assertEquals(8, ClientPlatform.Android.value)
    }

    @Test
    fun `anti-cheat self handle is recognized`() {
        assertEquals(-1L, EosAntiCheatCommon.ClientHandle.SELF.raw)
    }

    @Test
    fun `anti-cheat client mode is mapped correctly`() {
        assertEquals(0, gg.sona.eos.anticheat.client.EosAntiCheatClientMode.Invalid.value)
        assertEquals(1, gg.sona.eos.anticheat.client.EosAntiCheatClientMode.ClientServer.value)
        assertEquals(2, gg.sona.eos.anticheat.client.EosAntiCheatClientMode.PeerToPeer.value)
    }

    @Test
    fun `anti-cheat violation types are exposed`() {
        assertEquals(0, gg.sona.eos.anticheat.client.EosAntiCheatClientViolationType.Invalid.value)
        assertEquals(1, gg.sona.eos.anticheat.client.EosAntiCheatClientViolationType.IntegrityCatalogNotFound.value)
        assertEquals(15, gg.sona.eos.anticheat.client.EosAntiCheatClientViolationType.ForbiddenSystemConfiguration.value)
    }

    // endregion

    // region Lobby

    @Test
    fun `lobby member status mapping is consistent`() {
        assertEquals(0, EosLobbyMemberStatus.Joined.value)
        assertEquals(1, EosLobbyMemberStatus.Left.value)
        assertEquals(2, EosLobbyMemberStatus.Disconnected.value)
        assertEquals(3, EosLobbyMemberStatus.Kicked.value)
        assertEquals(4, EosLobbyMemberStatus.Promoted.value)
    }

    // endregion

    // region Ecom

    @Test
    fun `ownership status is consistent`() {
        assertEquals(0, EosOwnershipStatus.NotOwned.value)
        assertEquals(1, EosOwnershipStatus.Owned.value)
    }

    // endregion

    // region Logging

    @Test
    fun `log level values match the SDK`() {
        assertEquals(0, EosLogLevel.Off.value)
        assertEquals(100, EosLogLevel.Fatal.value)
        assertEquals(200, EosLogLevel.Error.value)
        assertEquals(300, EosLogLevel.Warning.value)
        assertEquals(400, EosLogLevel.Info.value)
        assertEquals(500, EosLogLevel.Verbose.value)
        assertEquals(600, EosLogLevel.VeryVerbose.value)
    }

    @Test
    fun `log category values are stable`() {
        assertEquals(0, EosLogCategory.Core.value)
        assertEquals(7, EosLogCategory.P2P.value)
        assertEquals(18, EosLogCategory.Lobby.value)
        assertEquals(24, EosLogCategory.AntiCheat.value)
        assertEquals(29, EosLogCategory.RTC.value)
        assertEquals(Int.MAX_VALUE, EosLogCategory.AllCategories.value)
    }

    // endregion
}
