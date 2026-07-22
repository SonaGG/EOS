package gg.sona.eos.p2p

/** Network connection type for a P2P connection. */
public enum class EosNetworkConnectionType(val value: Int) {
    NoConnection(0),
    DirectConnection(1),
    RelayedConnection(2);

    public companion object {
        public fun fromValue(v: Int): EosNetworkConnectionType = entries.firstOrNull { it.value == v } ?: NoConnection
    }
}
