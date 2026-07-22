package gg.sona.eos.kws

import gg.sona.eos.EosResult

public class QueryAgeGateResult(
    public val result: EosResult,
    public val countryCode: String,
    public val ageOfConsent: Int,
)