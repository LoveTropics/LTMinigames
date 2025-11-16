package com.lovetropics.minigames.common.core.game.behavior.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.MapCodec;

import java.util.function.Supplier;

public record ApplyToAction(GameActionList actions) implements IGameBehavior {
	public static final MapCodec<ApplyToAction> CODEC = GameActionList.MAP_CODEC.xmap(ApplyToAction::new, ApplyToAction::actions);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		actions.register(game, events);
		events.listen(GameActionEvents.APPLY, (context, targets) ->
				actions.apply(game, context, targets)
		);
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.APPLY_TO;
	}
}
