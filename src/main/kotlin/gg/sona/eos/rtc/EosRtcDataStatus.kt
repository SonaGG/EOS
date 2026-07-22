package gg.sona.eos.rtc

public enum class EosRtcDataStatus(val value: Int) {
    Unsupported(0),
    Enabled(1),
    Disabled(2);

    public companion object {
        public fun fromValue(v: Int): EosRtcDataStatus = entries.firstOrNull { it.value == v } ?: Unsupported
    }
}