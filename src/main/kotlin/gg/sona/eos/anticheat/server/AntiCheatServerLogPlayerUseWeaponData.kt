package gg.sona.eos.anticheat.server

import gg.sona.eos.anticheat.common.EosAntiCheatCommon
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class AntiCheatServerLogPlayerUseWeaponData(
    var playerHandle: EosAntiCheatCommon.ClientHandle,
    var playerPosition: EosAntiCheatCommon.Vec3f,
    var playerViewRotation: EosAntiCheatCommon.Quat,
    var isPlayerViewZoomed: Boolean,
    var isMeleeAttack: Boolean,
    var weaponName: String,
) {
    fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt64(0, playerHandle.raw)
        val positionSeg = arena.allocate(
            MemoryLayout.structLayout(
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
        ))
        positionSeg.set(ValueLayout.JAVA_FLOAT, 0, playerPosition.x)
        positionSeg.set(ValueLayout.JAVA_FLOAT, 4, playerPosition.y)
        positionSeg.set(ValueLayout.JAVA_FLOAT, 8, playerPosition.z)
        seg.setInt64(8, positionSeg.address())
        val viewRotSeg = arena.allocate(MemoryLayout.structLayout(
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
        ))
        viewRotSeg.set(ValueLayout.JAVA_FLOAT, 0, playerViewRotation.w)
        viewRotSeg.set(ValueLayout.JAVA_FLOAT, 4, playerViewRotation.x)
        viewRotSeg.set(ValueLayout.JAVA_FLOAT, 8, playerViewRotation.y)
        viewRotSeg.set(ValueLayout.JAVA_FLOAT, 12, playerViewRotation.z)
        seg.setInt64(16, viewRotSeg.address())
        seg.setInt32(24, if (isPlayerViewZoomed) 1 else 0)
        seg.setInt32(28, if (isMeleeAttack) 1 else 0)
        seg.setInt64(32, arena.allocCString(weaponName).address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
        )
    }
}