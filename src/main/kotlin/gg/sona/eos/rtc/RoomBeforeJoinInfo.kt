package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

/** Pre-join room info. */
public class RoomBeforeJoinInfo(
    public val localUserId: ProductUserId,
    public val roomName: String,
)
