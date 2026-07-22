package gg.sona.eos.anticheat.client

/** Anti-Cheat operating mode. */
enum class EosAntiCheatClientMode(val value: Int) {
    Invalid(0),
    ClientServer(1),
    PeerToPeer(2);

    companion object {
        fun fromValue(v: Int): EosAntiCheatClientMode = entries.firstOrNull { it.value == v } ?: Invalid
    }
}