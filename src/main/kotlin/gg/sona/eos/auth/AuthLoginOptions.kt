package gg.sona.eos.auth

import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class AuthLoginOptions(
    var credentials: AuthCredentials,
    var scopeFlags: Int,
    var loginFlags: Long,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        val credentialsSeg = credentials.writeTo(arena)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, credentialsSeg.address())
        seg.setInt32(16, scopeFlags)
        seg.setInt64(24, loginFlags)
        return seg
    }

    companion object {
        // EOS_AUTH_LOGIN_API_LATEST
        const val API_LATEST = 3
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
        )
    }
}
