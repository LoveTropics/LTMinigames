package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;

import java.util.List;

public record SequenceAction(
		List<GameActionList> actions
) implements IGameBehavior {
	public static final MapCodec<SequenceAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameActionList.CODEC.listOf().fieldOf("sequence").forGetter(SequenceAction::actions)
	).apply(i, SequenceAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		for (GameActionList action : actions) {
			action.register(game, events);
		}
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			for (GameActionList action : actions) {
				if (!action.apply(game, context, targets)) {
					return false;
				}
			}
			return true;
		});
	}
}
