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
package gg.sona.eos.metrics

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId
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
 * Metrics interface. Used to inform the backend about the start and end of
 * a player's session for analytics.
 */
public class EosMetrics internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetMetricsInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    public fun beginPlayerSession(
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

    public fun endPlayerSession(
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

public enum class EosMetricsControllerType(val value: Int) {
    Unknown(0),
    MouseKeyboard(1),
    Gamepad(2),
    TouchInput(3);

    public companion object {
        internal fun fromValue(v: Int): EosMetricsControllerType =
            entries.firstOrNull { it.value == v } ?: Unknown
    }
}

internal class MetricsBeginPlayerSessionOptions(
    var localUserId: ProductUserId?,
    var displayName: String?,
    var controllerType: EosMetricsControllerType,
    var serverIp: String?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId?.raw ?: 0L)
        seg.setInt64(16, arena.allocCString(displayName).address())
        seg.setInt64(24, arena.allocCString(serverIp).address())
        seg.setInt32(32, controllerType.value)
        return seg
    }

    companion object {
        const val API_LATEST = 3
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
        )
    }
}

internal class MetricsEndPlayerSessionOptions(var localUserId: ProductUserId?) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId?.raw ?: 0L)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG
        )
    }
}
