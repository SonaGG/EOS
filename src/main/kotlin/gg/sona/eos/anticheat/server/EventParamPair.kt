package gg.sona.eos.anticheat.server

import gg.sona.eos.anticheat.common.EosAntiCheatCommon
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** A custom event parameter value. */
public sealed class EventParamPair(public val type: EosAntiCheatCommon.EventParamType) {
    public class ClientHandleValue(public val value: gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientHandle) :
        EventParamPair(EosAntiCheatCommon.EventParamType.ClientHandle)
    public class StringValue(public val value: String) : EventParamPair(EosAntiCheatCommon.EventParamType.String)
    public class UInt32Value(public val value: UInt) : EventParamPair(EosAntiCheatCommon.EventParamType.UInt32)
    public class Int32Value(public val value: Int) : EventParamPair(EosAntiCheatCommon.EventParamType.Int32)
    public class UInt64Value(public val value: ULong) : EventParamPair(EosAntiCheatCommon.EventParamType.UInt64)
    public class Int64Value(public val value: Long) : EventParamPair(EosAntiCheatCommon.EventParamType.Int64)
    public class Vector3fValue(public val value: EosAntiCheatCommon.Vec3f) : EventParamPair(EosAntiCheatCommon.EventParamType.Vector3f)
    public class QuatValue(public val value: EosAntiCheatCommon.Quat) : EventParamPair(EosAntiCheatCommon.EventParamType.Quat)
    public class FloatValue(public val value: Float) : EventParamPair(EosAntiCheatCommon.EventParamType.Float)

    internal fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(
            MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG,
        ))
        seg.setInt32(0, type.value)
        when (this) {
            is ClientHandleValue -> seg.setInt64(8, value.raw)
            is StringValue -> seg.setInt64(8, arena.allocCString(value).address())
            is UInt32Value -> seg.setInt32(8, value.toInt())
            is Int32Value -> seg.setInt32(8, value)
            is UInt64Value -> seg.setInt64(8, value.toLong())
            is Int64Value -> seg.setInt64(8, value)
            is Vector3fValue -> {
                val v = arena.allocate(MemoryLayout.structLayout(
                    ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
                ))
                v.set(ValueLayout.JAVA_FLOAT, 0, value.x)
                v.set(ValueLayout.JAVA_FLOAT, 4, value.y)
                v.set(ValueLayout.JAVA_FLOAT, 8, value.z)
                seg.setInt64(8, v.address())
            }
            is QuatValue -> {
                val q = arena.allocate(MemoryLayout.structLayout(
                    ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
                ))
                q.set(ValueLayout.JAVA_FLOAT, 0, value.w)
                q.set(ValueLayout.JAVA_FLOAT, 4, value.x)
                q.set(ValueLayout.JAVA_FLOAT, 8, value.y)
                q.set(ValueLayout.JAVA_FLOAT, 12, value.z)
                seg.setInt64(8, q.address())
            }
            is FloatValue -> seg.set(ValueLayout.JAVA_FLOAT, 8, value)
        }
        return seg
    }
}
