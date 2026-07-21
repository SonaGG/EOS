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

import gg.sona.eos.common.EosAttributeType
import gg.sona.eos.common.EosComparisonOp
import gg.sona.eos.common.EosLoginStatus
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientPlatform
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientType
import gg.sona.eos.logging.EosLogCategory
import gg.sona.eos.logging.EosLogLevel
import gg.sona.eos.p2p.EosPacketReliability
import gg.sona.eos.rtc.EosRtcParticipantStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Enum and small-API tests that do not require the native library to be
 * loaded. They are used as a smoke test of the binding's public surface.
 */
class EnumTest {

    @Test
    fun `EosResult fromValue handles unknown`() {
        val unknown = EosResult.fromValue(99_999_999)
        assertEquals(EosResult.UnexpectedError, unknown)
    }

    @Test
    fun `enum mapping is consistent`() {
        assertEquals(EosLoginStatus.NotLoggedIn, EosLoginStatus.fromValue(0))
        assertEquals(EosLoginStatus.LoggedIn, EosLoginStatus.fromValue(2))
        assertEquals(EosAttributeType.String, EosAttributeType.fromValue(3))
        assertEquals(EosComparisonOp.Equal, EosComparisonOp.fromValue(0))
    }

    @Test
    fun `P2P reliability enum maps to expected ints`() {
        assertEquals(0, EosPacketReliability.UnreliableUnordered.value)
        assertEquals(2, EosPacketReliability.ReliableOrdered.value)
    }

    @Test
    fun `RTC participant status mapping is consistent`() {
        assertEquals(EosRtcParticipantStatus.Joined, EosRtcParticipantStatus.fromValue(0))
        assertEquals(EosRtcParticipantStatus.Left, EosRtcParticipantStatus.fromValue(1))
    }

    @Test
    fun `anti-cheat client types are exposed`() {
        assertEquals(0, ClientType.ProtectedClient.value)
        assertEquals(1, ClientType.UnprotectedClient.value)
        assertEquals(8, ClientPlatform.Android.value)
    }

    @Test
    fun `anti-cheat self handle is recognized`() {
        assertEquals(-1L, gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientHandle.SELF.raw)
    }

    @Test
    fun `logging levels exist`() {
        assertEquals(0, EosLogLevel.Off.value)
        assertEquals(400, EosLogLevel.Info.value)
    }

    @Test
    fun `logging categories map to expected ints`() {
        assertEquals(0, EosLogCategory.Core.value)
        assertEquals(7, EosLogCategory.P2P.value)
        assertEquals(24, EosLogCategory.AntiCheat.value)
    }
}
