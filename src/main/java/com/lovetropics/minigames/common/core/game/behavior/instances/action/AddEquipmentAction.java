package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record AddEquipmentAction(List<ItemStack> items, ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet, ItemStack offhand, boolean clear, boolean colorByTeam, Map<GameTeamKey, ItemStack> hotbarTeamItems) implements IGameBehavior {
	public static final MapCodec<AddEquipmentAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MoreCodecs.ITEM_STACK.listOf().optionalFieldOf("items", List.of()).forGetter(AddEquipmentAction::items),
			MoreCodecs.ITEM_STACK.optionalFieldOf("head", ItemStack.EMPTY).forGetter(AddEquipmentAction::head),
			MoreCodecs.ITEM_STACK.optionalFieldOf("chest", ItemStack.EMPTY).forGetter(AddEquipmentAction::chest),
			MoreCodecs.ITEM_STACK.optionalFieldOf("legs", ItemStack.EMPTY).forGetter(AddEquipmentAction::legs),
			MoreCodecs.ITEM_STACK.optionalFieldOf("feet", ItemStack.EMPTY).forGetter(AddEquipmentAction::feet),
			MoreCodecs.ITEM_STACK.optionalFieldOf("offhand", ItemStack.EMPTY).forGetter(AddEquipmentAction::offhand),
			Codec.BOOL.optionalFieldOf("clear", false).forGetter(AddEquipmentAction::clear),
			Codec.BOOL.optionalFieldOf("color_by_team", false).forGetter(AddEquipmentAction::colorByTeam),
			Codec.unboundedMap(GameTeamKey.CODEC, MoreCodecs.ITEM_STACK).optionalFieldOf("hotbar_team_items", Map.of()).forGetter(AddEquipmentAction::hotbarTeamItems)
	).apply(i, AddEquipmentAction::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		final TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		events.applyToEntities(game, (context, entity) -> {
			if (!(entity instanceof LivingEntity livingEntity)) {
				return false;
			}

			if (entity instanceof ServerPlayer player) {
				if (clear) {
					player.getInventory().clearContent();
				}
				for (final ItemStack item : items) {
					player.getInventory().add(copyAndModify(player, teams, item));
				}

				if (teams != null) {
					final GameTeamKey teamKey = teams.getTeamForPlayer(player);
					final ItemStack hotbarItem = hotbarTeamItems.get(teamKey);
					if (hotbarItem != null) {
						player.getInventory().add(8, copyAndModify(livingEntity, teams, hotbarItem));
					}
				}
			} else {
				if (clear) {
					for (EquipmentSlot slot : EquipmentSlot.VALUES) {
						livingEntity.setItemSlot(slot, ItemStack.EMPTY);
					}
				}
			}

			if (!head.isEmpty()) {
				livingEntity.setItemSlot(EquipmentSlot.HEAD, copyAndModify(livingEntity, teams, head));
			}
			if (!chest.isEmpty()) {
				livingEntity.setItemSlot(EquipmentSlot.CHEST, copyAndModify(livingEntity, teams, chest));
			}
			if (!legs.isEmpty()) {
				livingEntity.setItemSlot(EquipmentSlot.LEGS, copyAndModify(livingEntity, teams, legs));
			}
			if (!feet.isEmpty()) {
				livingEntity.setItemSlot(EquipmentSlot.FEET, copyAndModify(livingEntity, teams, feet));
			}
			if (!offhand.isEmpty()) {
				addOrReplaceInSlot(livingEntity, EquipmentSlot.OFFHAND, copyAndModify(livingEntity, teams, offhand));
			}

			return true;
		});
	}

	private void addOrReplaceInSlot(LivingEntity entity, EquipmentSlot slot, ItemStack itemStack) {
		ItemStack oldStack = entity.getItemBySlot(slot);
		if (oldStack.isEmpty()) {
			entity.setItemSlot(slot, itemStack);
			return;
		}

		// Try to include items from the old stack if we can merge them
		if (ItemStack.isSameItemSameComponents(oldStack, itemStack)) {
			int maxSize = itemStack.getMaxStackSize();
			int transferCount = Math.min(itemStack.getCount() + oldStack.getCount(), maxSize) - itemStack.getCount();
			itemStack.grow(transferCount);
			oldStack.shrink(transferCount);
		}

		entity.setItemSlot(slot, itemStack);
		if (entity instanceof ServerPlayer player) {
			player.getInventory().add(oldStack);
		}
	}

	private ItemStack copyAndModify(final LivingEntity entity, @Nullable final TeamState teams, final ItemStack item) {
		final ItemStack result = item.copy();
		if (!colorByTeam) {
			return result;
		}
		if (result.is(ItemTags.DYEABLE) && teams != null) {
			final GameTeamKey teamKey = entity instanceof ServerPlayer player ? teams.getTeamForPlayer(player) : null;
			final GameTeam team = teamKey != null ? teams.getTeamByKey(teamKey) : null;
			if (team != null) {
				setColor(team, result);
			}
		}
		return result;
	}

	private static void setColor(final GameTeam team, final ItemStack stack) {
		int color = team.config().dye().getTextureDiffuseColor();
		stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.ADD_EQUIPMENT;
	}
}
