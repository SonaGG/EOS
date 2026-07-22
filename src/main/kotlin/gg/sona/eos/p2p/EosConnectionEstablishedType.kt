package gg.sona.eos.p2p

/** Type of an established connection. */
enum class EosConnectionEstablishedType(val value: Int) {
    NewConnection(0),
    Reconnection(1);

    companion object {
        fun fromValue(v: Int): EosConnectionEstablishedType =
            entries.firstOrNull { it.value == v } ?: NewConnection
    }
}