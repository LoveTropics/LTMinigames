package org.lovetropics.games.common.content.qottott.behavior;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.instances.ConfiguredSound;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.game.state.BeaconState;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public record ItemDropperBehavior(Either<List<ItemStackTemplate>, ResourceKey<LootTable>> loot, RegionQuery region, boolean combined, boolean beacon, IntProvider intervalTicks, Optional<GameActionList> announcement, Optional<ConfiguredSound> sound) implements IGameBehavior {
	public static final MapCodec<ItemDropperBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.either(ExtraCodecs.compactListCodec(ItemStackTemplate.CODEC), ResourceKey.codec(Registries.LOOT_TABLE)).fieldOf("loot").forGetter(ItemDropperBehavior::loot),
			RegionQuery.CODEC.fieldOf("region").forGetter(ItemDropperBehavior::region),
			Codec.BOOL.optionalFieldOf("combined", false).forGetter(ItemDropperBehavior::combined),
			Codec.BOOL.optionalFieldOf("beacon", false).forGetter(ItemDropperBehavior::beacon),
			IntProviders.POSITIVE_CODEC.fieldOf("interval_ticks").forGetter(ItemDropperBehavior::intervalTicks),
			GameActionList.CODEC.optionalFieldOf("announcement").forGetter(ItemDropperBehavior::announcement),
			ConfiguredSound.CODEC.optionalFieldOf("sound").forGetter(ItemDropperBehavior::sound)
	).apply(i, ItemDropperBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		announcement.ifPresent(action -> action.register(game, events));
		for (ServerLevel level : game.allLevels()) {
			registerForLevel(game, events, level);
		}
	}

	private void registerForLevel(IGamePhase game, EventRegistrar events, ServerLevel level) {
		List<Region> regions = region.allIn(game, level);
		if (regions.isEmpty()) {
			return;
		}

		Supplier<ItemStack> lootProvider = createLootProvider(game, level);
		List<Dropper> droppers;
		if (combined) {
			droppers = List.of(new Dropper(level, regions.stream().map(Region::center).toList(), lootProvider));
		} else {
			droppers = regions.stream().map(box -> new Dropper(level, List.of(box.center()), lootProvider)).toList();
		}

		for (Dropper dropper : droppers) {
			dropper.resetDelay(game);
		}

		BeaconState beacons = beacon ? BeaconState.getOrAdd(game, events) : null;
		events.listen(GamePhaseEvents.TICK, () -> {
			for (Dropper dropper : droppers) {
				dropper.tick(game, beacons);
			}
		});
	}

	private Supplier<ItemStack> createLootProvider(IGamePhase game, ServerLevel level) {
		RandomSource random = game.random();
		return loot.map(
				stacks -> () -> Util.getRandomSafe(stacks, random).map(ItemStackTemplate::create).orElse(ItemStack.EMPTY),
				tableId -> {
					LootTable lootTable = game.server().reloadableRegistries().getLootTable(tableId);
					LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
					return () -> Util.getRandomSafe(lootTable.getRandomItems(params), random).orElse(ItemStack.EMPTY);
				}
		);
	}

	private class Dropper {
		private final ServerLevel level;
		private final List<Vec3> positions;
		private final Supplier<ItemStack> lootProvider;
		private int dropInTicks;

		private @Nullable ItemEntity lastDroppedItem;
		private @Nullable BlockPos beaconPos;

		private Dropper(ServerLevel level, List<Vec3> positions, Supplier<ItemStack> lootProvider) {
			this.level = level;
			this.positions = positions;
			this.lootProvider = lootProvider;
		}

		public void tick(IGamePhase game, @Nullable BeaconState beacons) {
			checkDroppedItem(game, beacons);
			if (lastDroppedItem != null) {
				return;
			}

			if (dropInTicks == 0) {
				Vec3 position = Util.getRandom(positions, game.random());
				resetDelay(game);
				ItemStack item = lootProvider.get();
				if (!item.isEmpty()) {
					spawnItem(game, item, position);
					if (beacons != null) {
						addBeacon(game, beacons, position);
					}
				}
			} else {
				dropInTicks--;
			}
		}

		private void checkDroppedItem(IGamePhase game, @Nullable BeaconState beacons) {
			if (lastDroppedItem == null || lastDroppedItem.isAlive()) {
				return;
			}
			if (beacons != null && beaconPos != null) {
				removeBeacon(game, beacons);
			}
			lastDroppedItem = null;
		}

		private void addBeacon(IGamePhase game, BeaconState beacons, Vec3 position) {
			beaconPos = BlockPos.containing(position);
			beacons.add(level.dimension(), beaconPos);
		}

		private void removeBeacon(IGamePhase game, BeaconState beacons) {
			beacons.remove(level.dimension(), beaconPos);
		}

		private void resetDelay(IGamePhase game) {
			dropInTicks = intervalTicks.sample(game.random());
		}

		private void spawnItem(IGamePhase game, ItemStack item, Vec3 position) {
			ItemEntity itemEntity = new ItemEntity(level, position.x, position.y, position.z, item, 0.0, 0.1, 0.0);
			level.addFreshEntity(itemEntity);
			lastDroppedItem = itemEntity;

			announcement.ifPresent(action -> action.apply(game, new ContextMap.Builder().withParameter(GameActionContextKeys.ITEM, item).create(ContextKeySet.EMPTY)));
			sound.ifPresent(sound -> level.playSound(null, position.x, position.y, position.z, sound.sound(), sound.source(), sound.volume(), sound.pitch()));
		}
	}
}
