package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContextKeys;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;

import java.util.Optional;
import java.util.function.Supplier;

public record ItemCraftedTrigger(Optional<ItemPredicate> itemPredicate, GameActionList action) implements IGameBehavior {
	public static final MapCodec<ItemCraftedTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ItemPredicate.CODEC.optionalFieldOf("item").forGetter(ItemCraftedTrigger::itemPredicate),
			GameActionList.MAP_CODEC.forGetter(ItemCraftedTrigger::action)
	).apply(i, ItemCraftedTrigger::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		action.register(game, events);

		events.listen(GamePlayerEvents.CRAFT, (player, crafted, craftingContainer) -> {
			if (itemPredicate.isEmpty() || itemPredicate.get().test(crafted)) {
				final ContextMap context = new ContextMap.Builder()
						.withParameter(GameActionContextKeys.ITEM, crafted)
						.withParameter(GameActionContextKeys.COUNT, crafted.getCount())
						.create(ContextKeySet.EMPTY);
				action.apply(game, context, ActionSubjects.ofPlayer(player));
			}
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.ITEM_CRAFTED;
	}
}
