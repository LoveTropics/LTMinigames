package com.lovetropics.minigames.common.core.game.behavior.instances.trigger.phase;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.context.ContextMap;

public record GameReadyTrigger(GameActionList<Void> actions) implements IGameBehavior {
	public static final MapCodec<GameReadyTrigger> CODEC = GameActionList.VOID_MAP_CODEC
			.xmap(GameReadyTrigger::new, GameReadyTrigger::actions);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		actions.register(game, events);
		actions.apply(game, ContextMap.EMPTY);
	}
}
