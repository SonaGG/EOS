package gg.sona.eos.anticheat.server

import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientAuthStatus
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientHandle

public class ClientAuthStatusChangedInfo(
    public val clientHandle: ClientHandle,
    public val newStatus: ClientAuthStatus,
)