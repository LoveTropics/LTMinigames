package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.integration.game_actions.GamePackage;
import org.lovetropics.games.common.util.Util;

/// Gives items according to how much was donated with the package that triggered this action, e.g. a tropicoin per
/// dollar. Packages without an amount, such as those sent without one by command, give the item as it is.
///
/// @param perAmount how many items to give for each unit of currency donated, rounded down
public record GiveItemQuantityBasedOnDonationAmountAction(ItemStackTemplate item, double perAmount) implements IGameBehavior {
	public static final MapCodec<GiveItemQuantityBasedOnDonationAmountAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ItemStackTemplate.CODEC.fieldOf("item").forGetter(GiveItemQuantityBasedOnDonationAmountAction::item),
			Codec.DOUBLE.optionalFieldOf("per_amount", 1.0).forGetter(GiveItemQuantityBasedOnDonationAmountAction::perAmount)
	).apply(i, GiveItemQuantityBasedOnDonationAmountAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToPlayers(game, (context, player) -> givePlayerItems(player, getCount(context)));
	}

	private int getCount(ContextMap context) {
		GamePackage gamePackage = context.getOptional(GameActionContextKeys.PACKAGE);
		if (gamePackage == null || gamePackage.amount().isEmpty()) {
			return item.count();
		}
		return (int) Math.floor(gamePackage.amount().get() * perAmount);
	}

	private boolean givePlayerItems(ServerPlayer player, int count) {
		// TODO maybe want to give this to some kind of shop inventory instead
		if (count <= 0) {
			return false;
		}

		// Large donations can give more items than fit in one stack
		ItemStack stack = item.create();
		int remaining = count;
		while (remaining > 0) {
			int stackCount = Math.min(remaining, stack.getMaxStackSize());
			Util.addItemStackToInventory(player, stack.copyWithCount(stackCount));
			remaining -= stackCount;
		}
		player.inventoryMenu.broadcastChanges();
		return true;
	}
}
