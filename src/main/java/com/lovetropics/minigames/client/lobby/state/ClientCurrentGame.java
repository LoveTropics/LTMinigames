package com.lovetropics.minigames.client.lobby.state;

import com.lovetropics.minigames.common.core.game.GamePhaseType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

public record ClientCurrentGame(
		ClientGameDefinition definition,
		GamePhaseType phase,
		Optional<Component> error
) {
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientCurrentGame> STREAM_CODEC = StreamCodec.composite(
			ClientGameDefinition.STREAM_CODEC, ClientCurrentGame::definition,
			GamePhaseType.STREAM_CODEC, ClientCurrentGame::phase,
			ComponentSerialization.OPTIONAL_STREAM_CODEC, ClientCurrentGame::error,
			ClientCurrentGame::new
	);

	public ClientCurrentGame(ClientGameDefinition definition, GamePhaseType phaseType) {
		this(definition, phaseType, Optional.empty());
	}

	public ClientCurrentGame withError(Component error) {
		return new ClientCurrentGame(definition, phase, Optional.of(error));
	}
}
