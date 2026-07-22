package gg.sona.eos.sessions

import gg.sona.eos.EosResult

class CreateOrUpdateSessionResult(
    val result: EosResult,
    val sessionId: String,
    val sessionName: String,
)