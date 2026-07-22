package gg.sona.eos.anticheat.server

import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientHandle

public class MessageToClientInfo(public val clientHandle: ClientHandle, public val data: ByteArray)
