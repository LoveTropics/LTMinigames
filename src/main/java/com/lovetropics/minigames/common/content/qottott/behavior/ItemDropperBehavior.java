package com.lovetropics.minigames.common.content.qottott.behavior;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContextKeys;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.instances.ConfiguredSound;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.state.BeaconState;
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public record ItemDropperBehavior(Either<List<ItemStackTemplate>, ResourceKey<LootTable>> loot, String regionKey, boolean combined, boolean beacon, IntProvider intervalTicks, Optional<GameActionList> announcement, Optional<ConfiguredSound> sound) implements IGameBehavior {
	public static final MapCodec<ItemDropperBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.either(ExtraCodecs.compactListCodec(ItemStackTemplate.CODEC), ResourceKey.codec(Registries.LOOT_TABLE)).fieldOf("loot").forGetter(ItemDropperBehavior::loot),
			Codec.STRING.fieldOf("region").forGetter(ItemDropperBehavior::regionKey),
			Codec.BOOL.optionalFieldOf("combined", false).forGetter(ItemDropperBehavior::combined),
			Codec.BOOL.optionalFieldOf("beacon", false).forGetter(ItemDropperBehavior::beacon),
			IntProviders.POSITIVE_CODEC.fieldOf("interval_ticks").forGetter(ItemDropperBehavior::intervalTicks),
			GameActionList.CODEC.optionalFieldOf("announcement").forGetter(ItemDropperBehavior::announcement),
			ConfiguredSound.CODEC.optionalFieldOf("sound").forGetter(ItemDropperBehavior::sound)
	).apply(i, ItemDropperBehavior::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		final Collection<BlockBox> regions = game.mapRegions().get(regionKey);
		if (regions.isEmpty()) {
			return;
		}

		announcement.ifPresent(action -> action.register(game, events));

		final Supplier<ItemStack> lootProvider = createLootProvider(game);
		final List<Dropper> droppers;
		if (combined) {
			droppers = List.of(new Dropper(regions.stream().map(BlockBox::center).toList(), lootProvider));
		} else {
			droppers = regions.stream().map(box -> new Dropper(List.of(box.center()), lootProvider)).toList();
		}

		for (final Dropper dropper : droppers) {
			dropper.resetDelay(game);
		}

		final BeaconState beacons = beacon ? game.state().get(BeaconState.KEY) : null;
		events.listen(GamePhaseEvents.TICK, () -> {
			for (final Dropper dropper : droppers) {
				dropper.tick(game, beacons);
			}
		});

		if (beacons != null) {
			events.listen(GamePlayerEvents.REMOVE, player -> GameClientState.removeFromPlayer(GameClientStateTypes.BEACON.get(), player));
		}
	}

	private Supplier<ItemStack> createLootProvider(final IGamePhase game) {
		final RandomSource random = game.random();
		return loot.map(
				stacks -> () -> Util.getRandomSafe(stacks, random).map(ItemStackTemplate::create).orElse(ItemStack.EMPTY),
				tableId -> {
					final LootTable lootTable = game.server().reloadableRegistries().getLootTable(tableId);
					final LootParams params = new LootParams.Builder(game.level()).create(LootContextParamSets.EMPTY);
					return () -> Util.getRandomSafe(lootTable.getRandomItems(params), random).orElse(ItemStack.EMPTY);
				}
		);
	}

	private class Dropper {
		private final List<Vec3> positions;
		private final Supplier<ItemStack> lootProvider;
		private int dropInTicks;

		private @Nullable ItemEntity lastDroppedItem;
		private @Nullable BlockPos beaconPos;

		private Dropper(final List<Vec3> positions, final Supplier<ItemStack> lootProvider) {
			this.positions = positions;
			this.lootProvider = lootProvider;
		}

		public void tick(final IGamePhase game, @Nullable final BeaconState beacons) {
			checkDroppedItem(game, beacons);
			if (lastDroppedItem != null) {
				return;
			}

			if (dropInTicks == 0) {
				final Vec3 position = Util.getRandom(positions, game.random());
				resetDelay(game);
				final ItemStack item = lootProvider.get();
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

		private void checkDroppedItem(final IGamePhase game, @Nullable final BeaconState beacons) {
			if (lastDroppedItem == null || lastDroppedItem.isAlive()) {
				return;
			}
			if (beacons != null && beaconPos != null) {
				removeBeacon(game, beacons);
			}
			lastDroppedItem = null;
		}

		private void addBeacon(final IGamePhase game, final BeaconState beacons, final Vec3 position) {
			beaconPos = BlockPos.containing(position);
			beacons.add(beaconPos);
			beacons.sendTo(game.allPlayers());
		}

		private void removeBeacon(final IGamePhase game, final BeaconState beacons) {
			beacons.remove(beaconPos);
			beacons.sendTo(game.allPlayers());
		}

		private void resetDelay(final IGamePhase game) {
			dropInTicks = intervalTicks.sample(game.random());
		}

		private void spawnItem(final IGamePhase game, final ItemStack item, final Vec3 position) {
			final ServerLevel level = game.level();
			final ItemEntity itemEntity = new ItemEntity(level, position.x, position.y, position.z, item, 0.0, 0.1, 0.0);
			level.addFreshEntity(itemEntity);
			lastDroppedItem = itemEntity;

			announcement.ifPresent(action -> action.apply(game, new ContextMap.Builder().withParameter(GameActionContextKeys.ITEM, item).create(ContextKeySet.EMPTY)));
			sound.ifPresent(sound -> level.playSound(null, position.x, position.y, position.z, sound.sound(), sound.source(), sound.volume(), sound.pitch()));
		}
	}
}
