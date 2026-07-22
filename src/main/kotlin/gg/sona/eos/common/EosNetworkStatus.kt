package gg.sona.eos.common

/**
 * Network state as told to the SDK by the application.
 */
enum class EosNetworkStatus(val value: Int) {
    /** Networking is disabled. */
    Disabled(0),

    /** Offline: not connected to the internet. */
    Offline(1),

    /** Online. */
    Online(2);

    companion object {
        fun fromValue(v: Int): EosNetworkStatus = entries.firstOrNull { it.value == v } ?: Offline
    }
}