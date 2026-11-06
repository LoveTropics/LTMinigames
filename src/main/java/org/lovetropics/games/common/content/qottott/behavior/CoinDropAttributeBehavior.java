package org.lovetropics.games.common.content.qottott.behavior;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStackTemplate;
import org.lovetropics.games.common.content.qottott.Qottott;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.instances.LeakyPocketsBehavior;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticsMap;

public record CoinDropAttributeBehavior(ItemStackTemplate item, StatisticKey<Integer> statistic) implements IGameBehavior {
	public static final MapCodec<CoinDropAttributeBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ItemStackTemplate.CODEC.fieldOf("item").forGetter(CoinDropAttributeBehavior::item),
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(CoinDropAttributeBehavior::statistic)
	).apply(i, CoinDropAttributeBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.DEATH, (player, damageSource) -> {
			if (damageSource.getEntity() instanceof LivingEntity killer && killer.getAttributes().hasAttribute(Qottott.COIN_DROPS)) {
				double coinDrops = killer.getAttributeValue(Qottott.COIN_DROPS);
				if (coinDrops > 0.0) {
					StatisticsMap statistics = game.statistics().forPlayer(player);
					int amount = Mth.floor(statistics.getInt(statistic) * coinDrops);
					if (amount > 0) {
						statistics.incrementInt(statistic, -amount);
						LeakyPocketsBehavior.spawnItems(player, amount, item.create(), 0);
					}
				}
			}
			return TriState.DEFAULT;
		});
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
