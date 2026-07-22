package gg.sona.eos.sessions

import gg.sona.eos.EosResult

class RegisterPlayersResult(
    val result: EosResult,
    val sessionName: String,
    val affectedCount: Int,
)
