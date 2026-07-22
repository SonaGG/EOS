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

internal class AntiCheatServerLogPlayerTakeDamageOptions(
    var victimPlayerHandle: EosAntiCheatCommon.ClientHandle,
    var victimPlayerPosition: EosAntiCheatCommon.Vec3f,
    var victimPlayerViewRotation: EosAntiCheatCommon.Quat,
    var attackerPlayerHandle: EosAntiCheatCommon.ClientHandle,
    var attackerPlayerPosition: EosAntiCheatCommon.Vec3f?,
    var attackerPlayerViewRotation: EosAntiCheatCommon.Quat?,
    var isHitscanAttack: Boolean,
    var hasLineOfSight: Boolean,
    var isCriticalHit: Boolean,
    var damageTaken: Float,
    var healthRemaining: Float,
    var damageSource: EosAntiCheatCommon.PlayerTakeDamageSource,
    var damageType: EosAntiCheatCommon.PlayerTakeDamageType,
    var damageResult: EosAntiCheatCommon.PlayerTakeDamageResult,
    var damagePosition: EosAntiCheatCommon.Vec3f?,
    var attackerPlayerViewPosition: EosAntiCheatCommon.Vec3f?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 4)
        seg.setInt64(8, victimPlayerHandle.raw)
        seg.setInt64(16, writeVec3(arena, victimPlayerPosition).address())
        seg.setInt64(24, writeQuat(arena, victimPlayerViewRotation).address())
        seg.setInt64(32, attackerPlayerHandle.raw)
        seg.setInt64(40, attackerPlayerPosition?.let { writeVec3(arena, it).address() } ?: 0L)
        seg.setInt64(48, attackerPlayerViewRotation?.let { writeQuat(arena, it).address() } ?: 0L)
        seg.setInt32(56, if (isHitscanAttack) 1 else 0)
        seg.setInt32(60, if (hasLineOfSight) 1 else 0)
        seg.setInt32(64, if (isCriticalHit) 1 else 0)
        seg.setInt32(68, 0) // deprecated
        seg.setFloat(72, damageTaken)
        seg.setFloat(76, healthRemaining)
        seg.setInt32(80, damageSource.value)
        seg.setInt32(84, damageType.value)
        seg.setInt32(88, damageResult.value)
        seg.setInt64(96, 0L) // use weapon data pointer (omitted)
        seg.setInt32(104, 0)
        seg.setInt64(112, damagePosition?.let { writeVec3(arena, it).address() } ?: 0L)
        seg.setInt64(120, attackerPlayerViewPosition?.let { writeVec3(arena, it).address() } ?: 0L)
        return seg
    }

    private fun writeVec3(arena: Arena, v: EosAntiCheatCommon.Vec3f): MemorySegment {
        val seg = arena.allocate(
            MemoryLayout.structLayout(
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
        ))
        seg.set(ValueLayout.JAVA_FLOAT, 0, v.x)
        seg.set(ValueLayout.JAVA_FLOAT, 4, v.y)
        seg.set(ValueLayout.JAVA_FLOAT, 8, v.z)
        return seg
    }

    private fun writeQuat(arena: Arena, q: EosAntiCheatCommon.Quat): MemorySegment {
        val seg = arena.allocate(MemoryLayout.structLayout(
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
        ))
        seg.set(ValueLayout.JAVA_FLOAT, 0, q.w)
        seg.set(ValueLayout.JAVA_FLOAT, 4, q.x)
        seg.set(ValueLayout.JAVA_FLOAT, 8, q.y)
        seg.set(ValueLayout.JAVA_FLOAT, 12, q.z)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
            MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
            MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS, ValueLayout.ADDRESS,
        )
    }
}
