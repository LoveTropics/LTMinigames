package org.lovetropics.games.common.core.game.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.player.MutablePlayerSet;
import org.lovetropics.games.common.core.item.MinigameDataComponents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SelectorItems<V> {
	private final Handlers<V> handlers;
	private final List<V> values = new ArrayList<>();

	private final MutablePlayerSet playersWithSelectors = new MutablePlayerSet();

	public SelectorItems(Handlers<V> handlers, Collection<V> values) {
		this.handlers = handlers;
		this.values.addAll(values);
	}

	public void set(Collection<V> values) {
		this.values.clear();
		this.values.addAll(values);
		for (ServerPlayer player : playersWithSelectors) {
			resetSelectorsFor(player);
		}
	}

	public void applyTo(EventRegistrar events) {
		events.listen(GamePlayerEvents.USE_ITEM, this::onUseItem);
		events.listen(GamePlayerEvents.THROW_ITEM, this::onThrowItem);

		events.listen(GamePlayerEvents.REMOVE, playersWithSelectors::remove);
	}

	public void giveSelectorsTo(ServerPlayer player) {
		if (playersWithSelectors.add(player)) {
			resetSelectorsFor(player);
		}
	}

	private void resetSelectorsFor(ServerPlayer player) {
		removeSelectorsFrom(player);
		for (V value : values) {
			Item item = handlers.getItemFor(value);
			player.addItem(createSelectorItem(item, value));
		}
	}

	public static void removeSelectorsFrom(ServerPlayer player) {
		CraftingContainer craftSlots = player.inventoryMenu.getCraftSlots();
		player.getInventory().clearOrCountMatchingItems(item -> item.has(MinigameDataComponents.SELECTOR), -1, craftSlots);
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
