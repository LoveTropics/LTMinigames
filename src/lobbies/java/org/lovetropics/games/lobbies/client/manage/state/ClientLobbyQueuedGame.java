package org.lovetropics.games.lobbies.client.manage.state;

import net.minecraft.network.RegistryFriendlyByteBuf;
import org.lovetropics.games.lobbies.QueuedGame;
import org.lovetropics.games.lobbies.client.ClientGameDefinition;

public record ClientLobbyQueuedGame(ClientGameDefinition definition) {
	public static ClientLobbyQueuedGame from(QueuedGame game) {
		ClientGameDefinition definition = ClientGameDefinition.from(game.config());
		return new ClientLobbyQueuedGame(definition);
	}

	public void encode(RegistryFriendlyByteBuf buffer) {
		definition.encode(buffer);
	}

	public static ClientLobbyQueuedGame decode(RegistryFriendlyByteBuf buffer) {
		ClientGameDefinition definition = ClientGameDefinition.decode(buffer);
		return new ClientLobbyQueuedGame(definition);
	}
}
