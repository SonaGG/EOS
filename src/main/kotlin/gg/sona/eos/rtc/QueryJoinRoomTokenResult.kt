package gg.sona.eos.rtc

import gg.sona.eos.EosResult

class QueryJoinRoomTokenResult(
    val result: EosResult,
    val roomName: String,
    val clientBaseUrl: String,
    val queryId: Long,
    val tokenCount: Int,
)
