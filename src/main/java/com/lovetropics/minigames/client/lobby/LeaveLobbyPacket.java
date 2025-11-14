package com.lovetropics.minigames.client.lobby;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import com.lovetropics.minigames.common.core.game.impl.GameLobbyManager;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LeaveLobbyPacket() implements CustomPacketPayload {
	public static final Type<LeaveLobbyPacket> TYPE = new Type<>(LoveTropics.location("leave_lobby"));

	public static final LeaveLobbyPacket INSTANCE = new LeaveLobbyPacket();

	public static final StreamCodec<ByteBuf, LeaveLobbyPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	public static void handle(LeaveLobbyPacket message, IPayloadContext context) {
		if (!(context.player() instanceof ServerPlayer player)) {
			return;
		}
		GameLobby lobby = GameLobbyManager.get().getLobbyFor(player);
		if (lobby != null && lobby.getPlayers().remove(player, false)) {
			player.sendSystemMessage(GameTexts.Commands.leftLobby(lobby));
		} else {
			player.sendSystemMessage(GameTexts.Commands.NOT_IN_LOBBY.copy().withStyle(ChatFormatting.RED));
		}
	}

	@Override
	public Type<LeaveLobbyPacket> type() {
		return TYPE;
	}
}
