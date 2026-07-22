package gg.sona.eos.stats

import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId

class IngestStatResult(
    val result: EosResult,
    val localUserId: ProductUserId,
    val targetUserId: ProductUserId,
)