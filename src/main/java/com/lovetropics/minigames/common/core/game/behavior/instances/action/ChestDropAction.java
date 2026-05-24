package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.entity.FireworkPalette;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.state.BeaconState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.util.Util;
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

import java.util.ArrayList;
import java.util.List;

public record ChestDropAction(String region, WeightedList<ResourceKey<LootTable>> lootTables, int delay, IntProvider count, float glowRadius) implements IGameBehavior {
	public static final MapCodec<ChestDropAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("region").forGetter(ChestDropAction::region),
			WeightedList.codec(ResourceKey.codec(Registries.LOOT_TABLE)).fieldOf("loot_tables").forGetter(c -> c.lootTables),
			Codec.INT.fieldOf("delay").forGetter(ChestDropAction::delay),
			IntProviders.POSITIVE_CODEC.fieldOf("count").forGetter(ChestDropAction::count),
			Codec.FLOAT.optionalFieldOf("glow_radius", 8.0f).forGetter(ChestDropAction::glowRadius)
	).apply(i, ChestDropAction::new));

	private static final int GLOWING_EFFECT_DURATION = SharedConstants.TICKS_PER_SECOND * 15;

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		List<BlockBox> regions = List.copyOf(game.mapRegions().get(region));
		if (regions.isEmpty()) {
			throw new GameException(Component.literal("No regions with key '" + region + "' to spawn chest drops"));
		}

		ServerLevel level = game.level();
		RandomSource random = level.getRandom();
		BeaconState beacons = game.state().get(BeaconState.KEY);

		List<DelayedDrop> delayedDrops = new ArrayList<>();

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			int count = this.count.sample(random);
			for (int i = 0; i < count; i++) {
				BlockBox region = Util.getRandom(regions, random);
				BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, region.sample(random));

				beacons.add(pos);
				beacons.sendTo(game.allPlayers());
				delayedDrops.add(new DelayedDrop(pos, game.ticks() + delay));
			}

			return true;
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			if (delayedDrops.isEmpty()) {
				return;
			}

			boolean removed = delayedDrops.removeIf(drop -> {
				if (game.ticks() >= drop.placeAtTime()) {
					beacons.remove(drop.pos());
					placeChest(level, random, drop);
					return true;
				} else if (game.ticks() % SharedConstants.TICKS_PER_SECOND == 0) {
					applyGlowingEffectAround(game, drop);
				}
				return false;
			});

			if (removed) {
				beacons.sendTo(game.allPlayers());
			}
		});

		events.listen(GamePlayerEvents.REMOVE, player -> GameClientState.removeFromPlayer(GameClientStateTypes.BEACON.get(), player));
	}

	private void applyGlowingEffectAround(IGamePhase game, DelayedDrop drop) {
		for (ServerPlayer player : game.allPlayers()) {
			if (drop.pos().closerToCenterThan(player.position(), glowRadius)) {
				player.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOWING_EFFECT_DURATION, 1, true, true));
			}
		}
	}

	private void placeChest(ServerLevel level, RandomSource random, DelayedDrop drop) {
		Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		level.setBlock(drop.pos(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, direction), Block.UPDATE_ALL);

		if (level.getBlockEntity(drop.pos()) instanceof ChestBlockEntity chest) {
			lootTables.getRandom(random).ifPresent(lootTable -> chest.setLootTable(lootTable, random.nextLong()));
		}

		FireworkPalette.DYE_COLORS.spawn(drop.pos().above(), level);
	}

	record DelayedDrop(BlockPos pos, long placeAtTime) {
	}
}
