package gg.sona.eos.p2p

/** Reason a P2P connection was closed. */
enum class EosConnectionClosedReason(val value: Int) {
    Unknown(0),
    ClosedByLocalUser(1),
    ClosedByPeer(2),
    TimedOut(3),
    TooManyConnections(4),
    InvalidMessage(5),
    InvalidData(6),
    ConnectionFailed(7),
    ConnectionClosed(8),
    NegotiationFailed(9),
    UnexpectedError(10),
    ConnectionIgnored(11);

    companion object {
        fun fromValue(v: Int): EosConnectionClosedReason = entries.firstOrNull { it.value == v } ?: Unknown
    }
}
