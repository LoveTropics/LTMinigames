package com.lovetropics.minigames.client.lobby.manage;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.lobby.manage.state.update.ServerLobbyUpdate;
import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import com.lovetropics.minigames.common.core.game.impl.GameLobbyManager;
import com.lovetropics.minigames.common.core.game.impl.LobbyManagement;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record ServerManageLobbyMessage(int id, Optional<ServerLobbyUpdate.Set> updates) implements CustomPacketPayload {
	public static final Type<ServerManageLobbyMessage> TYPE = new Type<>(LoveTropics.id("server_manage_lobby"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ServerManageLobbyMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ServerManageLobbyMessage::id,
			ServerLobbyUpdate.Set.STREAM_CODEC.apply(ByteBufCodecs::optional), ServerManageLobbyMessage::updates,
			ServerManageLobbyMessage::new
	);

	public static ServerManageLobbyMessage update(int id, ServerLobbyUpdate.Set updates) {
		return new ServerManageLobbyMessage(id, Optional.of(updates));
	}

	public static ServerManageLobbyMessage stop(int id) {
		return new ServerManageLobbyMessage(id, Optional.empty());
	}

	public static void handle(ServerManageLobbyMessage message, IPayloadContext context) {
		GameLobby lobby = GameLobbyManager.get().getLobbyByNetworkId(message.id);
		ServerPlayer player = (ServerPlayer) context.player();
		if (lobby == null) {
			return;
		}

		LobbyManagement management = lobby.getManagement();
		if (message.updates.isPresent()) {
			if (management.canManage(player.createCommandSourceStack())) {
				message.updates.get().applyTo(management);
			}
		} else {
			management.stopManaging(player);
		}
	}

	@Override
	public Type<ServerManageLobbyMessage> type() {
		return TYPE;
	}
}
