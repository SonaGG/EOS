package gg.sona.eos.anticheat.server

import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientAction
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientActionReason
import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientHandle

public class ClientActionRequiredInfo(
    public val clientHandle: ClientHandle,
    public val action: ClientAction,
    public val reasonCode: ClientActionReason,
    public val reasonString: String,
)
