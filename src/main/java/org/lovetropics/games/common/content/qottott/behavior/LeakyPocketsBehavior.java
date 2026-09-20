package org.lovetropics.games.common.content.qottott.behavior;

import org.lovetropics.games.common.content.qottott.Qottott;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticsMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.function.Supplier;

public record LeakyPocketsBehavior(ItemStackTemplate item, StatisticKey<Integer> statistic, int interval) implements IGameBehavior {
	public static final MapCodec<LeakyPocketsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ItemStackTemplate.CODEC.fieldOf("item").forGetter(LeakyPocketsBehavior::item),
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(LeakyPocketsBehavior::statistic),
			Codec.INT.optionalFieldOf("interval", 1).forGetter(LeakyPocketsBehavior::interval)
	).apply(i, LeakyPocketsBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		RandomSource random = game.random();
		events.listen(GamePlayerEvents.TICK, player -> {
			if (player.tickCount % interval != 0) {
				return;
			}
			double chancePerCoin = player.getAttributeValue(Qottott.LEAKY_POCKETS);
			if (chancePerCoin <= 0.0) {
				return;
			}
			StatisticsMap statistics = game.statistics().forPlayer(player);
			int count = statistics.getInt(statistic);
			int dropAmount = sampleDropCount(count, random, chancePerCoin);
			if (dropAmount > 0) {
				statistics.incrementInt(statistic, -dropAmount);
				CoinDropAttributeBehavior.spawnItems(player, dropAmount, item.create());
			}
		});
	}

	private int sampleDropCount(int count, RandomSource random, double chancePerCoin) {
		double totalChance = chancePerCoin * count;
		int amount = Mth.floor(totalChance);
		if (random.nextFloat() <= totalChance - amount) {
			amount++;
		}
		return Math.min(amount, count);
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return Qottott.LEAKY_POCKETS_BEHAVIOR;
	}
}
