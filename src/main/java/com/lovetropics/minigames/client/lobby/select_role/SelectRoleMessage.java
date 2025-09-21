package com.lovetropics.minigames.client.lobby.select_role;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.IGameManager;
import com.lovetropics.minigames.common.core.game.lobby.IGameLobby;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectRoleMessage(int lobbyId, boolean play) implements CustomPacketPayload {
	public static final Type<SelectRoleMessage> TYPE = new Type<>(LoveTropics.location("select_role"));

	public static final StreamCodec<ByteBuf, SelectRoleMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SelectRoleMessage::lobbyId,
			ByteBufCodecs.BOOL, SelectRoleMessage::play,
			SelectRoleMessage::new
	);

	public static void handle(SelectRoleMessage message, IPayloadContext context) {
		ServerPlayer player = (ServerPlayer) context.player();
		IGameLobby lobby = IGameManager.get().getLobbyByNetworkId(message.lobbyId);
		if (lobby != null) {
			PlayerRole role = message.play ? PlayerRole.PARTICIPANT : PlayerRole.SPECTATOR;
			lobby.getPlayers().getRoleSelections().acceptResponse(player, role);
		}
	}

	@Override
	public Type<SelectRoleMessage> type() {
		return TYPE;
	}
}
