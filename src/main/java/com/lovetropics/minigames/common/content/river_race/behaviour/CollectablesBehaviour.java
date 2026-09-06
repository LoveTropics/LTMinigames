package com.lovetropics.minigames.common.content.river_race.behaviour;

import com.lovetropics.lib.entity.FireworkPalette;
import com.lovetropics.minigames.common.content.river_race.RiverRace;
import com.lovetropics.minigames.common.content.river_race.RiverRaceState;
import com.lovetropics.minigames.common.content.river_race.RiverRaceTexts;
import com.lovetropics.minigames.common.content.river_race.event.RiverRaceEvents;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContextKeys;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.TriState;
import net.minecraft.util.Unit;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CollectablesBehaviour implements IGameBehavior {
	public static final MapCodec<CollectablesBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(Codec.STRING, CollectableConfig.CODEC).fieldOf("collectables_by_zone").forGetter(b -> b.collectablesByZone),
			DataComponentPatch.CODEC.optionalFieldOf("item_patch", DataComponentPatch.EMPTY).forGetter(b -> b.itemPatch)
	).apply(i, CollectablesBehaviour::new));

	private final Map<String, CollectableConfig> collectablesByZone;
	private final DataComponentPatch itemPatch;

	public CollectablesBehaviour(Map<String, CollectableConfig> collectablesByZone, DataComponentPatch itemPatch) {
		this.collectablesByZone = collectablesByZone;
		this.itemPatch = itemPatch;
	}

	private final Map<BlockPos, RiverRaceState.Zone> monumentSlots = new HashMap<>();
	private final LongSet placedCollectables = new LongOpenHashSet();

	private final Map<String, GameTeamKey> firstTeamToCollect = new HashMap<>();

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		RiverRaceState riverRace = game.state().get(RiverRaceState.KEY);

		collectablesByZone.forEach((zoneId, collectable) -> {
			RiverRaceState.Zone zone = riverRace.getZoneById(zoneId);
			collectable.onCompleteAction.register(game, events);
			for (String region : collectable.monumentSlotRegions) {
				for (BlockPos pos : game.mapRegions().getOrThrow(region)) {
					monumentSlots.put(pos.immutable(), zone);
				}
			}
			zone.setCollectable(createItem(zone, collectable));
		});

		events.listen(GamePhaseEvents.CREATE, () -> monumentSlots.forEach((pos, zone) -> {
			if (zone.collectable() != null) {
				spawnCollectableDisplay(game, zone.collectable(), Vec3.atCenterOf(pos));
			}
		}));
		events.listen(GamePlayerEvents.PLACE_BLOCK, (player, pos, placed, placedOn, placedItemStack) -> {
			RiverRaceState.Zone expectedCollectable = monumentSlots.get(pos);
			RiverRaceState.Zone placedCollectable = riverRace.getZoneWithCollectable(placedItemStack);
			if (expectedCollectable != null || placedCollectable != null) {
				return tryPlaceCollectable(game, teams, player, pos, expectedCollectable, placedCollectable);
			}
			return TriState.DEFAULT;
		});
		events.listen(GamePlayerEvents.BREAK_BLOCK, (player, pos, state, hand) -> {
			if (placedCollectables.contains(pos.asLong())) {
				return TriState.FALSE;
			}
			return TriState.DEFAULT;
		});

		events.listen(GamePlayerEvents.INVENTORY_CHANGED, (player, container, slotIndex, newItemStack) -> {
			Slot slot = container.getSlot(slotIndex);
			if (slot.container != player.getInventory() && newItemStack.has(RiverRace.COLLECTABLE_MARKER)) {
				ItemStack slotItem = slot.getItem();
				player.getInventory().add(slotItem.copy());
				slotItem.setCount(0);
			}
		});
	}

	private void spawnCollectableDisplay(IGamePhase game, ItemStack collectable, Vec3 position) {
		Display.ItemDisplay itemDisplay = EntityTypes.ITEM_DISPLAY.create(game.level(), EntitySpawnReason.COMMAND);
		itemDisplay.setPos(position);
		itemDisplay.setItemStack(collectable.copy());
		itemDisplay.setTransformation(new Transformation(null, null, new Vector3f(0.2f, 0.2f, 0.2f), null));
		game.level().addFreshEntity(itemDisplay);
	}

	private TriState tryPlaceCollectable(IGamePhase game, TeamState teams, ServerPlayer player, BlockPos pos, RiverRaceState.@Nullable Zone expectedCollectable, RiverRaceState.@Nullable Zone placedCollectable) {
		if (placedCollectable == null || !Objects.equals(expectedCollectable, placedCollectable)) {
			com.lovetropics.minigames.common.util.Util.sendNotifySound(player, SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
			player.sendSystemMessage(RiverRaceTexts.CANT_PLACE_COLLECTABLE, true);
			return TriState.FALSE;
		}

		GameTeamKey teamKey = teams.getTeamForPlayer(player);
		GameTeam team = teamKey != null ? teams.getTeamByKey(teamKey) : null;
		if (team == null) {
			return TriState.FALSE;
		}

		// If the players somehow broke the old one - don't let them continue to trigger it and gain points
		if (!placedCollectables.add(pos.asLong())) {
			return TriState.FALSE;
		}

		return onCollectablePlaced(game, player, team, placedCollectable, pos);
	}

	private TriState onCollectablePlaced(IGamePhase game, ServerPlayer player, GameTeam team, RiverRaceState.Zone collectableZone, BlockPos slotPos) {
		if (firstTeamToCollect.putIfAbsent(collectableZone.id(), team.key()) == null) {
			ContextMap context = new ContextMap.Builder()
					.withParameter(GameActionContextKeys.TEAM, team)
					.withParameter(GameActionContextKeys.NAME, collectableZone.displayName())
					.create(ContextKeySet.EMPTY);
			CollectableConfig config = collectablesByZone.get(collectableZone.id());
			if (config != null) {
				config.onCompleteAction.apply(game, context, ActionSubjects.ofPlayer(player));
			}
		}
		game.invoker(RiverRaceEvents.COLLECTABLE_PLACED).onCollectablePlaced(player, team, slotPos);
		FireworkPalette.DYE_COLORS.spawn(slotPos.above(), game.level());
		return TriState.DEFAULT;
	}

	private ItemStack createItem(RiverRaceState.Zone zone, CollectableConfig collectable) {
		ItemStack item = collectable.baseItem.create();
		item.set(DataComponents.ITEM_NAME, RiverRaceTexts.COLLECTABLE_NAME.apply(zone.displayName()));
		item.set(RiverRace.COLLECTABLE_MARKER.get(), Unit.INSTANCE);
		item.applyComponents(itemPatch);
		return item;
	}

	public record CollectableConfig(
			ItemStackTemplate baseItem,
			List<String> monumentSlotRegions,
			GameActionList onCompleteAction
	) {
		public static final Codec<CollectableConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
				ItemStackTemplate.CODEC.fieldOf("item").forGetter(CollectableConfig::baseItem),
				ExtraCodecs.nonEmptyList(Codec.STRING.listOf()).fieldOf("monument_slot_region").forGetter(CollectableConfig::monumentSlotRegions),
				GameActionList.CODEC.fieldOf("on_complete").forGetter(CollectableConfig::onCompleteAction)
		).apply(i, CollectableConfig::new));
	}
}
