package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContext;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.GameProgressionState;
import com.lovetropics.minigames.common.core.game.state.ProgressionPoint;
import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public record PhaseChangeTrigger(Map<ProgressionPoint, GameActionList> phases) implements IGameBehavior {
	public static final Codec<PhaseChangeTrigger> CODEC = Codec.unboundedMap(ProgressionPoint.CODEC, GameActionList.CODEC)
			.xmap(PhaseChangeTrigger::new, PhaseChangeTrigger::phases);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		GameProgressionState progression = game.getState().getOrThrow(GameProgressionState.KEY);

		for (GameActionList actions : phases.values()) {
			actions.register(game, events);
		}

		List<Map.Entry<ProgressionPoint, GameActionList>> remaining = new ArrayList<>(phases.entrySet());

		events.listen(GamePhaseEvents.TICK, () -> {
			Iterator<Map.Entry<ProgressionPoint, GameActionList>> iterator = remaining.iterator();
			while (iterator.hasNext()) {
				Map.Entry<ProgressionPoint, GameActionList> entry = iterator.next();
				if (progression.isAfter(entry.getKey())) {
					entry.getValue().applyPlayer(game, GameActionContext.EMPTY);
					iterator.remove();
				}
			}
		});
	}
}
