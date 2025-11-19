package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.SubGameEvents;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record InSubGamesBehavior(
		IGameBehavior behavior,
		boolean includeTop,
		boolean recursive
) implements IGameBehavior {
	public static final MapCodec<InSubGamesBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IGameBehavior.CODEC.fieldOf("behavior").forGetter(InSubGamesBehavior::behavior),
			Codec.BOOL.optionalFieldOf("include_top", false).forGetter(InSubGamesBehavior::includeTop),
			Codec.BOOL.optionalFieldOf("recursive", false).forGetter(InSubGamesBehavior::recursive)
	).apply(i, InSubGamesBehavior::new));

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		if (includeTop) {
			behavior.registerState(game, phaseState, instanceState);
		}
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		if (includeTop) {
			behavior.register(game, events);
		}
		events.listen(SubGameEvents.CREATE, (subGame, subEvents) -> {
			behavior.registerState(subGame, subGame.state(), subGame.instanceState());
			behavior.register(subGame, subEvents);
			if (recursive) {
				register(subGame, subEvents);
			}
		});
	}
}
