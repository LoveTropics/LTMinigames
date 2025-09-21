package com.lovetropics.minigames.common.content.qottott.behavior;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.SoundRegistry;
import com.lovetropics.minigames.common.content.qottott.Qottott;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticsMap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public record CoinDropAttributeBehavior(ItemStack item, StatisticKey<Integer> statistic) implements IGameBehavior {
	public static final MapCodec<CoinDropAttributeBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MoreCodecs.ITEM_STACK.fieldOf("item").forGetter(CoinDropAttributeBehavior::item),
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(CoinDropAttributeBehavior::statistic)
	).apply(i, CoinDropAttributeBehavior::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		events.listen(GamePlayerEvents.DEATH, (player, damageSource) -> {
			if (damageSource.getEntity() instanceof final LivingEntity killer && killer.getAttributes().hasAttribute(Qottott.COIN_DROPS)) {
				final double coinDrops = killer.getAttributeValue(Qottott.COIN_DROPS);
				if (coinDrops > 0.0) {
					final StatisticsMap statistics = game.statistics().forPlayer(player);
					final int amount = Mth.floor(statistics.getInt(statistic) * coinDrops);
					if (amount > 0) {
						statistics.incrementInt(statistic, -amount);
						spawnItems(game, player, amount, item);
					}
				}
			}
			return TriState.DEFAULT;
		});
	}

	public static void spawnItems(final IGamePhase game, final Player player, final int amount, final ItemStack item) {
		final ServerLevel level = game.level();
		final RandomSource random = level.random;
		for (int i = 0; i < amount; i++) {
			final ItemEntity entity = new ItemEntity(level, player.getRandomX(1.0), player.getRandomY(), player.getRandomZ(1.0), item.copyWithCount(1));
			entity.setDeltaMovement(random.triangle(0.0, 0.155), random.triangle(0.2, 0.155), random.triangle(0.0, 0.155));
			level.addFreshEntity(entity);
		}
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundRegistry.COINS, SoundSource.PLAYERS, 1.0f, random.nextFloat() * 0.4f + 1.3f);
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return Qottott.COIN_DROP_ATTRIBUTE_BEHAVIOR;
	}
}
