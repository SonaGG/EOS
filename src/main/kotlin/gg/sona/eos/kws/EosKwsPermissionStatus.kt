package gg.sona.eos.kws

/** Status of a KWS permission grant. */
enum class EosKwsPermissionStatus(val value: Int) {
    Granted(0),
    Rejected(1),
    Pending(2);

    companion object {
        internal fun fromValue(v: Int): EosKwsPermissionStatus =
            entries.firstOrNull { it.value == v } ?: Granted
    }
}
