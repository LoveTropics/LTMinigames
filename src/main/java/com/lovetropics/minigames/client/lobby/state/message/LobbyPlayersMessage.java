package com.lovetropics.minigames.client.lobby.state.message;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.lobby.state.ClientLobbyManager;
import com.lovetropics.minigames.common.core.game.lobby.IGameLobby;
import com.lovetropics.minigames.common.core.game.lobby.IGameLobbyPlayers;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Set;
import java.util.UUID;

public record LobbyPlayersMessage(int id, Set<UUID> players) implements CustomPacketPayload {
	public static final Type<LobbyPlayersMessage> TYPE = new Type<>(LoveTropics.location("lobby_players"));

	public static final StreamCodec<ByteBuf, LobbyPlayersMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, LobbyPlayersMessage::id,
			UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.collection(ObjectOpenHashSet::new)), LobbyPlayersMessage::players,
			LobbyPlayersMessage::new
	);

	public static LobbyPlayersMessage update(IGameLobby lobby) {
		IGameLobbyPlayers players = lobby.getPlayers();
		Set<UUID> playerIds = new ObjectOpenHashSet<>(players.size());
		for (ServerPlayer player : players) {
			playerIds.add(player.getUUID());
		}
		return new LobbyPlayersMessage(lobby.getMetadata().id().networkId(), playerIds);
	}

	public static void handle(LobbyPlayersMessage message, IPayloadContext context) {
		ClientLobbyManager.get(message.id).ifPresent(state -> state.setPlayers(message.players));
	}

	@Override
	public Type<LobbyPlayersMessage> type() {
		return TYPE;
	}
}
