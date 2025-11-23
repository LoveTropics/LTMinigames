package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;

import java.util.Optional;

public record RandomizeAction(
		WeightedList<GameActionList> options
) implements IGameBehavior {
	public static final MapCodec<RandomizeAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			WeightedList.nonEmptyCodec(GameActionList.CODEC).fieldOf("options").forGetter(RandomizeAction::options)
	).apply(i, RandomizeAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		for (Weighted<GameActionList> entry : options.unwrap()) {
			entry.value().register(game, events);
		}

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			Optional<GameActionList> action = options.getRandom(game.random());
			return action.isPresent() && action.get().apply(game, context, targets);
		});
	}
}
