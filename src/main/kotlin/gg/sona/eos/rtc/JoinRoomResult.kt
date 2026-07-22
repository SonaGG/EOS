package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

/** Result of [EosRtc.joinRoom]. */
public class JoinRoomResult(
    public val result: gg.sona.eos.EosResult,
    public val localUserId: ProductUserId,
    public val roomName: String,
)