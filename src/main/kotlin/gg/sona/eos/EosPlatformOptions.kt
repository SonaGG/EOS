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

import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * Options used to construct an [EosPlatform].
 *
 * At minimum [productId], [sandboxId], [deploymentId], and [clientCredentials]
 * are required. Set [isServer] to true for dedicated server builds.
 */
class EosPlatformOptions internal constructor() : StructWriter {

    var apiVersion: Int = API_LATEST

    /** Required. The product id from the dev portal. Max 64 bytes. */
    var productId: String = ""

    /** Required. The sandbox id from the dev portal. Max 64 bytes. */
    var sandboxId: String = ""

    /** Optional. The deployment id from the dev portal. Max 64 bytes. */
    var deploymentId: String = ""

    /** Required. The client credentials issued by the dev portal. */
    var clientCredentials: EosClientCredentials = EosClientCredentials()

    /** Set to true for dedicated server builds. Default false. */
    var isServer: Boolean = false

    /**
     * The 256-bit encryption key used by PlayerDataStorage and TitleStorage,
     * formatted as 64 hex characters. Leave null to disable storage encryption.
     */
    var encryptionKey: String? = null

    /** Override the country code used for ecom. Leave null for the default. */
    var overrideCountryCode: String? = null

    /** Override the locale code used for ecom. Leave null for the default. */
    var overrideLocaleCode: String? = null

    /** Bitwise-or of [EosPlatformFlags]. */
    var flags: Int = 0

    /**
     * Absolute path to a directory used for caching. Required if
     * [EosPlatformFlags.DisableOverlay] is not set and you want to enable
     * PlayerDataStorage or TitleStorage.
     */
    var cacheDirectory: String? = null

    /**
     * Maximum milliseconds per tick. Zero means "perform all available work".
     */
    var tickBudgetInMilliseconds: Int = 0

    /** Optional RTC options. Leave null to disable RTC. */
    var rtcOptions: EosRtcOptions? = null

    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(OFFSET_API_VERSION, apiVersion)
        seg.set(ValueLayout.ADDRESS, OFFSET_RESERVED, MemorySegment.NULL)
        seg.set(ValueLayout.ADDRESS, OFFSET_PRODUCT_ID, arena.allocCString(productId))
        seg.set(ValueLayout.ADDRESS, OFFSET_SANDBOX_ID, arena.allocCString(sandboxId))
        seg.setInt64(OFFSET_CLIENT_ID, clientCredentials.clientId?.let { arena.allocCString(it).address() } ?: 0L)
        seg.setInt64(
            OFFSET_CLIENT_SECRET,
            clientCredentials.clientSecret?.let { arena.allocCString(it).address() } ?: 0L)
        seg.setInt32(OFFSET_IS_SERVER, if (isServer) 1 else 0)
        seg.setInt64(OFFSET_ENCRYPTION_KEY, encryptionKey?.let { arena.allocCString(it).address() } ?: 0L)
        seg.setInt64(OFFSET_COUNTRY, overrideCountryCode?.let { arena.allocCString(it).address() } ?: 0L)
        seg.setInt64(OFFSET_LOCALE, overrideLocaleCode?.let { arena.allocCString(it).address() } ?: 0L)
        seg.setInt64(OFFSET_DEPLOYMENT_ID, deploymentId.let { arena.allocCString(it).address() })
        seg.setInt64(OFFSET_FLAGS, flags.toLong())
        seg.setInt64(OFFSET_CACHE_DIR, cacheDirectory?.let { arena.allocCString(it).address() } ?: 0L)
        seg.setInt32(OFFSET_TICK_BUDGET, tickBudgetInMilliseconds)
        seg.setInt64(OFFSET_RTC_OPTIONS, rtcOptions?.writeTo(arena)?.address() ?: 0L)
        seg.setInt64(OFFSET_IP_OPTIONS, 0L)
        seg.setInt64(OFFSET_SYSTEM_OPTIONS, 0L)
        seg.setInt64(OFFSET_TASK_TIMEOUT, 0L)
        return seg
    }

    companion object {
        const val API_LATEST: Int = 15

        internal val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("ApiVersion"),
            MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS.withName("Reserved"),
            ValueLayout.ADDRESS.withName("ProductId"),
            ValueLayout.ADDRESS.withName("SandboxId"),
            ValueLayout.ADDRESS.withName("ClientId"),
            ValueLayout.ADDRESS.withName("ClientSecret"),
            ValueLayout.JAVA_INT.withName("bIsServer"),
            MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS.withName("EncryptionKey"),
            ValueLayout.ADDRESS.withName("OverrideCountryCode"),
            ValueLayout.ADDRESS.withName("OverrideLocaleCode"),
            ValueLayout.ADDRESS.withName("DeploymentId"),
            ValueLayout.JAVA_LONG.withName("Flags"),
            ValueLayout.ADDRESS.withName("CacheDirectory"),
            ValueLayout.JAVA_INT.withName("TickBudgetInMilliseconds"),
            MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS.withName("RTCOptions"),
            ValueLayout.ADDRESS.withName("IntegratedPlatformOptionsContainerHandle"),
            ValueLayout.ADDRESS.withName("SystemSpecificOptions"),
            ValueLayout.ADDRESS.withName("TaskNetworkTimeoutSeconds"),
        )

        internal const val OFFSET_API_VERSION: Long = 0
        internal const val OFFSET_RESERVED: Long = 8
        internal const val OFFSET_PRODUCT_ID: Long = 16
        internal const val OFFSET_SANDBOX_ID: Long = 24
        internal const val OFFSET_CLIENT_ID: Long = 32
        internal const val OFFSET_CLIENT_SECRET: Long = 40
        internal const val OFFSET_IS_SERVER: Long = 48
        internal const val OFFSET_ENCRYPTION_KEY: Long = 56
        internal const val OFFSET_COUNTRY: Long = 64
        internal const val OFFSET_LOCALE: Long = 72
        internal const val OFFSET_DEPLOYMENT_ID: Long = 80
        internal const val OFFSET_FLAGS: Long = 88
        internal const val OFFSET_CACHE_DIR: Long = 96
        internal const val OFFSET_TICK_BUDGET: Long = 104
        internal const val OFFSET_RTC_OPTIONS: Long = 112
        internal const val OFFSET_IP_OPTIONS: Long = 120
        internal const val OFFSET_SYSTEM_OPTIONS: Long = 128
        internal const val OFFSET_TASK_TIMEOUT: Long = 136

        fun create(
            productId: String,
            sandboxId: String,
            deploymentId: String,
            clientCredentials: EosClientCredentials,
        ): EosPlatformOptions = EosPlatformOptions().apply {
            this.productId = productId
            this.sandboxId = sandboxId
            this.deploymentId = deploymentId
            this.clientCredentials = clientCredentials
        }
    }
}