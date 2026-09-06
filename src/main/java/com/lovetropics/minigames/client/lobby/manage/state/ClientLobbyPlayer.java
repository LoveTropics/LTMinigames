package com.lovetropics.minigames.client.lobby.manage.state;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.Nullable;
import java.util.UUID;

public record ClientLobbyPlayer(
		UUID uuid,
		@Nullable PlayerRole playingRole
) {
	public static ClientLobbyPlayer from(GameLobby lobby, ServerPlayer player) {
		IGamePhase topPhase = lobby.getTopPhase();
		PlayerRole playingRole = topPhase != null ? topPhase.getRoleFor(player) : null;
		return new ClientLobbyPlayer(player.getUUID(), playingRole);
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUUID(uuid);
		encodeRole(buffer, playingRole);
	}

	public static ClientLobbyPlayer decode(FriendlyByteBuf buffer) {
		return new ClientLobbyPlayer(
				buffer.readUUID(),
				decodeRole(buffer)
		);
	}

	private static void encodeRole(FriendlyByteBuf buffer, @Nullable PlayerRole role) {
		if (role != null) {
			buffer.writeVarInt(role.ordinal() + 1);
		} else {
			buffer.writeVarInt(0);
		}
	}

	private static @Nullable PlayerRole decodeRole(FriendlyByteBuf buffer) {
		int ordinal = buffer.readVarInt() - 1;
		if (ordinal >= 0 && ordinal < PlayerRole.ROLES.length) {
			return PlayerRole.ROLES[ordinal];
		} else {
			return null;
		}
	}
}
