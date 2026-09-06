package com.lovetropics.minigames.common.core.game.util;

import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.item.MinigameDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

public record SelectorItems<V>(Handlers<V> handlers, V[] values) {
	public void applyTo(EventRegistrar events) {
		events.listen(GamePlayerEvents.USE_ITEM, this::onUseItem);
		events.listen(GamePlayerEvents.THROW_ITEM, this::onThrowItem);
	}

	public void giveSelectorsTo(ServerPlayer player) {
		for (V value : values) {
			Item item = handlers.getItemFor(value);
			player.addItem(createSelectorItem(item, value));
		}
	}

	private InteractionResult onUseItem(ServerPlayer player, InteractionHand hand) {
		ItemStack heldStack = player.getItemInHand(hand);
		if (heldStack.isEmpty()) {
			return InteractionResult.PASS;
		}

		V value = getValueForSelector(heldStack);
		if (value != null) {
			handlers.onPlayerSelected(player, value);
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	private TriState onThrowItem(ServerPlayer player, ItemEntity entity) {
		V value = getValueForSelector(entity.getItem());
		return value != null ? TriState.FALSE : TriState.DEFAULT;
	}

	private @Nullable V getValueForSelector(ItemStack stack) {
		String id = stack.get(MinigameDataComponents.SELECTOR);
		if (id == null) {
			return null;
		}
		for (V value : values) {
			if (handlers.getIdFor(value).equals(id)) {
				return value;
			}
		}
		return null;
	}

	private ItemStack createSelectorItem(Item item, V value) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.CUSTOM_NAME, handlers.getNameFor(value));
		stack.set(MinigameDataComponents.SELECTOR, handlers.getIdFor(value));

		return stack;
	}

	public interface Handlers<V> {
		void onPlayerSelected(ServerPlayer player, V value);

		String getIdFor(V value);

		Component getNameFor(V value);

		Item getItemFor(V value);
	}
}
