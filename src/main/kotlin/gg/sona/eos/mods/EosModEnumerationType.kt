package gg.sona.eos.mods

enum class EosModEnumerationType(val value: Int) {
    RecentlyInstalled(0);

    companion object {
        internal fun fromValue(v: Int): EosModEnumerationType =
            entries.firstOrNull { it.value == v } ?: RecentlyInstalled
    }
}
