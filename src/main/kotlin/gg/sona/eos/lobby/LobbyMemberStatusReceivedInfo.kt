package gg.sona.eos.lobby

import gg.sona.eos.common.ProductUserId

class LobbyMemberStatusReceivedInfo(
    val lobbyId: String,
    val targetUserId: ProductUserId,
    val currentStatus: EosLobbyMemberStatus,
    val previousStatus: EosLobbyMemberStatus,
)
