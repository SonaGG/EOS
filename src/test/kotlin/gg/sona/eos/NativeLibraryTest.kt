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

import gg.sona.eos.anticheat.client.EosAntiCheatClientMode
import gg.sona.eos.EosClientCredentials
import gg.sona.eos.EosClientCredentials as _ignore
import gg.sona.eos.common.EosExternalCredentialType
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.p2p.EosP2PSocketId
import gg.sona.eos.p2p.EosPacketReliability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end smoke tests that actually load the EOS native library and
 * exercise the bindings. These tests are skipped automatically when the
 * library cannot be found (e.g. running unit tests in a CI environment
 * without the native binary).
 */
class NativeLibraryTest {

    private val libraryAvailable: Boolean = runCatching {
        // Eos.version is a lazy property; this triggers the C library
        // load. If the library is missing, runCatching returns false.
        Eos.version
        true
    }.getOrDefault(false)

    private fun runIfAvailable(block: () -> Unit) {
        if (!libraryAvailable) {
            println("Skipping: EOS native library not available")
            return
        }
        block()
    }

    @Test
    fun `version is a non-empty string`() = runIfAvailable {
        assertTrue(Eos.version.isNotEmpty(), "version should be non-empty")
        println("EOS version: ${Eos.version}")
    }

    @Test
    fun `isInitialized is false before init`() = runIfAvailable {
        assertEquals(false, Eos.isInitialized)
    }

    @Test
    fun `cannot double-initialize`() = runIfAvailable {
        val opts = EosInitializeOptions.create("SmokeTest", "0.0.1")
        Eos.initialize(opts)
        try {
            val second = Eos.initialize(opts)
            assertEquals(EosResult.AlreadyConfigured, second)
        } finally {
            Eos.shutdown()
        }
        assertEquals(false, Eos.isInitialized)
    }

    @Test
    fun `init then shutdown then init works`() = runIfAvailable {
        val opts = EosInitializeOptions.create("SmokeTest", "0.0.1")
        Eos.initialize(opts)
        Eos.shutdown()
        Eos.initialize(opts)
        Eos.shutdown()
    }

    @Test
    fun `cannot create platform before init`() = runIfAvailable {
        val ex = kotlin.runCatching {
            Eos.createPlatform(
                EosPlatformOptions.create(
                    productId = "p", sandboxId = "s", deploymentId = "d",
                    clientCredentials = EosClientCredentials.of("c", "s"),
                )
            )
        }.exceptionOrNull()
        assertTrue(ex is IllegalStateException, "expected IllegalStateException, got $ex")
    }

    @Test
    fun `full bootstrap and platform lifecycle`() = runIfAvailable {
        Eos.initialize(EosInitializeOptions.create("SmokeTest", "0.0.1"))
        try {
            val platform = Eos.createPlatform(
                EosPlatformOptions.create(
                    productId = "00000000000000000000000000000000",
                    sandboxId = "00000000000000000000000000000000",
                    deploymentId = "00000000000000000000000000000000",
                    clientCredentials = EosClientCredentials.of("client-id", "client-secret"),
                )
            )
            try {
                // tick a few times to exercise the platform loop
                repeat(5) { platform.tick() }
                // subsystem handle accessors must all be reachable
                @Suppress("UNUSED_VARIABLE")
                val authHandle = platform.auth
                @Suppress("UNUSED_VARIABLE")
                val rtcHandle = platform.rtc
                @Suppress("UNUSED_VARIABLE")
                val p2pHandle = platform.p2p
            } finally {
                platform.close()
            }
        } finally {
            Eos.shutdown()
        }
    }

    @Test
    fun `anti-cheat client BeginSession requires a valid product user`() = runIfAvailable {
        Eos.initialize(EosInitializeOptions.create("SmokeTest", "0.0.1"))
        try {
            val platform = Eos.createPlatform(
                EosPlatformOptions.create(
                    productId = "00000000000000000000000000000000",
                    sandboxId = "00000000000000000000000000000000",
                    deploymentId = "00000000000000000000000000000000",
                    clientCredentials = EosClientCredentials.of("c", "s"),
                )
            )
            try {
                val result = platform.antiCheatClient.beginSession(
                    ProductUserId.Invalid,
                    EosAntiCheatClientMode.ClientServer,
                )
                // Without a valid login, this should fail with InvalidAuth or similar
                assertTrue(result != EosResult.Success, "BeginSession with invalid user should not succeed")
            } finally {
                platform.close()
            }
        } finally {
            Eos.shutdown()
        }
    }

    @Test
    fun `p2p socket id name is preserved`() = runIfAvailable {
        val socket = EosP2PSocketId("smoke-test-channel")
        assertEquals("smoke-test-channel", socket.name)
    }

    @Test
    fun `enum value mapping is consistent across the binding`() = runIfAvailable {
        // EosExternalCredentialType is used directly in the API.
        assertEquals(0, EosExternalCredentialType.Epic.value)
        // EosPacketReliability is the P2P packet reliability enum.
        assertEquals(2, EosPacketReliability.ReliableOrdered.value)
    }
}
