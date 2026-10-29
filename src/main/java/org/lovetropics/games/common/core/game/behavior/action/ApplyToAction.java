package org.lovetropics.games.common.core.game.behavior.action;

import com.mojang.serialization.MapCodec;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;

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
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
