package gg.sona.eos.anticheat.server

import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientAuthStatus
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientHandle

class ClientAuthStatusChangedInfo(
    val clientHandle: ClientHandle,
    val newStatus: ClientAuthStatus,
)