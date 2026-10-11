package org.lovetropics.games.common.content.mcsr.shop;

import com.mojang.serialization.MapCodec;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.SubGameEvents;
import org.lovetropics.games.common.core.game.state.GameStateMap;

import java.util.List;

/// Sets up a [Shop] for this game, which participants can use from this phase and from any of its sub-phases
public final class ShopBehavior implements IGameBehavior {
	public static final MapCodec<ShopBehavior> CODEC = ShopOffer.CODEC.listOf().fieldOf("offers")
			.xmap(ShopBehavior::new, b -> b.offers);

	private static final int BALANCE_UPDATE_INTERVAL = 10;

	private final List<ShopOffer> offers;

	private Shop shop;

	public ShopBehavior(List<ShopOffer> offers) {
		this.offers = offers;
	}

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		shop = instanceState.register(Shop.KEY, new Shop(game, offers));
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		for (ShopOffer offer : offers) {
			offer.actions().register(game, events);
		}

		events.listen(GamePlayerEvents.ADD, shop::updatePlayer);
		events.listen(GamePlayerEvents.REMOVE, shop::removeFromPlayer);

		// Players moving into a sub-phase are removed from this one, which clears their shop - send it again once they arrive
		events.listen(SubGameEvents.CREATE, (subGame, subEvents) -> {
			subEvents.listen(GamePlayerEvents.ADD, shop::updatePlayer);
			subEvents.listen(GamePlayerEvents.REMOVE, shop::removeFromPlayer);
		});

		// Tropicoins can come from anywhere, e.g. a donation
		events.listen(GamePhaseEvents.TICK, () -> {
			if (game.ticks() % BALANCE_UPDATE_INTERVAL == 0) {
				shop.updateBalances();
			}
		});
	}
}
