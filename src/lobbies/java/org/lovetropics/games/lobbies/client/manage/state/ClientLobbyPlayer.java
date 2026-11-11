package org.lovetropics.games.lobbies.client.manage.state;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.GamePhaseType;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.lobbies.GameLobby;
import org.lovetropics.games.lobbies.client.state.ClientCurrentGame;

import java.util.UUID;

public record ClientLobbyPlayer(
		UUID uuid,
		String name,
		@Nullable PlayerRole playingRole
) {
	public static ClientLobbyPlayer from(GameLobby lobby, ServerPlayer player) {
		return new ClientLobbyPlayer(player.getUUID(), player.getGameProfile().name(), roleFor(lobby, player));
	}

	private static PlayerRole roleFor(GameLobby lobby, ServerPlayer player) {
		// roles are only allocated from the lobby selections once the game starts, so while waiting, show the role the player will get
		ClientCurrentGame currentGame = lobby.getClientCurrentGame();
		IGamePhase topPhase = lobby.getTopPhase();
		if (topPhase != null && currentGame != null && currentGame.phase() == GamePhaseType.PLAYING) {
			PlayerRole role = topPhase.getRoleFor(player);
			if (role != null) {
				return role;
			}
		}
		return lobby.getPlayers().getLobbyRole(player.getUUID());
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUUID(uuid);
		ByteBufCodecs.PLAYER_NAME.encode(buffer, name);
		encodeRole(buffer, playingRole);
	}

	public static ClientLobbyPlayer decode(FriendlyByteBuf buffer) {
		return new ClientLobbyPlayer(
				buffer.readUUID(),
				ByteBufCodecs.PLAYER_NAME.decode(buffer),
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
