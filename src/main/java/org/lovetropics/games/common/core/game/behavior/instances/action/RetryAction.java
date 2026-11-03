package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;

public record RetryAction(
		GameActionList action,
		int attempts
) implements IGameBehavior {
	public static final MapCodec<RetryAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameActionList.MAP_CODEC.forGetter(RetryAction::action),
			ExtraCodecs.POSITIVE_INT.fieldOf("attempts").forGetter(RetryAction::attempts)
	).apply(i, RetryAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		action.register(game, events);
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			for (int i = 0; i < attempts; i++) {
				if (action.apply(game, context, targets)) {
					return true;
				}
			}
			return false;
		});
	}

	@Override
	public MapCodec<RetryAction> codec() {
		return CODEC;
	}
}
