package org.lovetropics.games.lobbies.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lovetropics.games.lobbies.LobbiesMod;
import org.lovetropics.games.lobbies.client.ClientLobbyManager;

public record LeftLobbyMessage() implements CustomPacketPayload {
	public static final Type<LeftLobbyMessage> TYPE = new Type<>(LobbiesMod.id("left_lobby"));

	public static final StreamCodec<ByteBuf, LeftLobbyMessage> STREAM_CODEC = StreamCodec.unit(new LeftLobbyMessage());

	public static void handle(LeftLobbyMessage message, IPayloadContext context) {
		ClientLobbyManager.clearJoined();
	}

	@Override
	public Type<LeftLobbyMessage> type() {
		return TYPE;
	}
}
