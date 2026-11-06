package org.lovetropics.games.common.core.game.behavior.instances;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.lovetropics.games.SoundRegistry;
import org.lovetropics.games.common.content.qottott.Qottott;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;

import java.util.Optional;

public record LeakyPocketsBehavior(
		Optional<ItemPredicate> itemPredicate,
		int interval,
		int pickUpDelay
) implements IGameBehavior {
	public static final MapCodec<LeakyPocketsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ItemPredicate.CODEC.optionalFieldOf("item_predicate").forGetter(LeakyPocketsBehavior::itemPredicate),
			Codec.INT.optionalFieldOf("interval", 1).forGetter(LeakyPocketsBehavior::interval),
			Codec.INT.optionalFieldOf("pick_up_delay", 40).forGetter(LeakyPocketsBehavior::pickUpDelay)
	).apply(i, LeakyPocketsBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		RandomSource random = game.random();
		events.listen(GamePlayerEvents.TICK, player -> {
			if (player.tickCount % interval != 0) {
				return;
			}
			double chancePerItem = player.getAttributeValue(Qottott.LEAKY_POCKETS);
			if (chancePerItem <= 0.0) {
				return;
			}
			for (ItemStack stack : player.getInventory()) {
				if (stack.isEmpty()) {
					continue;
				}
				if (itemPredicate.isPresent() && !itemPredicate.get().test(stack)) {
					continue;
				}
				int dropAmount = sampleDropCount(stack.getCount(), random, chancePerItem);
				if (dropAmount > 0) {
					ItemStack split = stack.consumeAndReturn(dropAmount, null);
					spawnItems(player, dropAmount, split, pickUpDelay);
				}
			}
		});
	}

	public static void spawnItems(ServerPlayer player, int amount, ItemStack item, int pickUpDelay) {
		RandomSource random = player.getRandom();
		for (int i = 0; i < amount; i++) {
			ItemEntity entity = new ItemEntity(player.level(), player.getRandomX(1.0), player.getRandomY(), player.getRandomZ(1.0), item.copyWithCount(1));
			entity.setPickUpDelay(pickUpDelay);
			entity.setDeltaMovement(random.triangle(0.0, 0.155), random.triangle(0.2, 0.155), random.triangle(0.0, 0.155));
			player.level().addFreshEntity(entity);
		}
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundRegistry.COINS, SoundSource.PLAYERS, 1.0f, random.nextFloat() * 0.4f + 1.3f);
	}

	public static int sampleDropCount(int count, RandomSource random, double chancePerItem) {
		double totalChance = chancePerItem * count;
		int amount = Mth.floor(totalChance);
		if (random.nextFloat() <= totalChance - amount) {
			amount++;
		}
		return Math.min(amount, count);
	}

	@Override
	public MapCodec<LeakyPocketsBehavior> codec() {
		return CODEC;
	}
}
