package org.lovetropics.games.common.core.integration.game_actions;

import com.mojang.serialization.MapCodec;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.event.GamePackageEvents;

public record DonationGameAction(Donation donation) implements GameAction {
	public static final MapCodec<DonationGameAction> CODEC = Donation.MAP_CODEC.xmap(DonationGameAction::new, DonationGameAction::donation);

	@Override
	public boolean apply(IGamePhase game) {
		game.invoker(GamePackageEvents.RECEIVE_DONATION).onReceiveDonation(donation);
		return true;
	}
}
