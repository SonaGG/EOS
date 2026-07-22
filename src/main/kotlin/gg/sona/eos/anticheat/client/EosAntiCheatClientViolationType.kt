package gg.sona.eos.anticheat.client

/** Type of a client-side integrity violation. */
enum class EosAntiCheatClientViolationType(val value: Int) {
    Invalid(0),
    IntegrityCatalogNotFound(1),
    IntegrityCatalogError(2),
    IntegrityCatalogCertificateRevoked(3),
    IntegrityCatalogMissingMainExecutable(4),
    GameFileMismatch(5),
    RequiredGameFileNotFound(6),
    UnknownGameFileForbidden(7),
    SystemFileUntrusted(8),
    ForbiddenModuleLoaded(9),
    CorruptedMemory(10),
    ForbiddenToolDetected(11),
    InternalAntiCheatViolation(12),
    CorruptedNetworkMessageFlow(13),
    VirtualMachineNotAllowed(14),
    ForbiddenSystemConfiguration(15);

    companion object {
        fun fromValue(v: Int): EosAntiCheatClientViolationType =
            entries.firstOrNull { it.value == v } ?: Invalid
    }
}