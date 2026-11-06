package org.lovetropics.games.common.content.qottott.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStackTemplate;
import org.lovetropics.games.common.content.qottott.Qottott;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.instances.LeakyPocketsBehavior;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticsMap;

public record QottottLeakyPocketsBehavior(ItemStackTemplate item, StatisticKey<Integer> statistic, int interval) implements IGameBehavior {
	public static final MapCodec<QottottLeakyPocketsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ItemStackTemplate.CODEC.fieldOf("item").forGetter(QottottLeakyPocketsBehavior::item),
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(QottottLeakyPocketsBehavior::statistic),
			Codec.INT.optionalFieldOf("interval", 1).forGetter(QottottLeakyPocketsBehavior::interval)
	).apply(i, QottottLeakyPocketsBehavior::new));

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
			int dropAmount = LeakyPocketsBehavior.sampleDropCount(count, random, chancePerCoin);
			if (dropAmount > 0) {
				statistics.incrementInt(statistic, -dropAmount);
				LeakyPocketsBehavior.spawnItems(player, dropAmount, item.create(), 0);
			}
		});
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
