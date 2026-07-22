package gg.sona.eos.rtc

import gg.sona.eos.EosResult

public class QueryJoinRoomTokenResult(
    public val result: EosResult,
    public val roomName: String,
    public val clientBaseUrl: String,
    public val queryId: Long,
    public val tokenCount: Int,
)
