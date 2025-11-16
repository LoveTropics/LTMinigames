package com.lovetropics.minigames.common.core.game.behavior.instances.trigger.phase;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.context.ContextMap;

public record GameTickTrigger(GameActionList actions) implements IGameBehavior {
	public static final MapCodec<GameTickTrigger> CODEC = GameActionList.MAP_CODEC
			.xmap(GameTickTrigger::new, GameTickTrigger::actions);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		actions.register(game, events);
		events.listen(GamePhaseEvents.TICK, () -> actions.apply(game, ContextMap.EMPTY));
	}
}
