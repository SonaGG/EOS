package gg.sona.eos.rtc

import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId

/** Disconnection info. */
class DisconnectedInfo(
    val result: EosResult,
    val localUserId: ProductUserId,
    val roomName: String,
)
