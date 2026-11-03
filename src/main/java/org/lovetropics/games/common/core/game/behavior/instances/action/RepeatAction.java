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

public record RepeatAction(
		GameActionList action,
		int count
) implements IGameBehavior {
	public static final MapCodec<RepeatAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameActionList.MAP_CODEC.forGetter(RepeatAction::action),
			ExtraCodecs.NON_NEGATIVE_INT.fieldOf("count").forGetter(RepeatAction::count)
	).apply(i, RepeatAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		action.register(game, events);
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			boolean applied = false;
			for (int i = 0; i < count; i++) {
				applied |= action.apply(game, context, targets);
			}
			return applied;
		});
	}

	@Override
	public MapCodec<RepeatAction> codec() {
		return CODEC;
	}
}
