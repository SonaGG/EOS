package gg.sona.eos.lobby

import gg.sona.eos.common.ProductUserId

public class LobbyMemberStatusReceivedInfo(
    public val lobbyId: String,
    public val targetUserId: ProductUserId,
    public val currentStatus: EosLobbyMemberStatus,
    public val previousStatus: EosLobbyMemberStatus,
)
