package gg.sona.eos.rtc

import gg.sona.eos.internal.getInt32
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * A window onto the outgoing audio buffer, valid only for the duration of the callback.
 *
 * Wraps EOS-owned memory rather than copying it: this arrives every 10ms per room, and copying a
 * frame each time would allocate roughly 100 short arrays a second for a measurement that collapses
 * to a single float. Nothing here retains the segment beyond the call, and neither should callers -
 * read what you need, keep the number, drop the buffer.
 *
 * Corresponds to `EOS_RTCAudio_AudioBuffer`: ApiVersion@0, Frames@8, FramesCount@16, SampleRate@20,
 * Channels@24.
 */
class AudioFrames internal constructor(bufferPtr: MemorySegment) {
    private val buffer: MemorySegment = bufferPtr.reinterpret(32)

    /** Frames per channel, not the total sample count. */
    val framesCount: Int get() = buffer.getInt32(16)

    val sampleRate: Int get() = buffer.getInt32(20)

    val channels: Int get() = buffer.getInt32(24)

    /**
     * Root-mean-square amplitude over the whole buffer, normalised to 0.0..1.0.
     *
     * RMS rather than peak: peak reacts to a single click or a keyboard knock, where RMS tracks
     * sustained energy, which is what distinguishes speech from room noise.
     */
    fun rms(): Float {
        val frames = buffer.get(ValueLayout.ADDRESS, 8)
        val samples = framesCount * channels

        if (frames.address() == 0L || samples <= 0) return 0f

        val pcm = frames.reinterpret(samples * 2L)
        var sum = 0.0

        for (i in 0 until samples) {
            val sample = pcm.get(ValueLayout.JAVA_SHORT, i * 2L).toDouble()
            sum += sample * sample
        }

        // Short.MAX_VALUE normalises full-scale PCM16 to 1.0.
        return (kotlin.math.sqrt(sum / samples) / Short.MAX_VALUE).toFloat()
    }
}
