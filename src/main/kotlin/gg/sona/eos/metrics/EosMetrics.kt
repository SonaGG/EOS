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
package gg.sona.eos.metrics

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.withCallArena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.ValueLayout

/**
 * Metrics interface. Used to inform the backend about the start and end of
 * a player's session for analytics.
 */
class EosMetrics internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetMetricsInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    fun beginPlayerSession(
        localUserId: ProductUserId? = null,
        displayName: String? = null,
        controllerType: EosMetricsControllerType = EosMetricsControllerType.Unknown,
        serverIp: String? = null,
    ): EosResult = withCallArena { arena ->
        val options = MetricsBeginPlayerSessionOptions(
            localUserId, displayName, controllerType, serverIp
        )
        EosResult.fromValue(
            Native.invoke(
                "EOS_Metrics_BeginPlayerSession",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    fun endPlayerSession(
        localUserId: ProductUserId? = null,
    ): EosResult = withCallArena { arena ->
        val options = MetricsEndPlayerSessionOptions(localUserId)
        EosResult.fromValue(
            Native.invoke(
                "EOS_Metrics_EndPlayerSession",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }
}