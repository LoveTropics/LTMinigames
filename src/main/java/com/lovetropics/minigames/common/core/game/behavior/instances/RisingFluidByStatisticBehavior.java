package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.util.FluidFiller;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class RisingFluidByStatisticBehavior implements IGameBehavior {
	public static final MapCodec<RisingFluidByStatisticBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(b -> b.statistic),
			FluidFiller.Type.CODEC.optionalFieldOf("fill_type", FluidFiller.Type.WATER).forGetter(b -> b.fillType),
			Codec.unboundedMap(MoreCodecs.numberAsString(Integer::parseInt), Codec.either(Codec.STRING, Codec.INT)).fieldOf("statistic_to_level").forGetter(b -> b.statisticToLevel),
			Codec.INT.optionalFieldOf("step_interval", 10).forGetter(b -> b.stepInterval)
	).apply(i, RisingFluidByStatisticBehavior::new));

	private final StatisticKey<Integer> statistic;
	private final FluidFiller.Type fillType;
	private final Map<Integer, Either<String, Integer>> statisticToLevel;
	private final int stepInterval;

	private int fluidLevel;
	private int stepTimeout;
	private FluidFiller filler;

	public RisingFluidByStatisticBehavior(StatisticKey<Integer> statistic, FluidFiller.Type fillType, Map<Integer, Either<String, Integer>> statisticToLevel, int stepInterval) {
		this.statistic = statistic;
		this.fillType = fillType;
		this.statisticToLevel = statisticToLevel;
		this.stepInterval = stepInterval;
		stepTimeout = stepInterval;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		record StatisticThreshold(int value, int level) {
		}

		List<StatisticThreshold> thresholds = statisticToLevel.entrySet().stream()
				.map(entry -> new StatisticThreshold(entry.getKey(), entry.getValue().map(
						key -> game.mapRegions().getOrThrow(key).min().getY(),
						Function.identity()
				)))
				.sorted(Comparator.comparingInt(StatisticThreshold::value))
				.toList();

		int baseLevel = thresholds.getFirst().level();
		fluidLevel = baseLevel;
		filler = new FluidFiller(game.mapRegions().getOrThrow("flood_area"), FluidFiller.Type.WATER, baseLevel);

		events.listen(GamePhaseEvents.TICK, () -> {
			int statisticValue = game.statistics().global().getInt(statistic);
			int targetLevel = baseLevel;
			for (StatisticThreshold threshold : thresholds) {
				if (statisticValue >= threshold.value) {
					targetLevel = threshold.level;
				}
			}

			if (targetLevel > fluidLevel) {
				stepTimeout--;
				if (stepTimeout == 0) {
					stepTimeout = stepInterval;
					fluidLevel++;
				}
			}
			filler.tick(game, fluidLevel);
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.RISING_FLUID_BY_STATISTIC;
	}
}
