package org.lovetropics.games.lobbies.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lovetropics.games.lobbies.GameLobby;
import org.lovetropics.games.lobbies.LobbiesMod;
import org.lovetropics.games.lobbies.client.ClientLobbyManager;

public record JoinedLobbyMessage(int id) implements CustomPacketPayload {
	public static final Type<JoinedLobbyMessage> TYPE = new Type<>(LobbiesMod.id("joined_lobby"));

	public static final StreamCodec<ByteBuf, JoinedLobbyMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, JoinedLobbyMessage::id,
			JoinedLobbyMessage::new
	);

	public static JoinedLobbyMessage create(GameLobby lobby) {
		return new JoinedLobbyMessage(lobby.getMetadata().id().networkId());
	}

	public static void handle(JoinedLobbyMessage message, IPayloadContext context) {
		ClientLobbyManager.setJoined(message.id);
	}

	@Override
	public Type<JoinedLobbyMessage> type() {
		return TYPE;
	}
}
