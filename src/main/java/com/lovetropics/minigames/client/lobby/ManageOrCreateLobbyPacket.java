package com.lovetropics.minigames.client.lobby;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.command.game.ManageGameLobbyCommand;
import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import com.lovetropics.minigames.common.core.game.impl.GameLobbyManager;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ManageOrCreateLobbyPacket() implements CustomPacketPayload {
	public static final Type<ManageOrCreateLobbyPacket> TYPE = new Type<>(LoveTropics.id("lobby_manage"));

	public static final ManageOrCreateLobbyPacket INSTANCE = new ManageOrCreateLobbyPacket();

	public static final StreamCodec<ByteBuf, ManageOrCreateLobbyPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	public static void handle(ManageOrCreateLobbyPacket message, IPayloadContext context) {
		if (!(context.player() instanceof ServerPlayer player)) {
			return;
		}
		if (!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
			player.sendSystemMessage(GameTexts.Commands.NO_MANAGE_PERMISSION);
			return;
		}
		GameLobby lobby = GameLobbyManager.get().getLobbyFor(player);
		if (lobby != null) {
			if (!lobby.getManagement().startManaging(player)) {
				player.sendSystemMessage(GameTexts.Commands.NO_MANAGE_PERMISSION);
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
