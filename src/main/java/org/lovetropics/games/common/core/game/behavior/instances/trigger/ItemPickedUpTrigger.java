package org.lovetropics.games.common.core.game.behavior.instances.trigger;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.PickUpResult;
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
	public void register(IGamePhase game, EventRegistrar events) {
		action.register(game, events);

		events.listen(GamePlayerEvents.PICK_UP_ITEM, (player, item) -> {
			ItemStack stack = item.getItem();
			if (itemPredicate.isEmpty() || itemPredicate.get().test(stack)) {
				ContextMap context = new ContextMap.Builder()
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
