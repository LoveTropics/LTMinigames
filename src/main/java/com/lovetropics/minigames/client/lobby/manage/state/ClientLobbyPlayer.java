package com.lovetropics.minigames.client.lobby.manage.state;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;

public final class ClientLobbyPlayer {
	private final UUID uuid;
	@Nullable
	private final PlayerRole playingRole;

	private ClientLobbyPlayer(UUID uuid, @Nullable PlayerRole playingRole) {
		this.uuid = uuid;
		this.playingRole = playingRole;
	}

	public static ClientLobbyPlayer from(GameLobby lobby, ServerPlayer player) {
		IGamePhase currentPhase = lobby.getActivePhase();
		PlayerRole playingRole = currentPhase != null ? currentPhase.getRoleFor(player) : null;
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

	@Nullable
	private static PlayerRole decodeRole(FriendlyByteBuf buffer) {
		int ordinal = buffer.readVarInt() - 1;
		if (ordinal >= 0 && ordinal < PlayerRole.ROLES.length) {
			return PlayerRole.ROLES[ordinal];
		} else {
			return null;
		}
	}

	public UUID uuid() {
		return uuid;
	}

	@Nullable
	public PlayerRole playingRole() {
		return playingRole;
	}
}
