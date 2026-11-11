package org.lovetropics.games.lobbies.client.manage.state;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.lobbies.GameLobby;
import org.lovetropics.games.lobbies.GameLobbyManager;

import java.util.UUID;

public record ClientOnlinePlayer(UUID uuid, String name, @Nullable String lobbyName) {
	private static final int MAX_LOBBY_NAME_LENGTH = 200;

	public static ClientOnlinePlayer from(GameLobbyManager manager, ServerPlayer player) {
		GameLobby lobby = manager.getLobbyFor(player);
		return new ClientOnlinePlayer(player.getUUID(), player.getGameProfile().name(), lobby != null ? lobby.getMetadata().name() : null);
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUUID(uuid);
		ByteBufCodecs.PLAYER_NAME.encode(buffer, name);
		buffer.writeNullable(lobbyName, (b, name) -> b.writeUtf(name, MAX_LOBBY_NAME_LENGTH));
	}

	public static ClientOnlinePlayer decode(FriendlyByteBuf buffer) {
		return new ClientOnlinePlayer(
				buffer.readUUID(),
				ByteBufCodecs.PLAYER_NAME.decode(buffer),
				buffer.readNullable(b -> b.readUtf(MAX_LOBBY_NAME_LENGTH))
		);
	}
}
