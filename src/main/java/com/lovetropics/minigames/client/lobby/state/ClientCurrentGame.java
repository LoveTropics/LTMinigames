package com.lovetropics.minigames.client.lobby.state;

import com.lovetropics.minigames.common.core.game.GamePhaseType;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

public final class ClientCurrentGame {
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientCurrentGame> STREAM_CODEC = StreamCodec.composite(
			ClientGameDefinition.STREAM_CODEC, ClientCurrentGame::definition,
			GamePhaseType.STREAM_CODEC, ClientCurrentGame::phase,
			ComponentSerialization.OPTIONAL_STREAM_CODEC, ClientCurrentGame::error,
			ClientCurrentGame::new
	);

	private final ClientGameDefinition definition;
	private final GamePhaseType phase;
	private final Optional<Component> error;

	private ClientCurrentGame(ClientGameDefinition definition, GamePhaseType phase, Optional<Component> error) {
		this.definition = definition;
		this.phase = phase;
		this.error = error;
	}

	public static ClientCurrentGame create(ClientGameDefinition definition, GamePhaseType phaseType) {
		return new ClientCurrentGame(definition, phaseType, Optional.empty());
	}

	public static ClientCurrentGame create(IGamePhase phase) {
		ClientGameDefinition definition = ClientGameDefinition.from(phase.game().definition());
		GamePhaseType phaseType = phase.phaseType();
		return ClientCurrentGame.create(definition, phaseType);
	}

	public ClientCurrentGame withError(Component error) {
		return new ClientCurrentGame(definition, phase, Optional.of(error));
	}

	public ClientGameDefinition definition() {
		return definition;
	}

	public GamePhaseType phase() {
		return phase;
	}

	public Optional<Component> error() {
		return error;
	}
}
