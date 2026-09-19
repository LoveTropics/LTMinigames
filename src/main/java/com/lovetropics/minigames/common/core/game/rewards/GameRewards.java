package com.lovetropics.minigames.common.core.game.rewards;

import com.lovetropics.minigames.common.content.MinigameTexts;
import com.mojang.serialization.DynamicOps;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class GameRewards {
	private final List<ItemStack> stacks = new ArrayList<>();
	private final Set<Identifier> collectibleIds = new HashSet<>();
	private final Set<ItemStack> collectibleStacks = ItemStackLinkedSet.createTypeAndComponentsSet();

	public void give(ItemStack item) {
		ItemStack remainder = tryMergeIntoExistingStack(item.copy());
		if (!remainder.isEmpty()) {
			stacks.add(remainder);
		}
	}

	public void giveCollectible(Identifier id) {
		collectibleIds.add(id);
	}

	public void giveCollectible(ItemStack item) {
		if (!item.isEmpty()) {
			collectibleStacks.add(item.copyWithCount(1));
		}
	}

	private ItemStack tryMergeIntoExistingStack(ItemStack item) {
		for (ItemStack stack : stacks) {
			if (!ItemStack.isSameItemSameComponents(item, stack)) {
				continue;
			}
			int maxAmount = stack.getMaxStackSize() - stack.getCount();
			int amount = Math.min(item.getCount(), maxAmount);
			if (amount > 0) {
				stack.grow(amount);
				item.shrink(amount);
				if (item.isEmpty()) {
					return ItemStack.EMPTY;
				}
			}
		}
		return item;
	}

	public void grant(ServerPlayer player) {
		if (stacks.isEmpty() && collectibleIds.isEmpty() && collectibleStacks.isEmpty()) {
			return;
		}
		player.sendSystemMessage(MinigameTexts.REWARDS);
		for (ItemStack item : stacks) {
			player.sendSystemMessage(MinigameTexts.REWARD_ITEM.apply(
					Component.literal(String.valueOf(item.getCount())),
					item.getDisplayName().copy().withStyle(ChatFormatting.AQUA)
			));
			player.getInventory().placeItemBackInInventory(item);
		}
		// TODO: Can we have a proper interface, so we can actually display that correctly?
		int collectibleCount = collectibleIds.size() + collectibleStacks.size();
		if (collectibleCount > 0) {
			player.sendSystemMessage(MinigameTexts.REWARD_ITEM.apply(
					Component.literal(String.valueOf(collectibleCount)),
					Component.literal("Collectibles").withStyle(ChatFormatting.AQUA)
			));
		}
		for (Identifier id : collectibleIds) {
			grantCollectible(player, id);
		}
		for (ItemStack item : collectibleStacks) {
			grantCollectible(player, item);
		}
	}

	private static void grantCollectible(ServerPlayer player, ItemStack item) {
		// TODO: We should probably be splitting collectibles into their own mod at this point - and have an API for this. Commands are not a good API!
		grantCollectible(player, serializeItem(item, player.registryAccess()));
	}

	private static void grantCollectible(ServerPlayer player, Identifier id) {
		grantCollectible(player, id.toString());
	}

	private static void grantCollectible(ServerPlayer player, String collectibleString) {
		CommandSourceStack source = player.level().getServer().createCommandSourceStack();
		String commandBuilder = "collectible give " + player.nameAndId().name() + " " + collectibleString;
		player.level().getServer().getCommands().performPrefixedCommand(source, commandBuilder);
	}

	private static String serializeItem(ItemStack item, RegistryAccess registryAccess) {
		StringBuilder output = new StringBuilder();
		output.append(item.typeHolder().getRegisteredName());

		DataComponentPatch components = item.getComponentsPatch();
		if (components.isEmpty()) {
			return output.toString();
		}

		RegistryOps<Tag> ops = registryAccess.createSerializationContext(NbtOps.INSTANCE);
		output.append('[');
		for (Map.Entry<DataComponentType<?>, Optional<?>> entry : components.entrySet()) {
			DataComponentType<?> component = entry.getKey();
			if (component.codec() == null) {
				continue;
			}
			Identifier componentId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component);
			if (entry.getValue().isPresent()) {
				output.append(componentId).append('=');
				output.append(serializeComponentUnchecked(ops, component, entry.getValue().get()));
			} else {
				output.append('!').append(componentId);
			}
		}
		output.append(']');

		return output.toString();
	}

	@SuppressWarnings("unchecked")
	private static <T> Tag serializeComponentUnchecked(DynamicOps<Tag> ops, DataComponentType<T> type, Object value) {
		return type.codecOrThrow().encodeStart(ops, (T) value).getOrThrow();
	}
}
