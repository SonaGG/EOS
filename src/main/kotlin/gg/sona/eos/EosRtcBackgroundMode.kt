package gg.sona.eos

public enum class EosRtcBackgroundMode(val value: Int) {
    LeaveRooms(0),
    KeepRoomsAlive(1);

    public companion object {
        public fun fromValue(v: Int): EosRtcBackgroundMode = entries.firstOrNull { it.value == v } ?: LeaveRooms
    }
}
