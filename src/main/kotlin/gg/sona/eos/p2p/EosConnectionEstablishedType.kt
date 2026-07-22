package gg.sona.eos.p2p

/** Type of an established connection. */
public enum class EosConnectionEstablishedType(val value: Int) {
    NewConnection(0),
    Reconnection(1);

    public companion object {
        public fun fromValue(v: Int): EosConnectionEstablishedType = entries.firstOrNull { it.value == v } ?: NewConnection
    }
}