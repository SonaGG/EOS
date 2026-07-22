package gg.sona.eos.stats

import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId

public class IngestStatResult(
    public val result: EosResult,
    public val localUserId: ProductUserId,
    public val targetUserId: ProductUserId,
)