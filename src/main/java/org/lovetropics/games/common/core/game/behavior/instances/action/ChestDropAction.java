package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.lovetropics.lib.entity.FireworkPalette;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.region.LevelBlockPos;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.game.state.BeaconState;

import java.util.ArrayList;
import java.util.List;

public record ChestDropAction(RegionQuery region, WeightedList<ResourceKey<LootTable>> lootTables, int delay, IntProvider count, float glowRadius) implements IGameBehavior {
	public static final MapCodec<ChestDropAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("region").forGetter(ChestDropAction::region),
			WeightedList.codec(ResourceKey.codec(Registries.LOOT_TABLE)).fieldOf("loot_tables").forGetter(c -> c.lootTables),
			Codec.INT.fieldOf("delay").forGetter(ChestDropAction::delay),
			IntProviders.POSITIVE_CODEC.fieldOf("count").forGetter(ChestDropAction::count),
			Codec.FLOAT.optionalFieldOf("glow_radius", 8.0f).forGetter(ChestDropAction::glowRadius)
	).apply(i, ChestDropAction::new));

	private static final int GLOWING_EFFECT_DURATION = SharedConstants.TICKS_PER_SECOND * 15;

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		List<Region> regions = List.copyOf(region.all(game));
		if (regions.isEmpty()) {
			throw new GameException(Component.literal("No regions with key '" + region + "' to spawn chest drops"));
		}

		RandomSource random = game.random();
		BeaconState beacons = BeaconState.getOrAdd(game, events);

		List<DelayedDrop> delayedDrops = new ArrayList<>();

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			int count = this.count.sample(random);
			for (int i = 0; i < count; i++) {
				LevelBlockPos randomPos = region.pickRandomPos(game, game.random());
				if (randomPos == null) {
					continue;
				}
				ServerLevel level = randomPos.level();
				BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, randomPos.pos());

				beacons.add(level.dimension(), surfacePos);
				delayedDrops.add(new DelayedDrop(level, surfacePos, game.ticks() + delay));
			}

			return true;
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			if (delayedDrops.isEmpty()) {
				return;
			}

			delayedDrops.removeIf(drop -> {
				if (game.ticks() >= drop.placeAtTime()) {
					beacons.remove(drop.level().dimension(), drop.pos());
					placeChest(random, drop);
					return true;
				} else if (game.ticks() % SharedConstants.TICKS_PER_SECOND == 0) {
					applyGlowingEffectAround(game, drop);
				}
				return false;
			});
		});
	}

	private void applyGlowingEffectAround(IGamePhase game, DelayedDrop drop) {
		for (ServerPlayer player : game.allPlayers()) {
			if (drop.pos().closerToCenterThan(player.position(), glowRadius)) {
				player.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOWING_EFFECT_DURATION, 1, true, true));
			}
		}
	}

	private void placeChest(RandomSource random, DelayedDrop drop) {
		ServerLevel level = drop.level;
		Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		level.setBlock(drop.pos(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, direction), Block.UPDATE_ALL);

		if (level.getBlockEntity(drop.pos()) instanceof ChestBlockEntity chest) {
			lootTables.getRandom(random).ifPresent(lootTable -> chest.setLootTable(lootTable, random.nextLong()));
		}

		FireworkPalette.DYE_COLORS.spawn(drop.pos().above(), level);
	}

	record DelayedDrop(ServerLevel level, BlockPos pos, long placeAtTime) {
	}
}
