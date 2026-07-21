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

import gg.sona.eos.EosClientCredentials
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the options builders. They exercise only the Kotlin side of
 * the API, so they do not require the native library.
 */
class OptionsBuilderTest {

    @Test
    fun `EosInitializeOptions create sets product fields`() {
        val options = EosInitializeOptions.create("MyProduct", "1.0.0")
        assertEquals("MyProduct", options.productName)
        assertEquals("1.0.0", options.productVersion)
        assertEquals(EosInitializeOptions.API_LATEST, options.apiVersion)
    }

    @Test
    fun `EosInitializeOptions build returns the same instance`() {
        val options = EosInitializeOptions.create("a", "b")
        assertTrue(options === options.build())
    }

    @Test
    fun `EosClientCredentials of creates with both fields`() {
        val creds = EosClientCredentials.of("client-id", "client-secret")
        assertEquals("client-id", creds.clientId)
        assertEquals("client-secret", creds.clientSecret)
    }

    @Test
    fun `EosPlatformOptions create populates required fields`() {
        val creds = EosClientCredentials.of("id", "secret")
        val options = EosPlatformOptions.create(
            productId = "prod",
            sandboxId = "sand",
            deploymentId = "dep",
            clientCredentials = creds,
        )
        assertEquals("prod", options.productId)
        assertEquals("sand", options.sandboxId)
        assertEquals("dep", options.deploymentId)
        assertEquals(creds, options.clientCredentials)
        assertEquals(false, options.isServer)
    }

    @Test
    fun `EosPlatformOptions builder allows mutation`() {
        val creds = EosClientCredentials.of("id", "secret")
        val options = EosPlatformOptions.create("a", "b", "c", creds)
        options.isServer = true
        options.flags = EosPlatformFlags.DisableOverlay
        assertEquals(true, options.isServer)
        assertEquals(EosPlatformFlags.DisableOverlay, options.flags)
    }

    @Test
    fun `EosRtcOptions has stable defaults`() {
        val rtc = EosRtcOptions()
        assertEquals(EosRtcBackgroundMode.LeaveRooms, rtc.backgroundMode)
    }

    @Test
    fun `EosPlatformFlags constants are distinct powers of two`() {
        val expected = listOf(
            EosPlatformFlags.LoadingInEditor,
            EosPlatformFlags.DisableOverlay,
            EosPlatformFlags.DisableSocialOverlay,
            EosPlatformFlags.Reserved1,
            EosPlatformFlags.WindowsEnableOverlayD3D9,
            EosPlatformFlags.WindowsEnableOverlayD3D10,
            EosPlatformFlags.WindowsEnableOverlayOpenGL,
            EosPlatformFlags.ConsoleEnableOverlayAutomaticUnloading,
            EosPlatformFlags.EnableOverlayDebugLogging,
        )
        // Each constant is a single bit.
        for (flag in expected) {
            assertTrue(flag != 0, "flag must be non-zero")
            assertEquals(flag, flag and flag, "flag must be a power of two (or zero)")
        }
        // No two flags share a bit.
        var combined = 0
        for (flag in expected) {
            assertEquals(0, combined and flag, "duplicate bit in flags")
            combined = combined or flag
        }
        assertNotNull(combined)
    }
}
