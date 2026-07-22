package gg.sona.eos.mods

public enum class EosModEnumerationType(val value: Int) {
    RecentlyInstalled(0);

    public companion object {
        internal fun fromValue(v: Int): EosModEnumerationType = entries.firstOrNull { it.value == v } ?: RecentlyInstalled
    }
}
