package org.lovetropics.games.common.core.game.behavior.instances.trigger.phase;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameLogicEvents;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;

public record GameOverTrigger(
		GameActionList actions
) implements IGameBehavior {
	public static final MapCodec<GameOverTrigger> CODEC = GameActionList.MAP_CODEC
			.xmap(GameOverTrigger::new, GameOverTrigger::actions);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		actions.register(game, events);
		events.listen(GameLogicEvents.GAME_OVER, winner -> {
			ContextMap context = new ContextMap.Builder().withParameter(GameActionContextKeys.WINNER, winner.name()).create(ContextKeySet.EMPTY);
			actions.apply(game, context, winner.resolveSubjects(game));
		});
	}
}
