package org.lovetropics.games.common.content.mcsr.shop;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.context.ContextMap;
import org.lovetropics.games.common.content.mcsr.McsrTexts;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.IGameState;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticsMap;

import java.util.List;
import java.util.UUID;

/// The shop of a game, where participants spend their tropicoins.
///
/// The shop belongs to the phase that declares it, which keeps the players' tropicoins. It is shared by all phases of the
/// game, so that players can use it from sub-phases too (e.g. each playing in their own world).
public final class Shop implements IGameState {
	public static final GameStateKey<Shop> KEY = GameStateKey.create("Shop");

	private final IGamePhase game;
	private final List<ShopOffer> offers;
	private final List<ShopClientState.Offer> clientOffers;
	/// The balance last sent to each player who has the shop, to only update players whose balance changed
	private final Object2IntMap<UUID> sentBalances = new Object2IntOpenHashMap<>();

	public Shop(IGamePhase game, List<ShopOffer> offers) {
		this.game = game;
		this.offers = offers;
		clientOffers = offers.stream().map(ShopOffer::asClientOffer).toList();
		sentBalances.defaultReturnValue(-1);
	}

	public void purchase(ServerPlayer player, int offerIndex) {
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

		// Only charge for what the player actually got
		if (!offer.actions().apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player))) {
			player.sendSystemMessage(McsrTexts.PURCHASE_FAILED.apply(offer.name()).withStyle(ChatFormatting.RED));
			return;
		}
		statistics.set(StatisticKey.TROPICOINS, balance - offer.cost());

		player.sendSystemMessage(McsrTexts.PURCHASED.apply(offer.name(), offer.cost()).withStyle(ChatFormatting.GREEN));
		PlayerSet.of(player).playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);
		updatePlayer(player);
	}

	/// Sends the shop again to players whose balance changed since, e.g. after a donation
	public void updateBalances() {
		// Only players who have the shop: players who left the game keep their role, so must not get it back
		for (UUID playerId : List.copyOf(sentBalances.keySet())) {
			ServerPlayer player = game.server().getPlayerList().getPlayer(playerId);
			if (player != null && sentBalances.getInt(playerId) != getBalance(player)) {
				updatePlayer(player);
			}
		}
	}

	/// Sends the shop to the given player, if they are playing - no matter which phase of the game they are in
	public void updatePlayer(ServerPlayer player) {
		if (!isParticipant(player)) {
			return;
		}
		int balance = getBalance(player);
		sentBalances.put(player.getUUID(), balance);
		GameClientState.sendToPlayer(new ShopClientState(balance, clientOffers), player);
	}

	public void removeFromPlayer(ServerPlayer player) {
		sentBalances.removeInt(player.getUUID());
		GameClientState.removeFromPlayer(GameClientStateTypes.SHOP.get(), player);
	}

	private boolean isParticipant(ServerPlayer player) {
		return game.getRoleFor(player) == PlayerRole.PARTICIPANT;
	}

	private int getBalance(ServerPlayer player) {
		Integer balance = game.statistics().forPlayer(player).get(StatisticKey.TROPICOINS);
		return balance != null ? balance : 0;
	}
}
