package gg.sona.eos.p2p

/** Packet reliability mode. */
public enum class EosPacketReliability(val value: Int) {
    UnreliableUnordered(0),
    ReliableUnordered(1),
    ReliableOrdered(2);

    public companion object {
        public fun fromValue(v: Int): EosPacketReliability = entries.firstOrNull { it.value == v } ?: UnreliableUnordered
    }
}
