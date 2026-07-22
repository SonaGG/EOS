package gg.sona.eos.anticheat.server

import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientAction
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientActionReason
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientHandle

class ClientActionRequiredInfo(
    val clientHandle: ClientHandle,
    val action: ClientAction,
    val reasonCode: ClientActionReason,
    val reasonString: String,
)
