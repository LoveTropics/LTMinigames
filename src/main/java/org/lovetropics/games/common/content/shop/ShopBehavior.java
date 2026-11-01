package org.lovetropics.games.common.content.shop;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.Commands;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.instances.command.CustomCommandBehavior;
import org.lovetropics.games.common.core.game.state.GameStateMap;

import java.util.List;

/// Sets up a [Shop] for this game, which participants can use from this phase and from any of its sub-phases
public final class ShopBehavior implements IGameBehavior {
	public static final MapCodec<ShopBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ShopOffer.CODEC.listOf().fieldOf("offers").forGetter(b -> b.offers),
			Codec.doubleRange(0.0, Double.MAX_VALUE).optionalFieldOf("tropicoins_per_donated_amount", 1.0).forGetter(b -> b.tropicoinsPerDonatedAmount)
	).apply(i, ShopBehavior::new));

	private static final int UPDATE_INTERVAL = 10;

	private final List<ShopOffer> offers;
	private final double tropicoinsPerDonatedAmount;

	private Shop shop;

	public ShopBehavior(List<ShopOffer> offers, double tropicoinsPerDonatedAmount) {
		this.offers = offers;
		this.tropicoinsPerDonatedAmount = tropicoinsPerDonatedAmount;
	}

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		shop = instanceState.register(Shop.KEY, new Shop(game, offers, tropicoinsPerDonatedAmount));
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		for (ShopOffer offer : offers) {
			offer.actions().register(game, events);
		}

		events.listen(GamePlayerEvents.ADD, shop::updatePlayer);
		events.listen(GamePlayerEvents.REMOVE, shop::removeFromPlayer);

		// Tropicoins can come from anywhere (e.g. a donation), and other players' points change as they play
		events.listen(GamePhaseEvents.TICK, () -> {
			if (game.ticks() % UPDATE_INTERVAL == 0) {
				shop.updateChangedPlayers();
			}
		});

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, _) -> commands.register(Commands.literal("shop")
				.requires(source -> CustomCommandBehavior.PermissionLevel.OVERLORD.hasPermission(game, source))
				.then(Commands.literal("donation_rate")
						.executes(context -> {
							context.getSource().sendSuccess(() -> ShopTexts.DONATION_RATE.apply(shop.tropicoinsPerDonatedAmount()), false);
							return 1;
						})
						.then(Commands.argument("rate", DoubleArgumentType.doubleArg(0.0))
								.executes(context -> {
									shop.setTropicoinsPerDonatedAmount(DoubleArgumentType.getDouble(context, "rate"));
									context.getSource().sendSuccess(() -> ShopTexts.DONATION_RATE.apply(shop.tropicoinsPerDonatedAmount()), true);
									return 1;
								})
						)
				)
		));
	}
}
