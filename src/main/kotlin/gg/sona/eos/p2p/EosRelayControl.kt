package gg.sona.eos.p2p

/** Relay server usage policy. */
public enum class EosRelayControl(val value: Int) {
    NoRelays(0),
    AllowRelays(1),
    ForceRelays(2);

    public companion object {
        public fun fromValue(v: Int): EosRelayControl = entries.firstOrNull { it.value == v } ?: AllowRelays
    }
}
