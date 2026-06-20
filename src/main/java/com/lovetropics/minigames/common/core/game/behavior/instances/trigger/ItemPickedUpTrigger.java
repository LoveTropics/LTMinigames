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
import com.lovetropics.minigames.common.core.game.behavior.event.PickUpResult;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.function.Supplier;

public record ItemPickedUpTrigger(Optional<ItemPredicate> itemPredicate, GameActionList action, boolean consume) implements IGameBehavior {
	public static final MapCodec<ItemPickedUpTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ItemPredicate.CODEC.optionalFieldOf("item").forGetter(ItemPickedUpTrigger::itemPredicate),
			GameActionList.MAP_CODEC.forGetter(ItemPickedUpTrigger::action),
			Codec.BOOL.optionalFieldOf("consume", false).forGetter(ItemPickedUpTrigger::consume)
	).apply(i, ItemPickedUpTrigger::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		action.register(game, events);

		events.listen(GamePlayerEvents.PICK_UP_ITEM, (player, item) -> {
			final ItemStack stack = item.getItem();
			if (itemPredicate.isEmpty() || itemPredicate.get().test(stack)) {
				final ContextMap context = new ContextMap.Builder()
						.withParameter(GameActionContextKeys.ITEM, stack)
						.withParameter(GameActionContextKeys.COUNT, stack.getCount())
						.create(ContextKeySet.EMPTY);
				action.apply(game, context, ActionSubjects.ofPlayer(player));
				return consume ? PickUpResult.DISCARD : PickUpResult.PASS;
			}
			return PickUpResult.PASS;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.ITEM_PICKED_UP;
	}
}
