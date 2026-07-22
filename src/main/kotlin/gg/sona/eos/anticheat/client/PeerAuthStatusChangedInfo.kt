package gg.sona.eos.anticheat.client

import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientAuthStatus
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientHandle

public class PeerAuthStatusChangedInfo(
    public val peerHandle: ClientHandle,
    public val newStatus: ClientAuthStatus,
)