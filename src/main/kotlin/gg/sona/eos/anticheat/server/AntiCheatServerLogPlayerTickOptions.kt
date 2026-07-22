package gg.sona.eos.anticheat.server

import gg.sona.eos.anticheat.common.EosAntiCheatCommon
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.setFloat
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class AntiCheatServerLogPlayerTickOptions(
    var playerHandle: EosAntiCheatCommon.ClientHandle,
    var playerPosition: EosAntiCheatCommon.Vec3f,
    var playerViewRotation: EosAntiCheatCommon.Quat,
    var isPlayerViewZoomed: Boolean,
    var playerHealth: Float,
    var playerMovementState: EosAntiCheatCommon.PlayerMovementState,
    var playerViewPosition: EosAntiCheatCommon.Vec3f?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 3)
        seg.setInt64(8, playerHandle.raw)
        val positionSeg = arena.allocate(
            MemoryLayout.structLayout(
                ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
            )
        )
        positionSeg.set(ValueLayout.JAVA_FLOAT, 0, playerPosition.x)
        positionSeg.set(ValueLayout.JAVA_FLOAT, 4, playerPosition.y)
        positionSeg.set(ValueLayout.JAVA_FLOAT, 8, playerPosition.z)
        seg.setInt64(16, positionSeg.address())
        val viewRotSeg = arena.allocate(
            MemoryLayout.structLayout(
                ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
            )
        )
        viewRotSeg.set(ValueLayout.JAVA_FLOAT, 0, playerViewRotation.w)
        viewRotSeg.set(ValueLayout.JAVA_FLOAT, 4, playerViewRotation.x)
        viewRotSeg.set(ValueLayout.JAVA_FLOAT, 8, playerViewRotation.y)
        viewRotSeg.set(ValueLayout.JAVA_FLOAT, 12, playerViewRotation.z)
        seg.setInt64(24, viewRotSeg.address())
        seg.setInt32(32, if (isPlayerViewZoomed) 1 else 0)
        seg.setFloat(36, playerHealth)
        seg.setInt32(40, playerMovementState.value)
        if (playerViewPosition != null) {
            val pos = playerViewPosition!!
            val vpSeg = arena.allocate(
                MemoryLayout.structLayout(
                    ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
                )
            )
            vpSeg.set(ValueLayout.JAVA_FLOAT, 0, pos.x)
            vpSeg.set(ValueLayout.JAVA_FLOAT, 4, pos.y)
            vpSeg.set(ValueLayout.JAVA_FLOAT, 8, pos.z)
            seg.setInt64(48, vpSeg.address())
        } else {
            seg.setInt64(48, 0L)
        }
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_INT,
            MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS,
        )
    }
}