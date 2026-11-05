package org.lovetropics.games.common.core.advancement;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.duck.PlayerAdvancementsExtension;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/// Listens to advancement criteria for every player on the server, as if they were the criteria of an advancement.
/// Yucky but necessary
public final class CriteriaTracker {
	private static final Map<AdvancementHolder, CriteriaTracker> ACTIVE = new HashMap<>();
	private static int nextId;

	private final AdvancementHolder holder;
	private final Predicate<ServerPlayer> canProgress;
	private final Consumer<ServerPlayer> onCompleted;
	private final Map<UUID, AdvancementProgress> progressByPlayer = new HashMap<>();

	public CriteriaTracker(Map<String, Criterion<?>> criteria, AdvancementRequirements requirements, Predicate<ServerPlayer> canProgress, Consumer<ServerPlayer> onCompleted) {
		Identifier id = Identifier.fromNamespaceAndPath(LoveTropics.ID, "tracked/" + nextId++);
		holder = new AdvancementHolder(id, new Advancement(Optional.empty(), Optional.empty(), AdvancementRewards.EMPTY, criteria, requirements, false));
		this.canProgress = canProgress;
		this.onCompleted = onCompleted;
	}

	public static @Nullable CriteriaTracker get(AdvancementHolder holder) {
		return ACTIVE.isEmpty() ? null : ACTIVE.get(holder);
	}

	public static void addListenersTo(PlayerAdvancements advancements, ServerPlayer player) {
		if (player.isFakePlayer()) {
			return;
		}
		for (CriteriaTracker tracker : ACTIVE.values()) {
			tracker.addListeners(advancements, player);
		}
	}

	public void start(MinecraftServer server) {
		startAll(server, List.of(this));
	}

	/// starts several trackers at once, which is cheaper than starting them one by one
	public static void startAll(MinecraftServer server, Collection<CriteriaTracker> trackers) {
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		boolean checkInventory = false;
		for (CriteriaTracker tracker : trackers) {
			if (ACTIVE.putIfAbsent(tracker.holder, tracker) != null) {
				continue;
			}
			for (ServerPlayer player : players) {
				tracker.addListeners(player.getAdvancements(), player);
			}
			checkInventory |= tracker.holder.value().criteria().values().stream().anyMatch(c -> c.trigger() == CriteriaTriggers.INVENTORY_CHANGED);
		}
		if (checkInventory) {
			for (ServerPlayer player : players) {
				triggerInventory(player);
			}
		}
	}

	public void stop(MinecraftServer server) {
		if (!ACTIVE.remove(holder, this)) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			removeListeners(player.getAdvancements());
		}
		progressByPlayer.clear();
	}

	public boolean award(PlayerAdvancements advancements, ServerPlayer player, String criterion) {
		if (!canProgress.test(player)) {
			return false;
		}
		AdvancementProgress progress = getProgress(player);
		if (!progress.grantProgress(criterion)) {
			return false;
		}
		Criterion<?> granted = holder.value().criteria().get(criterion);
		if (granted != null) {
			((PlayerAdvancementsExtension) advancements).lt$removeListener(granted.trigger(), new PlayerAdvancements.TriggerInstanceKey(holder, criterion));
		}
		if (progress.isDone()) {
			removeListeners(advancements);
			onCompleted.accept(player);
		}
		return true;
	}

	private void addListeners(PlayerAdvancements advancements, ServerPlayer player) {
		AdvancementProgress progress = getProgress(player);
		if (progress.isDone()) {
			return;
		}
		PlayerAdvancementsExtension extension = (PlayerAdvancementsExtension) advancements;
		holder.value().criteria().forEach((name, criterion) -> {
			CriterionProgress criterionProgress = progress.getCriterion(name);
			if (criterionProgress != null && !criterionProgress.isDone()) {
				extension.lt$addListener(criterion, new PlayerAdvancements.TriggerInstanceKey(holder, name));
			}
		});
	}

	private void removeListeners(PlayerAdvancements advancements) {
		PlayerAdvancementsExtension extension = (PlayerAdvancementsExtension) advancements;
		holder.value().criteria().forEach((name, criterion) ->
				extension.lt$removeListener(criterion.trigger(), new PlayerAdvancements.TriggerInstanceKey(holder, name))
		);
	}

	private AdvancementProgress getProgress(ServerPlayer player) {
		return progressByPlayer.computeIfAbsent(player.getUUID(), _ -> {
			AdvancementProgress progress = new AdvancementProgress();
			progress.update(holder.value().requirements());
			return progress;
		});
	}

	private static void triggerInventory(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty()) {
				CriteriaTriggers.INVENTORY_CHANGED.trigger(player, inventory, stack);
			}
		}
	}
}
