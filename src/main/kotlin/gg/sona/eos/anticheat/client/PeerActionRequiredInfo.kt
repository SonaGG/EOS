package gg.sona.eos.anticheat.client

import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientAction
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientActionReason
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientHandle

class PeerActionRequiredInfo(
    val peerHandle: ClientHandle,
    val action: ClientAction,
    val reasonCode: ClientActionReason,
    val reasonString: String,
)