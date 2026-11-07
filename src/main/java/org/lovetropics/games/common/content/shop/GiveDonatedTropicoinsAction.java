package org.lovetropics.games.common.content.shop;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.integration.game_actions.GamePackage;
import org.slf4j.Logger;

/// Gives the targets tropicoins for the donated amount of the package, at the current rate of the [Shop]
///
/// @param fallbackAmount the tropicoins to give for packages without an amount, such as those sent by command
public record GiveDonatedTropicoinsAction(
		int fallbackAmount
) implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<GiveDonatedTropicoinsAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.optionalFieldOf("fallback_amount", 1).forGetter(GiveDonatedTropicoinsAction::fallbackAmount)
	).apply(i, GiveDonatedTropicoinsAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			Shop shop = game.state().getOrNull(Shop.KEY);
			if (shop == null) {
				return false;
			}
			int tropicoins = getTropicoins(context, shop);
			return shop.coinsScope().applyTo(game, targets, statistics -> statistics.incrementInt(StatisticKey.TROPICOINS, tropicoins));
		});
	}

	private int getTropicoins(ContextMap context, Shop shop) {
		GamePackage gamePackage = context.getOptional(GameActionContextKeys.PACKAGE);
		if (gamePackage == null || gamePackage.amount().isEmpty()) {
			LOGGER.warn("Received a package without a donated amount, giving {} tropicoins: {}. Bleh :(", fallbackAmount, gamePackage);
			return fallbackAmount;
		}
		return Mth.floor(gamePackage.amount().get() * shop.tropicoinsPerDonatedAmount());
	}

	@Override
	public MapCodec<GiveDonatedTropicoinsAction> codec() {
		return CODEC;
	}
}
