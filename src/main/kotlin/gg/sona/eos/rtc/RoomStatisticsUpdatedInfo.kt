package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

/** Periodic room statistics update. */
class RoomStatisticsUpdatedInfo(
    val localUserId: ProductUserId,
    val roomName: String,
    val statisticJson: String,
)