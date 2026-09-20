package org.lovetropics.games.lobbies.network;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.GameResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lovetropics.games.lobbies.GameLobby;
import org.lovetropics.games.lobbies.GameLobbyManager;
import org.lovetropics.games.lobbies.GameLobbyTexts;
import org.lovetropics.games.lobbies.command.ManageGameLobbyCommand;

public record ManageOrCreateLobbyPacket() implements CustomPacketPayload {
	public static final Type<ManageOrCreateLobbyPacket> TYPE = new Type<>(LoveTropics.id("lobby_manage"));

	public static final ManageOrCreateLobbyPacket INSTANCE = new ManageOrCreateLobbyPacket();

	public static final StreamCodec<ByteBuf, ManageOrCreateLobbyPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	public static void handle(ManageOrCreateLobbyPacket message, IPayloadContext context) {
		if (!(context.player() instanceof ServerPlayer player)) {
			return;
		}
		if (!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
			player.sendSystemMessage(GameLobbyTexts.Commands.NO_MANAGE_PERMISSION);
			return;
		}
		GameLobby lobby = GameLobbyManager.get().getLobbyFor(player);
		if (lobby != null) {
			if (!lobby.getManagement().startManaging(player)) {
				player.sendSystemMessage(GameLobbyTexts.Commands.NO_MANAGE_PERMISSION);
			}
		} else {
			GameResult<GameLobby> result = ManageGameLobbyCommand.createAndJoinLobby(player);
			if (result.isError()) {
				player.sendSystemMessage(result.getError());
			}
		}
	}

	@Override
	public Type<ManageOrCreateLobbyPacket> type() {
		return TYPE;
	}
}
