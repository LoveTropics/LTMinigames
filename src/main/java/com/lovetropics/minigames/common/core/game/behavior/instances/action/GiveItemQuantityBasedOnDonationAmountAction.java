package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStackTemplate;

public record GiveItemQuantityBasedOnDonationAmountAction(ItemStackTemplate item, double amount) implements IGameBehavior {
	public static final MapCodec<GiveItemQuantityBasedOnDonationAmountAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ItemStackTemplate.CODEC.fieldOf("item").forGetter(GiveItemQuantityBasedOnDonationAmountAction::item),
			Codec.DOUBLE.fieldOf("amount").forGetter(GiveItemQuantityBasedOnDonationAmountAction::amount)
	).apply(i, GiveItemQuantityBasedOnDonationAmountAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToPlayers(game, (context, player) -> givePlayerItems(player));
	}

	private boolean givePlayerItems(final ServerPlayer player) {
		// TODO maybe want to give this to some kind of shop inventory instead

		boolean changed = Util.addItemStackToInventory(player, item.create());

		if (changed) {
			player.inventoryMenu.broadcastChanges();
			return true;
		} else {
			return false;
		}
	}
}
