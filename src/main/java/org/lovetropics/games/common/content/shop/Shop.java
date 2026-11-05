package org.lovetropics.games.common.content.shop;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.SoundRegistry;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.Ghosts;
import org.lovetropics.games.common.core.game.state.IGameState;
import org.lovetropics.games.common.core.game.state.Overlords;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/// The shop of a game, where participants and ghosts spend their tropicoins.
///
/// The shop belongs to the phase that declares it, which keeps the players' tropicoins.
public final class Shop implements IGameState {
	public static final GameStateKey<Shop> KEY = GameStateKey.create("Shop");

	private final IGamePhase game;
	private final List<ShopOffer> playerOffers;
	private final List<ShopOffer> ghostOffers;
	private final List<ShopOffer> overlordOffers;
	/// What was last sent to each player who has the shop, to only update players whose shop changed
	private final Map<UUID, SentShop> sentShops = new HashMap<>();

	private double tropicoinsPerDonatedAmount;

	public Shop(IGamePhase game, List<ShopOffer> offers, double tropicoinsPerDonatedAmount) {
		this.game = game;
		playerOffers = offers.stream().filter(ShopOffer::isUsableByPlayers).toList();
		ghostOffers = offers.stream().filter(ShopOffer::isUsableByGhosts).toList();
		overlordOffers = offers.stream().filter(ShopOffer::isUsableByOverlords).map(ShopOffer::forOverlords).toList();
		this.tropicoinsPerDonatedAmount = tropicoinsPerDonatedAmount;
	}

	public double tropicoinsPerDonatedAmount() {
		return tropicoinsPerDonatedAmount;
	}

	public void setTropicoinsPerDonatedAmount(double tropicoinsPerDonatedAmount) {
		this.tropicoinsPerDonatedAmount = tropicoinsPerDonatedAmount;
	}

	/// @param requestedTarget the player to use the offer on, if the buyer picked one
	public void purchase(ServerPlayer player, int offerIndex, Optional<UUID> requestedTarget) {
		List<ShopOffer> offers = getOffers(player);
		if (offerIndex < 0 || offerIndex >= offers.size()) {
			return;
		}
		ShopOffer offer = offers.get(offerIndex);
		int balance = getBalance(player);
		if (balance < offer.cost()) {
			player.sendSystemMessage(ShopTexts.CANNOT_AFFORD.apply(offer.name()).withStyle(ChatFormatting.RED));
			return;
		}

		// Players who don't pick a target get a random one
		boolean picked = offer.target() == ShopTarget.PICK && requestedTarget.isPresent();
		ServerPlayer targetPlayer = switch (offer.target()) {
			case SELF -> player;
			case PICK -> picked ? getPickedTarget(player, requestedTarget.get()) : getRandomTarget(player);
			case RANDOM -> getRandomTarget(player);
			case GLOBAL -> null;
		};
		if (targetPlayer == null && offer.target() != ShopTarget.GLOBAL) {
			Component message = picked ? ShopTexts.TARGET_UNAVAILABLE : ShopTexts.NO_TARGETS;
			player.sendSystemMessage(message.copy().withStyle(ChatFormatting.RED));
			return;
		}

		ActionSubjects<?> targets = targetPlayer != null ? ActionSubjects.ofPlayer(targetPlayer) : ActionSubjects.EMPTY;

		ContextMap context = new ContextMap.Builder()
				.withParameter(GameActionContextKeys.PACKAGE_SENDER, PlayerKey.from(player).name())
				.create(ContextKeySet.EMPTY);
		// Only charge for what the player actually got
		if (!offer.actions().apply(game, context, targets)) {
			player.sendSystemMessage(ShopTexts.PURCHASE_FAILED.apply(offer.name()).withStyle(ChatFormatting.RED));
			return;
		}

		// It's important right now to not instantiate statistics if we don't actually want it, because everything is terrible
		if (offer.cost() > 0) {
			game.statistics().forPlayer(player).set(StatisticKey.TROPICOINS, balance - offer.cost());
		}

		if (targetPlayer == null || targetPlayer == player) {
			player.sendSystemMessage(offer.cost() == 0 ? ShopTexts.USED.apply(offer.name()) : ShopTexts.USED_WITH_COST.apply(offer.name(), offer.cost()));
			if (targetPlayer == null) {
				announceUseGlobal(player, offer);
			}
		} else {
			announceUseOnTarget(player, targetPlayer, offer);
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

	private void announceUseGlobal(ServerPlayer buyer, ShopOffer offer) {
		Component offerName = offer.name().copy().withStyle(ChatFormatting.AQUA);
		PlayerSet players = game.allPlayers();

		if (offer.flavor() == ShopOfferFlavor.SABOTAGE) {
			players.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.4f, 1.5f);
			players.sendMessage(ShopTexts.USE_GLOBAL_BROADCAST.apply(buyer.getDisplayName(), offerName).withStyle(ChatFormatting.RED));
		} else {
			players.playSound(SoundRegistry.PACKAGE_RECEIVE.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
			players.sendMessage(ShopTexts.USE_GLOBAL_BROADCAST.apply(buyer.getDisplayName(), offerName).withStyle(ChatFormatting.GREEN));
		}
	}

	private void announceUseOnTarget(ServerPlayer buyer, ServerPlayer target, ShopOffer offer) {
		Component offerName = offer.name().copy().withStyle(ChatFormatting.AQUA);
		buyer.sendSystemMessage(offer.cost() > 0 ? ShopTexts.USED_ON_WITH_COST.apply(offerName, target.getDisplayName(), offer.cost()) : ShopTexts.USED_ON.apply(offerName, target.getDisplayName()));

		if (offer.flavor() == ShopOfferFlavor.SABOTAGE) {
			PlayerSet.of(target).playSound(SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.4f, 1.5f);
			target.sendSystemMessage(ShopTexts.SABOTAGED_BY.apply(buyer.getDisplayName(), offerName));
		} else {
			PlayerSet.of(target).playSound(SoundRegistry.PACKAGE_RECEIVE.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
			target.sendSystemMessage(ShopTexts.BONUS_FROM.apply(buyer.getDisplayName(), offerName));
		}

		Component broadcast = ShopTexts.USE_ON_BROADCAST.apply(buyer.getDisplayName(), offerName, target.getDisplayName()).withStyle(ChatFormatting.GRAY);
		for (ServerPlayer player : game.allPlayers()) {
			if (player != buyer && player != target) {
				player.sendSystemMessage(broadcast);
			}
		}
	}

	/// Sends the shop again to players whose shop changed since, e.g. after a donation or when they became a ghost
	public void updateChangedPlayers() {
		game.allPlayers().forEach(this::updatePlayer);
	}

	/// Sends the shop to the given player, if they can use it - no matter which phase of the game they are in
	public void updatePlayer(ServerPlayer player) {
		SentShop shop = createShopFor(player);
		if (shop != null) {
			updatePlayer(player, shop);
		} else {
			removeFromPlayer(player);
		}
	}

	private void updatePlayer(ServerPlayer player, SentShop shop) {
		SentShop lastShop = sentShops.put(player.getUUID(), shop);
		if (shop.equals(lastShop)) {
			return;
		}

		List<ShopClientState.Offer> clientOffers = shop.offers.stream().map(ShopOffer::asClientOffer).toList();
		boolean anyHasCost = shop.offers.stream().anyMatch(offer -> offer.cost() > 0);

		GameClientState.sendToPlayer(new ShopClientState(anyHasCost ? Optional.of(shop.balance) : Optional.empty(), clientOffers, shop.targets), player);
	}

	public void removeFromPlayer(ServerPlayer player) {
		SentShop shop = sentShops.remove(player.getUUID());
		if (shop != null) {
			GameClientState.removeFromPlayer(GameClientStateTypes.SHOP.get(), player);
		}
	}

	private @Nullable SentShop createShopFor(ServerPlayer player) {
		List<ShopOffer> offers = getOffers(player);
		if (offers.isEmpty()) {
			return null;
		}
		List<ShopClientState.Target> targets = getTargets(player).stream()
				.map(target -> new ShopClientState.Target(target.getUUID(), target.getDisplayName(), getPoints(target)))
				.sorted(Comparator.comparingInt(ShopClientState.Target::points).reversed())
				.toList();
		return new SentShop(getBalance(player), offers, targets);
	}

	private List<ShopOffer> getOffers(ServerPlayer player) {
		if (Overlords.get(game).contains(player)) {
			return overlordOffers;
		} else if (isParticipant(player)) {
			return playerOffers;
		} else if (Ghosts.get(game).contains(player)) {
			return ghostOffers;
		}
		return List.of();
	}

	private boolean isParticipant(ServerPlayer player) {
		return game.getRoleFor(player) == PlayerRole.PARTICIPANT;
	}

	private int getBalance(ServerPlayer player) {
		return game.statistics().readForPlayer(player).getOr(StatisticKey.TROPICOINS, 0);
	}

	private int getPoints(ServerPlayer player) {
		return game.statistics().readForPlayer(player).getOr(StatisticKey.POINTS, 0);
	}

	private record SentShop(int balance, List<ShopOffer> offers, List<ShopClientState.Target> targets) {
	}
}
