package gg.sona.eos.common

/**
 * Online platform identifier returned by EOS for the current process.
 */
enum class EosOnlinePlatform {
    Unknown,
    Epic,
    Psn,
    Nintendo,
    XboxLive,
    Steam;

    companion object {
        internal fun fromValue(v: Int): EosOnlinePlatform = when (v) {
            100 -> Epic
            1000 -> Psn
            2000 -> Nintendo
            3000 -> XboxLive
            4000 -> Steam
            else -> Unknown
        }
    }
}