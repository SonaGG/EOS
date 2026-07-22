package gg.sona.eos.p2p

/** Relay server usage policy. */
enum class EosRelayControl(val value: Int) {
    NoRelays(0),
    AllowRelays(1),
    ForceRelays(2);

    companion object {
        fun fromValue(v: Int): EosRelayControl = entries.firstOrNull { it.value == v } ?: AllowRelays
    }
}
