package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public record ItemUsedTrigger(
		GameActionList sourceActions,
		ItemPredicate itemUsed
) implements IGameBehavior {

	public static final MapCodec<ItemUsedTrigger> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		GameActionList.CODEC.optionalFieldOf("source_actions", GameActionList.EMPTY).forGetter(ItemUsedTrigger::sourceActions),
		ItemPredicate.CODEC.fieldOf("item_used").forGetter(ItemUsedTrigger::itemUsed)
	).apply(instance, ItemUsedTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		sourceActions.register(game, events);

		events.listen(GamePlayerEvents.USE_ITEM, (player, hand) -> {
			final ItemStack usedItem = player.getItemInHand(hand);

			if (itemUsed.test(usedItem)) {
				sourceActions.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));

				return InteractionResult.SUCCESS;
			}

			return InteractionResult.PASS;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.ITEM_USED;
	}
}
