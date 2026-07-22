package gg.sona.eos.auth

import gg.sona.eos.common.EosExternalCredentialType
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class AuthCredentials(
    var id: String?,
    var token: String?,
    var type: EosAuthCredentialType,
    var externalType: EosExternalCredentialType?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(id).address())
        seg.setInt64(16, arena.allocCString(token).address())
        seg.setInt32(24, type.value)
        seg.setInt64(32, 0L) // SystemAuthCredentialsOptions - unsupported
        seg.setInt32(40, externalType?.value ?: 0)
        return seg
    }

    companion object {
        // EOS_AUTH_CREDENTIALS_API_LATEST
        const val API_LATEST = 4
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}

