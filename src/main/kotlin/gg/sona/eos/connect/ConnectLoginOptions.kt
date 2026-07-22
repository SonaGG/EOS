package gg.sona.eos.connect

import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class ConnectLoginOptions(
    var credentials: ConnectCredentials,
    var userLoginInfo: ConnectUserLoginInfo?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        val credentialsSeg = credentials.writeTo(arena)
        val userLoginInfoAddress = userLoginInfo?.writeTo(arena)?.address() ?: 0L
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, credentialsSeg.address())
        seg.setInt64(16, userLoginInfoAddress)
        return seg
    }

    companion object {
        // EOS_CONNECT_LOGIN_API_LATEST
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
        )
    }
}