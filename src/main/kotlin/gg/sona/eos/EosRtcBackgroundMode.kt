package gg.sona.eos

enum class EosRtcBackgroundMode(val value: Int) {
    LeaveRooms(0),
    KeepRoomsAlive(1);

    companion object {
        fun fromValue(v: Int): EosRtcBackgroundMode = entries.firstOrNull { it.value == v } ?: LeaveRooms
    }
}
