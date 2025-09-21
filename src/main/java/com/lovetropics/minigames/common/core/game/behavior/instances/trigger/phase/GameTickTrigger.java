package com.lovetropics.minigames.common.core.game.behavior.instances.trigger.phase;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContext;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.MapCodec;

public record GameTickTrigger(GameActionList<Void> actions) implements IGameBehavior {
	public static final MapCodec<GameTickTrigger> CODEC = GameActionList.VOID_MAP_CODEC
			.xmap(GameTickTrigger::new, GameTickTrigger::actions);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		actions.register(game, events);
		events.listen(GamePhaseEvents.TICK, () -> actions.apply(game, GameActionContext.EMPTY));
	}
}
