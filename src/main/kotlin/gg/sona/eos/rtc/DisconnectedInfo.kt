package gg.sona.eos.rtc

import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId

/** Disconnection info. */
public class DisconnectedInfo(
    public val result: EosResult,
    public val localUserId: ProductUserId,
    public val roomName: String,
)
