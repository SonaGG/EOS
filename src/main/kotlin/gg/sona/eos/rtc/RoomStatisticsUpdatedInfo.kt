package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

/** Periodic room statistics update. */
public class RoomStatisticsUpdatedInfo(
    public val localUserId: ProductUserId,
    public val roomName: String,
    public val statisticJson: String,
)