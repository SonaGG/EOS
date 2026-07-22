package gg.sona.eos.anticheat.client

public class ClientIntegrityViolatedInfo(
    public val violationType: EosAntiCheatClientViolationType,
    public val message: String,
)