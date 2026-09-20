package org.lovetropics.games.common.core.game.behavior.instances.trigger;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public record ItemUsedTrigger(
		GameActionList sourceActions,
		ItemPredicate itemUsed,
		boolean consume
) implements IGameBehavior {

	public static final MapCodec<ItemUsedTrigger> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			GameActionList.CODEC.optionalFieldOf("source_actions", GameActionList.EMPTY).forGetter(ItemUsedTrigger::sourceActions),
			ItemPredicate.CODEC.fieldOf("item_used").forGetter(ItemUsedTrigger::itemUsed),
			Codec.BOOL.optionalFieldOf("consume", false).forGetter(ItemUsedTrigger::consume)
	).apply(instance, ItemUsedTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		sourceActions.register(game, events);

		events.listen(GamePlayerEvents.USE_ITEM, (player, hand) -> {
			ItemStack usedItem = player.getItemInHand(hand);
			if (!itemUsed.test(usedItem)) {
				return InteractionResult.PASS;
			}

			if (sourceActions.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player))) {
				if (consume) {
					usedItem.consume(1, player);
				}
				return InteractionResult.SUCCESS_SERVER;
			} else {
				return InteractionResult.FAIL;
			}
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.ITEM_USED;
	}
}
