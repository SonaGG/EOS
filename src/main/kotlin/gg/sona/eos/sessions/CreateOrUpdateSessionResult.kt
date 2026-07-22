package gg.sona.eos.sessions

import gg.sona.eos.EosResult

public class CreateOrUpdateSessionResult(
    public val result: EosResult,
    public val sessionId: String,
    public val sessionName: String,
)