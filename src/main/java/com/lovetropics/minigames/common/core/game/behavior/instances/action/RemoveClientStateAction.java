package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RemoveClientStateAction(GameClientStateType<?> type) implements IGameBehavior {
	public static final MapCodec<RemoveClientStateAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameClientStateTypes.TYPE_CODEC.fieldOf("state").forGetter(RemoveClientStateAction::type)
	).apply(i, RemoveClientStateAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.applyToPlayers(game, (context, target) -> {
			GameClientState.removeFromPlayer(type, target);
			return true;
		});
	}
}
