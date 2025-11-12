package com.lovetropics.minigames.client.lobby.manage.state;

import com.lovetropics.minigames.client.lobby.state.ClientGameDefinition;
import com.lovetropics.minigames.common.core.game.lobby.QueuedGame;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record ClientLobbyQueuedGame(ClientGameDefinition definition) {
	public static ClientLobbyQueuedGame from(QueuedGame game) {
		ClientGameDefinition definition = ClientGameDefinition.from(game.definition());
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
