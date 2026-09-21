package org.lovetropics.games.lobbies.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lovetropics.games.lobbies.GameLobby;
import org.lovetropics.games.lobbies.GameLobbyManager;
import org.lovetropics.games.lobbies.GameLobbyTexts;
import org.lovetropics.games.lobbies.LobbiesMod;

public record LeaveLobbyPacket() implements CustomPacketPayload {
	public static final Type<LeaveLobbyPacket> TYPE = new Type<>(LobbiesMod.id("leave_lobby"));

	public static final LeaveLobbyPacket INSTANCE = new LeaveLobbyPacket();

	public static final StreamCodec<ByteBuf, LeaveLobbyPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	public static void handle(LeaveLobbyPacket message, IPayloadContext context) {
		if (!(context.player() instanceof ServerPlayer player)) {
			return;
		}
		GameLobby lobby = GameLobbyManager.get().getLobbyFor(player);
		if (lobby != null && lobby.getPlayers().remove(player, false)) {
			player.sendSystemMessage(GameLobbyTexts.Commands.leftLobby(lobby));
		} else {
			player.sendSystemMessage(GameLobbyTexts.Commands.NOT_IN_LOBBY.copy().withStyle(ChatFormatting.RED));
		}
	}

	@Override
	public Type<LeaveLobbyPacket> type() {
		return TYPE;
	}
}
