package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.world.item.ItemStack;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;

public record RemoveItemFromPlayerAction(
		ItemPredicate itemPredicate
) implements IGameBehavior {
	public static final MapCodec<RemoveItemFromPlayerAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ItemPredicate.CODEC.fieldOf("item").forGetter(RemoveItemFromPlayerAction::itemPredicate)
	).apply(i, RemoveItemFromPlayerAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			targets.asPlayers(game).forEach(serverPlayer -> {
				for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
					ItemStack item = serverPlayer.getInventory().getItem(i);
					if(itemPredicate.test(item)){
						serverPlayer.getInventory().removeItem(i, item.count());
					}
				}
			});
			return true;
		});
	}
}
