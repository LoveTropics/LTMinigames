package com.lovetropics.minigames.client.lobby.manage.state;

import com.lovetropics.minigames.client.lobby.state.ClientBehaviorList;
import com.lovetropics.minigames.client.lobby.state.ClientGameDefinition;
import com.lovetropics.minigames.common.core.game.lobby.QueuedGame;
import net.minecraft.network.RegistryFriendlyByteBuf;

import javax.annotation.Nullable;

public final class ClientLobbyQueuedGame {
	private final ClientGameDefinition definition;
	private final ClientBehaviorList playingConfigs;
	@Nullable
	private final ClientBehaviorList waitingConfigs;

	private ClientLobbyQueuedGame(ClientGameDefinition definition, ClientBehaviorList playingConfigs, @Nullable ClientBehaviorList waitingConfigs) {
		this.definition = definition;
		this.playingConfigs = playingConfigs;
		this.waitingConfigs = waitingConfigs;
	}

	public static ClientLobbyQueuedGame from(QueuedGame game) {
		ClientGameDefinition definition = ClientGameDefinition.from(game.definition());
		ClientBehaviorList playingConfigs = ClientBehaviorList.from(game.playingBehaviors());
		ClientBehaviorList waitingConfigs = game.waitingBehaviors() != null ? ClientBehaviorList.from(game.waitingBehaviors()) : null;
		return new ClientLobbyQueuedGame(definition, playingConfigs, waitingConfigs);
	}

	public void encode(RegistryFriendlyByteBuf buffer) {
		definition.encode(buffer);
		playingConfigs.encode(buffer);

		buffer.writeBoolean(waitingConfigs != null);
		if (waitingConfigs != null) {
			waitingConfigs.encode(buffer);
		}
	}

	public static ClientLobbyQueuedGame decode(RegistryFriendlyByteBuf buffer) {
		ClientGameDefinition definition = ClientGameDefinition.decode(buffer);
		ClientBehaviorList playingConfigs = ClientBehaviorList.decode(buffer);
		ClientBehaviorList waitingConfigs = buffer.readBoolean() ? ClientBehaviorList.decode(buffer) : null;
		return new ClientLobbyQueuedGame(definition, playingConfigs, waitingConfigs);
	}

	public ClientGameDefinition definition() {
		return definition;
	}

	public ClientBehaviorList playingConfigs() {
		return playingConfigs;
	}

	@Nullable
	public ClientBehaviorList waitingConfigs() {
		return waitingConfigs;
	}
}
