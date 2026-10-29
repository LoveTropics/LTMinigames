package org.lovetropics.games.common.core.game.behavior.instances.statistics;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticsMap;

public record SetXpToStatisticBehavior(StatisticKey<Integer> statistic) implements IGameBehavior {
	public static final MapCodec<SetXpToStatisticBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(SetXpToStatisticBehavior::statistic)
	).apply(i, SetXpToStatisticBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.TICK, player -> {
			StatisticsMap statistics = game.statistics().forPlayer(player);
			int level = statistics.getInt(statistic);
			if (level != player.experienceLevel) {
				player.setExperienceLevels(level);
				player.experienceProgress = 1.0f;
			}
		});
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
