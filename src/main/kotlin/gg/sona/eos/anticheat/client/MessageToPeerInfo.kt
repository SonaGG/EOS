package gg.sona.eos.anticheat.client

import gg.sona.eos.anticheat.common.EosAntiCheatCommon.ClientHandle

public class MessageToPeerInfo(public val peerHandle: ClientHandle, public val data: ByteArray)
