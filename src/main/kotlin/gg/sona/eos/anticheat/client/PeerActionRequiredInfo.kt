package gg.sona.eos.anticheat.client

import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientAction
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientActionReason
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientHandle

public class PeerActionRequiredInfo(
    public val peerHandle: ClientHandle,
    public val action: ClientAction,
    public val reasonCode: ClientActionReason,
    public val reasonString: String,
)