package org.lovetropics.games.common.content.mcsr.shop;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.content.mcsr.McsrTexts;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.IGameState;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticsMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/// The shop of a game, where participants spend their tropicoins.
///
/// The shop belongs to the phase that declares it, which keeps the players' tropicoins. It is shared by all phases of the
/// game, so that players can use it from sub-phases too (e.g. each playing in their own world).
///
/// Offers can also be used on other participants, to sabotage them.
public final class Shop implements IGameState {
	public static final GameStateKey<Shop> KEY = GameStateKey.create("Shop");

	private final IGamePhase game;
	private final List<ShopOffer> offers;
	private final List<ShopClientState.Offer> clientOffers;
	/// What was last sent to each player who has the shop, to only update players whose shop changed
	private final Map<UUID, SentShop> sentShops = new HashMap<>();

	public Shop(IGamePhase game, List<ShopOffer> offers) {
		this.game = game;
		this.offers = offers;
		clientOffers = offers.stream().map(ShopOffer::asClientOffer).toList();
	}

	/// @param requestedTarget the player to use the offer on, if the buyer picked one
	public void purchase(ServerPlayer player, int offerIndex, Optional<UUID> requestedTarget) {
		if (!isParticipant(player) || offerIndex < 0 || offerIndex >= offers.size()) {
			return;
		}
		ShopOffer offer = offers.get(offerIndex);
		StatisticsMap statistics = game.statistics().forPlayer(player);
		int balance = getBalance(player);
		if (balance < offer.cost()) {
			player.sendSystemMessage(McsrTexts.CANNOT_AFFORD.apply(offer.name()).withStyle(ChatFormatting.RED));
			return;
		}

		// Players who don't pick a target get a random one
		boolean picked = offer.target() == ShopTarget.PICK && requestedTarget.isPresent();
		ServerPlayer target = switch (offer.target()) {
			case SELF -> player;
			case PICK -> picked ? getPickedTarget(player, requestedTarget.get()) : getRandomTarget(player);
			case RANDOM -> getRandomTarget(player);
		};
		if (target == null) {
			Component message = picked ? McsrTexts.TARGET_UNAVAILABLE : McsrTexts.NO_TARGETS;
			player.sendSystemMessage(message.copy().withStyle(ChatFormatting.RED));
			return;
		}

		ContextMap context = new ContextMap.Builder()
				.withParameter(GameActionContextKeys.PACKAGE_SENDER, PlayerKey.from(player).name())
				.create(ContextKeySet.EMPTY);
		// Only charge for what the player actually got
		if (!offer.actions().apply(game, context, ActionSubjects.ofPlayer(target))) {
			player.sendSystemMessage(McsrTexts.PURCHASE_FAILED.apply(offer.name()).withStyle(ChatFormatting.RED));
			return;
		}
		statistics.set(StatisticKey.TROPICOINS, balance - offer.cost());

		if (target == player) {
			player.sendSystemMessage(McsrTexts.PURCHASED.apply(offer.name(), offer.cost()).withStyle(ChatFormatting.GREEN));
		} else {
			announceSabotage(player, target, offer);
		}
		PlayerSet.of(player).playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);
		updatePlayer(player);
	}

	/// @return the picked player, or `null` if they can't be targeted, e.g. because they left the game since
	private @Nullable ServerPlayer getPickedTarget(ServerPlayer buyer, UUID targetId) {
		ServerPlayer target = game.server().getPlayerList().getPlayer(targetId);
		return target != null && target != buyer && isParticipant(target) ? target : null;
	}

	private @Nullable ServerPlayer getRandomTarget(ServerPlayer buyer) {
		List<ServerPlayer> targets = getTargets(buyer);
		return !targets.isEmpty() ? targets.get(game.random().nextInt(targets.size())) : null;
	}

	/// @return the participants who offers can be used on by the given player, no matter which phase of the game they are in
	private List<ServerPlayer> getTargets(ServerPlayer viewer) {
		List<ServerPlayer> targets = new ArrayList<>();
		for (ServerPlayer player : game.participants()) {
			if (player != viewer) {
				targets.add(player);
			}
		}
		return targets;
	}

	private void announceSabotage(ServerPlayer buyer, ServerPlayer target, ShopOffer offer) {
		Component offerName = offer.name().copy().withStyle(ChatFormatting.AQUA);
		buyer.sendSystemMessage(McsrTexts.USED_ON.apply(offerName, target.getDisplayName(), offer.cost()).withStyle(ChatFormatting.GREEN));
		target.sendSystemMessage(McsrTexts.SABOTAGED_BY.apply(buyer.getDisplayName(), offerName).withStyle(ChatFormatting.RED));
		PlayerSet.of(target).playSound(SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.4f, 1.5f);

		Component broadcast = McsrTexts.SABOTAGE_BROADCAST.apply(buyer.getDisplayName(), offerName, target.getDisplayName()).withStyle(ChatFormatting.GRAY);
		for (ServerPlayer player : game.allPlayers()) {
			if (player != buyer && player != target) {
				player.sendSystemMessage(broadcast);
			}
		}
	}

	/// Sends the shop again to players whose shop changed since, e.g. after a donation or when the points of other players changed
	public void updateChangedPlayers() {
		// Only players who have the shop: players who left the game keep their role, so must not get it back
		for (UUID playerId : List.copyOf(sentShops.keySet())) {
			ServerPlayer player = game.server().getPlayerList().getPlayer(playerId);
			if (player != null && !createSentShop(player).equals(sentShops.get(playerId))) {
				updatePlayer(player);
			}
		}
	}

	/// Sends the shop to the given player, if they are playing - no matter which phase of the game they are in
	public void updatePlayer(ServerPlayer player) {
		if (!isParticipant(player)) {
			return;
		}
		SentShop shop = createSentShop(player);
		sentShops.put(player.getUUID(), shop);
		GameClientState.sendToPlayer(new ShopClientState(shop.balance, clientOffers, shop.targets), player);
	}

	public void removeFromPlayer(ServerPlayer player) {
		sentShops.remove(player.getUUID());
		GameClientState.removeFromPlayer(GameClientStateTypes.SHOP.get(), player);
	}

	private SentShop createSentShop(ServerPlayer player) {
		List<ShopClientState.Target> targets = getTargets(player).stream()
				.map(target -> new ShopClientState.Target(target.getUUID(), PlayerKey.from(target).name(), getPoints(target)))
				.sorted(Comparator.comparingInt(ShopClientState.Target::points).reversed())
				.toList();
		return new SentShop(getBalance(player), targets);
	}

	private boolean isParticipant(ServerPlayer player) {
		return game.getRoleFor(player) == PlayerRole.PARTICIPANT;
	}

	private int getBalance(ServerPlayer player) {
		return game.statistics().forPlayer(player).getOr(StatisticKey.TROPICOINS, 0);
	}

	private int getPoints(ServerPlayer player) {
		return game.statistics().forPlayer(player).getOr(StatisticKey.POINTS, 0);
	}

	private record SentShop(int balance, List<ShopClientState.Target> targets) {
	}
}
