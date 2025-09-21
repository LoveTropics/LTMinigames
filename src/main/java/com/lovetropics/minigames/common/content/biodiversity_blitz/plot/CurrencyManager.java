package com.lovetropics.minigames.common.content.biodiversity_blitz.plot;

import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public final class CurrencyManager implements IGameState {
	public static final GameStateKey<CurrencyManager> KEY = GameStateKey.create("Currency");

	private final IGamePhase game;

	private final Item item;
	private final Predicate<ItemStack> itemPredicate;

	private final Object2IntMap<UUID> trackedValues = new Object2IntOpenHashMap<>();
	private final Object2IntOpenHashMap<GameTeamKey> accumulator = new Object2IntOpenHashMap<>();

	public CurrencyManager(IGamePhase game, Item item) {
		this.game = game;
		this.item = item;
		itemPredicate = stack -> stack.getItem() == item;

		trackedValues.defaultReturnValue(0);
	}

	public void tickTracked() {
		for (ServerPlayer player : game.participants()) {
			if (player.tickCount % 5 == 0) {
				int value = get(player);
				setTracked(player, value);
			}
		}
	}

	private void setTracked(ServerPlayer player, int value) {
		int lastValue = trackedValues.put(player.getUUID(), value);
		if (lastValue != value) {
			game.invoker(BbEvents.CURRENCY_CHANGED).onCurrencyChanged(player, value, lastValue);
		}
	}

	private void incrementTracked(ServerPlayer player, int amount) {
		int value = trackedValues.getInt(player.getUUID());
		setTracked(player, value + amount);
	}

	public int set(ServerPlayer player, int value) {
		int oldValue = get(player);
		if (value > oldValue) {
			int increment = value - oldValue;
			return oldValue + add(player, increment);
		} else if (value < oldValue) {
			int decrement = oldValue - value;
			return oldValue - remove(player, decrement);
		} else {
			return value;
		}
	}

	public int add(ServerPlayer player, int amount) {
		int added = addToInventory(player, amount);
		incrementTracked(player, added);
		return added;
	}

	public void accumulate(GameTeamKey team, int amount) {
		int lastValue = accumulator.addTo(team, amount);
		int newValue = lastValue + amount;

		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		for (ServerPlayer player : teams.getPlayersForTeam(team)) {
			game.statistics().forPlayer(player)
					.set(StatisticKey.POINTS, newValue);
		}

		game.invoker(BbEvents.CURRENCY_ACCUMULATE).onCurrencyChanged(team, newValue, lastValue);
	}

	public int remove(ServerPlayer player, int amount) {
		int removed = removeFromInventory(player, amount);
		incrementTracked(player, -removed);
		return removed;
	}

	private int addToInventory(ServerPlayer player, int amount) {
		ItemStack stack = new ItemStack(item, amount);
		player.getInventory().add(stack);
		sendInventoryUpdate(player);
		return amount - stack.getCount();
	}

	private int removeFromInventory(ServerPlayer player, int amount) {
		int remaining = amount;

		List<Slot> slots = player.containerMenu.slots;
		for (Slot slot : slots) {
			remaining -= removeFromSlot(slot, remaining);
			if (remaining <= 0) {
				break;
			}
		}

		remaining -= ContainerHelper.clearOrCountMatchingItems(player.containerMenu.getCarried(), itemPredicate, remaining, false);

		sendInventoryUpdate(player);

		return amount - remaining;
	}

	private int removeFromSlot(Slot slot, int amount) {
		ItemStack stack = slot.getItem();
		if (itemPredicate.test(stack)) {
			int removed = Math.min(amount, stack.getCount());
			stack.shrink(removed);
			return removed;
		}

		return 0;
	}

	public int get(ServerPlayer player) {
		int count = 0;

		List<Slot> slots = player.containerMenu.slots;
		for (Slot slot : slots) {
			ItemStack stack = slot.getItem();
			if (itemPredicate.test(stack)) {
				count += stack.getCount();
			}
		}

		ItemStack stack = player.containerMenu.getCarried();
		if (itemPredicate.test(stack)) {
			count += stack.getCount();
		}

		return count;
	}

	public int getPoints(GameTeamKey team) {
		return accumulator.getOrDefault(team, 0);
	}

	private static void sendInventoryUpdate(ServerPlayer player) {
		player.containerMenu.broadcastChanges();
	}

	public void equalize() {
		int sum = IntStream.of(accumulator.values().toIntArray()).sum();
		int count = accumulator.keySet().size();
		int newSum = sum / count;

		for (GameTeamKey team : accumulator.keySet()) {
			int lastValue = accumulator.put(team, newSum);
			game.invoker(BbEvents.CURRENCY_ACCUMULATE).onCurrencyChanged(team, newSum, lastValue);
		}
	}
}
