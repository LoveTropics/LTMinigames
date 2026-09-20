package org.lovetropics.games.common.core.game.behavior.instances.trigger;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.context.ContextMap;

public record GameFinishTrigger(GameActionList actions) implements IGameBehavior {
	public static final MapCodec<GameFinishTrigger> CODEC = GameActionList.MAP_CODEC.xmap(GameFinishTrigger::new, GameFinishTrigger::actions);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		actions.register(game, events);
		events.listen(GamePhaseEvents.FINISH, () -> actions.apply(game, ContextMap.EMPTY));
	}
}
