package gg.sona.eos.p2p

/** Packet reliability mode. */
enum class EosPacketReliability(val value: Int) {
    UnreliableUnordered(0),
    ReliableUnordered(1),
    ReliableOrdered(2);

    companion object {
        fun fromValue(v: Int): EosPacketReliability =
            entries.firstOrNull { it.value == v } ?: UnreliableUnordered
    }
}
