package gg.sona.eos.p2p

/** NAT type as reported by the P2P subsystem. */
enum class EosNatType(val value: Int) {
    Unknown(0),
    Open(1),
    Moderate(2),
    Strict(3);

    companion object {
        fun fromValue(v: Int): EosNatType = entries.firstOrNull { it.value == v } ?: Unknown
    }
}