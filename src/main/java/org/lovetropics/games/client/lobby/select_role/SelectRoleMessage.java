package org.lovetropics.games.client.lobby.select_role;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.impl.GameLobby;
import org.lovetropics.games.common.core.game.impl.GameLobbyManager;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectRoleMessage(int lobbyId, boolean play) implements CustomPacketPayload {
	public static final Type<SelectRoleMessage> TYPE = new Type<>(LoveTropics.id("select_role"));

	public static final StreamCodec<ByteBuf, SelectRoleMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SelectRoleMessage::lobbyId,
			ByteBufCodecs.BOOL, SelectRoleMessage::play,
			SelectRoleMessage::new
	);

	public static void handle(SelectRoleMessage message, IPayloadContext context) {
		ServerPlayer player = (ServerPlayer) context.player();
		GameLobby lobby = GameLobbyManager.get().getLobbyByNetworkId(message.lobbyId);
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
