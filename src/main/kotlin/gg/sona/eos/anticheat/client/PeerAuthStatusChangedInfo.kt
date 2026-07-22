package gg.sona.eos.anticheat.client

import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientAuthStatus
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientHandle

class PeerAuthStatusChangedInfo(
    val peerHandle: ClientHandle,
    val newStatus: ClientAuthStatus,
)