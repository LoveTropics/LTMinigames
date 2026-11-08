package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.DyedItemColor;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record AddEquipmentAction(
		List<ItemStackTemplate> items,
		Optional<ItemStackTemplate> head,
		Optional<ItemStackTemplate> chest,
		Optional<ItemStackTemplate> legs,
		Optional<ItemStackTemplate> feet,
		Optional<ItemStackTemplate> mainhand,
		Optional<ItemStackTemplate> offhand,
		boolean clear,
		boolean colorByTeam,
		Map<GameTeamKey, ItemStackTemplate> hotbarTeamItems,
		boolean allowDuplicates,
		boolean keepReplaced
) implements IGameBehavior {
	public static final MapCodec<AddEquipmentAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ItemStackTemplate.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(AddEquipmentAction::items),
			ItemStackTemplate.CODEC.optionalFieldOf("head").forGetter(AddEquipmentAction::head),
			ItemStackTemplate.CODEC.optionalFieldOf("chest").forGetter(AddEquipmentAction::chest),
			ItemStackTemplate.CODEC.optionalFieldOf("legs").forGetter(AddEquipmentAction::legs),
			ItemStackTemplate.CODEC.optionalFieldOf("feet").forGetter(AddEquipmentAction::feet),
			ItemStackTemplate.CODEC.optionalFieldOf("mainhand").forGetter(AddEquipmentAction::mainhand),
			ItemStackTemplate.CODEC.optionalFieldOf("offhand").forGetter(AddEquipmentAction::offhand),
			Codec.BOOL.optionalFieldOf("clear", false).forGetter(AddEquipmentAction::clear),
			Codec.BOOL.optionalFieldOf("color_by_team", false).forGetter(AddEquipmentAction::colorByTeam),
			Codec.unboundedMap(GameTeamKey.CODEC, ItemStackTemplate.CODEC).optionalFieldOf("hotbar_team_items", Map.of()).forGetter(AddEquipmentAction::hotbarTeamItems),
			Codec.BOOL.optionalFieldOf("allow_duplicates", true).forGetter(AddEquipmentAction::allowDuplicates),
			Codec.BOOL.optionalFieldOf("keep_replaced", false).forGetter(AddEquipmentAction::keepReplaced)
	).apply(i, AddEquipmentAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		events.applyToEntities(game, (context, level, entity) -> {
			if (!(entity instanceof LivingEntity livingEntity)) {
				return false;
			}

			if (entity instanceof ServerPlayer player) {
				Inventory inventory = player.getInventory();
				if (clear) {
					inventory.clearContent();
				}
				for (ItemStackTemplate item : items) {
					ItemStack stack = copyAndModify(player, teams, item.create());
					if (allowDuplicates || !inventory.contains(stack)) {
						inventory.add(stack);
					}
				}

				if (teams != null) {
					GameTeamKey teamKey = teams.getTeamForPlayer(player);
					if (teamKey != null) {
						ItemStackTemplate hotbarItem = hotbarTeamItems.get(teamKey);
						if (hotbarItem != null) {
							ItemStack stack = copyAndModify(livingEntity, teams, hotbarItem.create());
							if (allowDuplicates || !ItemStack.isSameItemSameComponents(inventory.getItem(8), stack)) {
								inventory.add(8, stack);
							}
						}
					}
				}
			} else {
				if (clear) {
					for (EquipmentSlot slot : EquipmentSlot.VALUES) {
						livingEntity.setItemSlot(slot, ItemStack.EMPTY);
					}
				}
			}

			head.ifPresent(stack -> setArmorSlot(livingEntity, EquipmentSlot.HEAD, copyAndModify(livingEntity, teams, stack.create())));
			chest.ifPresent(stack -> setArmorSlot(livingEntity, EquipmentSlot.CHEST, copyAndModify(livingEntity, teams, stack.create())));
			legs.ifPresent(stack -> setArmorSlot(livingEntity, EquipmentSlot.LEGS, copyAndModify(livingEntity, teams, stack.create())));
			feet.ifPresent(stack -> setArmorSlot(livingEntity, EquipmentSlot.FEET, copyAndModify(livingEntity, teams, stack.create())));
			mainhand.ifPresent(stack -> addOrReplaceInSlot(livingEntity, EquipmentSlot.MAINHAND, copyAndModify(livingEntity, teams, stack.create())));
			offhand.ifPresent(stack -> addOrReplaceInSlot(livingEntity, EquipmentSlot.OFFHAND, copyAndModify(livingEntity, teams, stack.create())));

			return true;
		});
	}

	private void setArmorSlot(LivingEntity entity, EquipmentSlot slot, ItemStack itemStack) {
		ItemStack oldStack = entity.getItemBySlot(slot);
		entity.setItemSlot(slot, itemStack);
		if (keepReplaced && !oldStack.isEmpty() && entity instanceof ServerPlayer player) {
			player.getInventory().placeItemBackInInventory(oldStack);
		}
	}

	private void addOrReplaceInSlot(LivingEntity entity, EquipmentSlot slot, ItemStack itemStack) {
		if (entity instanceof ServerPlayer player && slot == EquipmentSlot.MAINHAND) {
			Inventory inventory = player.getInventory();
			int selectedSlot = inventory.getSelectedSlot();
			ItemStack oldStack = inventory.getItem(selectedSlot);
			if (oldStack.isEmpty()) {
				inventory.setItem(selectedSlot, itemStack);
				return;
			}
			tryTransfer(oldStack, itemStack);
			inventory.setItem(selectedSlot, itemStack);
			inventory.add(oldStack);
			return;
		}

		ItemStack oldStack = entity.getItemBySlot(slot);
		if (oldStack.isEmpty()) {
			entity.setItemSlot(slot, itemStack);
			return;
		}

		// Try to include items from the old stack if we can merge them
		tryTransfer(oldStack, itemStack);

		entity.setItemSlot(slot, itemStack);
		if (entity instanceof ServerPlayer player) {
			player.getInventory().add(oldStack);
		}
	}

	private void tryTransfer(ItemStack fromStack, ItemStack toStack) {
		// Try to include items from the old stack if we can merge them
		if (ItemStack.isSameItemSameComponents(fromStack, toStack)) {
			int maxSize = toStack.getMaxStackSize();
			int transferCount = Math.min(toStack.getCount() + fromStack.getCount(), maxSize) - toStack.getCount();
			toStack.grow(transferCount);
			fromStack.shrink(transferCount);
		}
	}

	private ItemStack copyAndModify(LivingEntity entity, @Nullable TeamState teams, ItemStack item) {
		ItemStack result = item.copy();
		if (!colorByTeam) {
			return result;
		}
		if (result.has(DataComponents.DYED_COLOR) && teams != null) {
			GameTeamKey teamKey = entity instanceof ServerPlayer player ? teams.getTeamForPlayer(player) : null;
			GameTeam team = teamKey != null ? teams.getTeamByKey(teamKey) : null;
			if (team != null) {
				setColor(team, result);
			}
		}
		return result;
	}

	private static void setColor(GameTeam team, ItemStack stack) {
		int color = team.dyeColor().getTextureDiffuseColor();
		stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
