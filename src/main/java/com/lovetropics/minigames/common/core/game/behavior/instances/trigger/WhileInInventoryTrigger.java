package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.CraftingContainer;

import java.util.UUID;
import java.util.function.Supplier;

public record WhileInInventoryTrigger(
		ItemPredicate itemPredicate,
		GameActionList apply,
		GameActionList clear,
		boolean stack
) implements IGameBehavior {
	public static final MapCodec<WhileInInventoryTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ItemPredicate.CODEC.fieldOf("item").forGetter(WhileInInventoryTrigger::itemPredicate),
			GameActionList.CODEC.optionalFieldOf("apply", GameActionList.EMPTY).forGetter(WhileInInventoryTrigger::apply),
			GameActionList.CODEC.optionalFieldOf("clear", GameActionList.EMPTY).forGetter(WhileInInventoryTrigger::clear),
			Codec.BOOL.optionalFieldOf("stack", false).forGetter(WhileInInventoryTrigger::stack)
	).apply(i, WhileInInventoryTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		apply.register(game, events);
		clear.register(game, events);

		Object2IntMap<UUID> countByPlayer = new Object2IntOpenHashMap<>();
		events.listen(GamePlayerEvents.TICK, player -> {
			int oldCount = countByPlayer.getInt(player.getUUID());
			int newCount = countInInventory(player);
			if (newCount != oldCount) {
				countByPlayer.put(player.getUUID(), newCount);
				onItemCountChanged(game, player, oldCount, newCount);
			}
		});
	}

	private void onItemCountChanged(IGamePhase game, ServerPlayer player, int oldCount, int newCount) {
		if (!stack) {
			oldCount = Math.min(oldCount, 1);
			newCount = Math.min(newCount, 1);
		}

		if (newCount > oldCount) {
			for (int i = 0; i < newCount - oldCount; i++) {
				apply.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
			}
		} else {
			for (int i = 0; i < oldCount - newCount; i++) {
				clear.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
			}
		}
	}

	private int countInInventory(ServerPlayer player) {
		CraftingContainer craftSlots = player.inventoryMenu.getCraftSlots();
		return player.getInventory().clearOrCountMatchingItems(itemPredicate::test, 0, craftSlots);
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.WHILE_IN_INVENTORY;
	}
}
