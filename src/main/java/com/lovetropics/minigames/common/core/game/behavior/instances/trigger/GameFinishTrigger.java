package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContext;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.MapCodec;

public record GameFinishTrigger(GameActionList<Void> actions) implements IGameBehavior {
	public static final MapCodec<GameFinishTrigger> CODEC = GameActionList.VOID_MAP_CODEC.xmap(GameFinishTrigger::new, GameFinishTrigger::actions);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		actions.register(game, events);
		events.listen(GamePhaseEvents.FINISH, () -> actions.apply(game, GameActionContext.EMPTY));
	}
}
