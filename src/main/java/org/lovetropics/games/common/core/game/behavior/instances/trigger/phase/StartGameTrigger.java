package org.lovetropics.games.common.core.game.behavior.instances.trigger.phase;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.context.ContextMap;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;

public record StartGameTrigger(GameActionList actions) implements IGameBehavior {
	public static final MapCodec<StartGameTrigger> CODEC = GameActionList.MAP_CODEC.xmap(StartGameTrigger::new, StartGameTrigger::actions);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		actions.register(game, events);
		events.listen(GamePhaseEvents.START, initiator -> actions.apply(game, ContextMap.EMPTY));
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
