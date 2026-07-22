package gg.sona.eos

import gg.sona.eos.internal.*
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** RTC options for the platform. */
public class EosRtcOptions : StructWriter {
    public var backgroundMode: EosRtcBackgroundMode = EosRtcBackgroundMode.LeaveRooms

    /**
     * Absolute path to `xaudio2_9redist.dll`.
     *
     * Windows refuses to create a platform with RTC enabled unless this is supplied, and the
     * failure surfaces as a null handle out of `EOS_Platform_Create` rather than an error code.
     * Defaults to the copy bundled in this library, so it only needs setting to override that.
     */
    public var xAudio29DllPath: String? = null

    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, windowsOptions(arena)?.address() ?: 0L)
        seg.setInt32(16, backgroundMode.value)
        seg.setInt64(24, 0L) // Reserved
        return seg
    }

    private fun windowsOptions(arena: Arena): MemorySegment? {
        if (!IS_WINDOWS) return null

        val path = xAudio29DllPath ?: Native.extractBundledFile(XAUDIO_DLL) ?: return null

        val seg = arena.allocate(WINDOWS_LAYOUT)
        seg.setInt32(0, WINDOWS_API_LATEST)
        seg.setInt64(8, arena.allocCString(path).address())
        return seg
    }

    public companion object {
        public const val API_LATEST: Int = 3

        internal const val WINDOWS_API_LATEST: Int = 1
        internal const val XAUDIO_DLL: String = "xaudio2_9redist.dll"

        internal val IS_WINDOWS: Boolean =
            System.getProperty("os.name").orEmpty().lowercase().contains("win")

        internal val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("ApiVersion"),
            MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS.withName("PlatformSpecificOptions"),
            ValueLayout.JAVA_INT.withName("BackgroundMode"),
            MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS.withName("Reserved"),
        )

        internal val WINDOWS_LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("ApiVersion"),
            MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS.withName("XAudio29DllPath"),
        )
    }
}
