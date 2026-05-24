package com.lovetropics.minigames.client.lobby.manage;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.lobby.manage.state.update.ClientLobbyUpdate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientManageLobbyMessage(int id, ClientLobbyUpdate.Set updates) implements CustomPacketPayload {
	public static final Type<ClientManageLobbyMessage> TYPE = new Type<>(LoveTropics.id("client_manage_lobby"));

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
