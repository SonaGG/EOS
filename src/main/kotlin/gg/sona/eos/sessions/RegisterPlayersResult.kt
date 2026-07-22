package gg.sona.eos.sessions

import gg.sona.eos.EosResult

public class RegisterPlayersResult(
    public val result: EosResult,
    public val sessionName: String,
    public val affectedCount: Int,
)
