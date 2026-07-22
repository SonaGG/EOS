package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

/** Result of [EosRtc.joinRoom]. */
class JoinRoomResult(
    val result: gg.sona.eos.EosResult,
    val localUserId: ProductUserId,
    val roomName: String,
)