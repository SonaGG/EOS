package gg.sona.eos.rtc

enum class EosRtcDataStatus(val value: Int) {
    Unsupported(0),
    Enabled(1),
    Disabled(2);

    companion object {
        fun fromValue(v: Int): EosRtcDataStatus = entries.firstOrNull { it.value == v } ?: Unsupported
    }
}