package com.lovetropics.minigames.common.core.integration.game_actions;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePackageEvents;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.MinecraftServer;

public record DonationGameAction(Donation donation) implements GameAction {
	public static final MapCodec<DonationGameAction> CODEC = Donation.MAP_CODEC.xmap(DonationGameAction::new, DonationGameAction::donation);

	@Override
	public boolean resolve(IGamePhase game, MinecraftServer server) {
		game.invoker(GamePackageEvents.RECEIVE_DONATION).onReceiveDonation(donation);
		return true;
	}
}
