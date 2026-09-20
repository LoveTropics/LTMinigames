package org.lovetropics.games.lobbies.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lovetropics.games.lobbies.LobbiesMod;
import org.lovetropics.games.lobbies.client.manage.ClientLobbyManagement;
import org.lovetropics.games.lobbies.client.manage.state.update.ClientLobbyUpdate;

public record ClientManageLobbyMessage(int id, ClientLobbyUpdate.Set updates) implements CustomPacketPayload {
	public static final Type<ClientManageLobbyMessage> TYPE = new Type<>(LobbiesMod.id("client_manage_lobby"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ClientManageLobbyMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ClientManageLobbyMessage::id,
			ClientLobbyUpdate.Set.STREAM_CODEC, ClientManageLobbyMessage::updates,
			ClientManageLobbyMessage::new
	);

	public static void handle(ClientManageLobbyMessage message, IPayloadContext context) {
		ClientLobbyManagement.update(message.id, message.updates);
	}

	@Override
	public Type<ClientManageLobbyMessage> type() {
		return TYPE;
	}
}
