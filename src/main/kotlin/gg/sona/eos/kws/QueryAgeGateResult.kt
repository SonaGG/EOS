package gg.sona.eos.kws

import gg.sona.eos.EosResult

class QueryAgeGateResult(
    val result: EosResult,
    val countryCode: String,
    val ageOfConsent: Int,
)