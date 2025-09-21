package com.lovetropics.minigames.common.core.game.rewards;

import com.lovetropics.minigames.common.content.MinigameTexts;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GameRewards {
	private final List<ItemStack> stacks = new ArrayList<>();
	private final Set<ItemStack> collectibles = ItemStackLinkedSet.createTypeAndComponentsSet();

	public void give(final ItemStack item) {
		final ItemStack remainder = tryMergeIntoExistingStack(item.copy());
		if (!remainder.isEmpty()) {
			stacks.add(remainder);
		}
	}

	public void giveCollectible(final ItemStack item) {
		if (!item.isEmpty()) {
			collectibles.add(item.copyWithCount(1));
		}
	}

	private ItemStack tryMergeIntoExistingStack(final ItemStack item) {
		for (final ItemStack stack : stacks) {
			if (!ItemStack.isSameItemSameComponents(item, stack)) {
				continue;
			}
			final int maxAmount = stack.getMaxStackSize() - stack.getCount();
			final int amount = Math.min(item.getCount(), maxAmount);
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

	public void grant(final ServerPlayer player) {
		if (stacks.isEmpty()) {
			return;
		}
		player.sendSystemMessage(MinigameTexts.REWARDS);
		for (final ItemStack item : stacks) {
			player.sendSystemMessage(MinigameTexts.REWARD_ITEM.apply(
					Component.literal(String.valueOf(item.getCount())),
					item.getDisplayName().copy().withStyle(ChatFormatting.AQUA)
			));
			player.getInventory().placeItemBackInInventory(item);
		}
		for (final ItemStack item : collectibles) {
			// TODO: Can we have a proper interface?
			grantCollectible(player, item);
		}
	}

	private static void grantCollectible(final ServerPlayer player, final ItemStack item) {
		final CommandSourceStack source = player.getServer().createCommandSourceStack();
		String commandBuilder = "collectible give " +
				player.getGameProfile().getName() +
				" " +
				new ItemInput(item.getItemHolder(), item.getComponentsPatch()).serialize(player.registryAccess());
		player.getServer().getCommands().performPrefixedCommand(source, commandBuilder);
	}
}
