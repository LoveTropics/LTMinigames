package org.lovetropics.games.common.content.shop;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.SoundRegistry;
import org.lovetropics.games.common.content.MinigameTexts;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjectDescriber;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;
import org.lovetropics.games.common.core.game.command.GameCommandRegistrar;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.Ghosts;
import org.lovetropics.games.common.core.game.state.IGameState;
import org.lovetropics.games.common.core.game.state.Overlords;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

/// The shop of a game, where participants and ghosts spend their tropicoins.
///
/// The shop belongs to the phase that declares it, which keeps the players' tropicoins.
public final class Shop implements IGameState {
	public static final GameStateKey<Shop> KEY = GameStateKey.create("Shop");

	private final IGamePhase game;
	private final List<ShopOffer> allOffers;
	private List<ShopOffer> playerOffers = List.of();
	private List<ShopOffer> ghostOffers = List.of();
	private List<ShopOffer> overlordOffers = List.of();
	/// What was last sent to each player who has the shop, to only update players whose shop changed
	private final Map<UUID, SentShop> sentShops = new HashMap<>();
	/// How many times each player bought each offer that has a limited stock, by offer id so that it survives changing the offer's cost
	private final Map<UUID, Object2IntOpenHashMap<String>> purchases = new HashMap<>();
	/// For each player or team, the game tick from which the offers that have a cooldown can be used on them again
	private final Map<UUID, Object2LongOpenHashMap<String>> cooldownEnds = new HashMap<>();

	private double tropicoinsPerDonatedAmount;

	private Shop(IGamePhase game, List<ShopOffer> offers, double tropicoinsPerDonatedAmount) {
		this.game = game;
		allOffers = new ArrayList<>(offers);
		rebuildOffers();
		this.tropicoinsPerDonatedAmount = tropicoinsPerDonatedAmount;
	}

	private void rebuildOffers() {
		playerOffers = allOffers.stream().filter(ShopOffer::isUsableByPlayers).toList();
		ghostOffers = allOffers.stream().filter(ShopOffer::isUsableByGhosts).toList();
		overlordOffers = allOffers.stream().filter(ShopOffer::isUsableByOverlords).map(ShopOffer::forOverlords).toList();
	}

	private boolean setOfferCost(String id, int cost) {
		for (int i = 0; i < allOffers.size(); i++) {
			ShopOffer offer = allOffers.get(i);
			if (offer.id().equals(id)) {
				allOffers.set(i, offer.withCost(cost));
				rebuildOffers();
				return true;
			}
		}
		return false;
	}

	public static Shop register(IGamePhase game, EventRegistrar events, List<ShopOffer> offers, double tropicoinsPerDonatedAmount) {
		Shop shop = new Shop(game, offers, tropicoinsPerDonatedAmount);
		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, _) -> {
			commands.register(Commands.literal("shop").then(Commands.literal("offer")
					.requires(GameCommandRegistrar.adminOrOverlord(game))
					.then(Commands.argument("id", StringArgumentType.string())
							.suggests((_, builder) -> SharedSuggestionProvider.suggest(shop.allOffers.stream().map(ShopOffer::id), builder))
							.then(Commands.literal("setcost")
									.then(Commands.argument("cost", IntegerArgumentType.integer(0))
											.executes(context -> {
												String id = StringArgumentType.getString(context, "id");
												int cost = IntegerArgumentType.getInteger(context, "cost");
												if (shop.setOfferCost(id, cost)) {
													context.getSource().sendSuccess(() -> Component.literal("Set shop offer " + id + " cost to " + cost), true);
												} else {
													context.getSource().sendFailure(Component.literal("No shop with offer " + id));
												}
												return 1;
											})
									)
							)
					)
			));
		});
		return shop;
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
		if (isSoldOut(player, offer)) {
			player.sendSystemMessage(ShopTexts.SOLD_OUT_FOR_YOU.apply(offer.name()).withStyle(ChatFormatting.RED));
			return;
		}
		int balance = getBalance(player);
		if (balance < offer.cost()) {
			player.sendSystemMessage(ShopTexts.CANNOT_AFFORD.apply(offer.name()).withStyle(ChatFormatting.RED));
			return;
		}

		// Players who don't pick a target get a random one
		ResolvedTarget target = getTarget(player, requestedTarget, offer);
		if (target == null) {
			Component message = requestedTarget.isPresent() ? ShopTexts.TARGET_UNAVAILABLE
					: offer.targetCooldownTicks().isPresent() && !getPlayerTargets(player).isEmpty() ? ShopTexts.ALL_ON_COOLDOWN
					: ShopTexts.NO_TARGETS;
			player.sendSystemMessage(message.copy().withStyle(ChatFormatting.RED));
			return;
		}
		// Someone else may have used it on the picked target since the buyer's shop was last sent
		if (target.id() != null && isOnCooldown(target.id(), offer)) {
			Component targetName = Objects.requireNonNullElse(ActionSubjectDescriber.describe(game, target.subjects(), null), MinigameTexts.EVERYONE_RECEIVER);
			player.sendSystemMessage(ShopTexts.TARGET_ON_COOLDOWN.apply(targetName, offer.name()).withStyle(ChatFormatting.RED));
			return;
		}

		ActionSubjects<?> targets = target.subjects();
		if (offer.includeSelf()) {
			if (offer.targetTeam()) {
				Set<GameTeamKey> teams = new ObjectOpenHashSet<>(targets.asTeams(game));
				GameTeamKey selfTeam = game.instanceState().getOrThrow(TeamState.KEY).getTeamForPlayer(player);
				if (selfTeam != null) {
					teams.add(selfTeam);
				}
				targets = ActionSubjects.ofTeams(teams);
			} else {
				Set<Entity> entities = new ReferenceOpenHashSet<>(targets.asEntities(game));
				entities.add(player);
				targets = ActionSubjects.ofEntities(entities);
			}
		}

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
		if (offer.stock().isPresent()) {
			purchases.computeIfAbsent(player.getUUID(), _ -> new Object2IntOpenHashMap<>()).addTo(offer.id(), 1);
		}
		Optional<Integer> cooldown = offer.targetCooldownTicks();
		if (cooldown.isPresent() && target.id() != null) {
			cooldownEnds.computeIfAbsent(target.id(), _ -> new Object2LongOpenHashMap<>()).put(offer.id(), game.ticks() + cooldown.get());
		}

		List<ServerPlayer> targetPlayers = targets.asPlayers(game);
		if (targets.isEmpty() || (targetPlayers.size() == 1 && targetPlayers.getFirst() == player)) {
			player.sendSystemMessage(offer.cost() == 0 ? ShopTexts.USED.apply(offer.name()) : ShopTexts.USED_WITH_COST.apply(offer.name(), offer.cost()));
			if (targets.isEmpty()) {
				announceUseGlobal(player, offer);
			}
		} else {
			announceUseOnTarget(player, targets, offer);
		}
		PlayerSet.of(player).playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);
		updatePlayer(player);
	}

	/// @return `null` if the target could not be resolved
	private @Nullable ResolvedTarget getTarget(ServerPlayer player, Optional<UUID> requestedTarget, ShopOffer offer) {
		boolean picked = offer.target() == ShopTarget.PICK && requestedTarget.isPresent();
		TeamState teams = offer.targetTeam() ? game.instanceState().getOrThrow(TeamState.KEY) : null;
		return switch (offer.target()) {
			case SELF -> {
				if (teams != null) {
					GameTeamKey playerTeam = teams.getTeamForPlayer(player);
					yield playerTeam != null ? new ResolvedTarget(ActionSubjects.ofTeam(playerTeam), null) : null;
				}
				yield new ResolvedTarget(ActionSubjects.ofPlayer(player), null);
			}
			case PICK -> {
				if (picked) {
					if (teams != null) {
						GameTeam pickedTeam = teams.getTeamByUuid(requestedTarget.get());
						yield pickedTeam != null ? new ResolvedTarget(ActionSubjects.ofTeam(pickedTeam.key()), pickedTeam.uuid()) : null;
					} else {
						ServerPlayer pickedTarget = getPickedTarget(player, requestedTarget.get());
						yield pickedTarget != null ? ResolvedTarget.of(pickedTarget) : null;
					}
				} else {
					ServerPlayer randomTarget = getRandomTarget(player, offer);
					yield randomTarget != null ? ResolvedTarget.of(randomTarget) : null;
				}
			}
			case RANDOM -> {
				ServerPlayer randomTarget = getRandomTarget(player, offer);
				yield randomTarget != null ? ResolvedTarget.of(randomTarget) : null;
			}
			case GLOBAL -> new ResolvedTarget(ActionSubjects.EMPTY, null);
		};
	}

	/// @return the picked player, or `null` if they can't be targeted, e.g. because they left the game since
	private @Nullable ServerPlayer getPickedTarget(ServerPlayer buyer, UUID targetId) {
		ServerPlayer target = game.server().getPlayerList().getPlayer(targetId);
		return target != null && target != buyer && isParticipant(target) ? target : null;
	}

	/// @return a random player who the offer can be used on right now, if any
	private @Nullable ServerPlayer getRandomTarget(ServerPlayer buyer, ShopOffer offer) {
		List<ServerPlayer> targets = getPlayerTargets(buyer).stream()
				.filter(target -> !isOnCooldown(target.getUUID(), offer))
				.toList();
		return !targets.isEmpty() ? targets.get(game.random().nextInt(targets.size())) : null;
	}

	private long getCooldownEndingTick(UUID target, ShopOffer offer) {
		Object2LongOpenHashMap<String> ends = cooldownEnds.get(target);
		return ends != null ? ends.getLong(offer.id()) : 0;
	}

	private boolean isOnCooldown(UUID target, ShopOffer offer) {
		return getCooldownEndingTick(target, offer) > game.ticks();
	}

	/// @return the participants who offers can be used on by the given player, no matter which phase of the game they are in
	private List<ServerPlayer> getPlayerTargets(ServerPlayer viewer) {
		List<ServerPlayer> targets = new ArrayList<>();
		for (ServerPlayer player : game.participants()) {
			if (player != viewer) {
				targets.add(player);
			}
		}
		return targets;
	}

	private List<GameTeam> getTeamTargets(ServerPlayer viewer) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		if (teams == null) {
			return List.of();
		}
		List<GameTeam> targets = new ArrayList<>();
		for (GameTeam team : teams) {
			if (!teams.isOnTeam(viewer, team.key())) {
				targets.add(team);
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

	private void announceUseOnTarget(ServerPlayer buyer, ActionSubjects<?> targets, ShopOffer offer) {
		Component targetName = ActionSubjectDescriber.describe(game, targets, null);
		if (targetName == null) {
			targetName = MinigameTexts.EVERYONE_RECEIVER;
		}

		Component offerName = offer.name().copy().withStyle(ChatFormatting.AQUA);
		buyer.sendSystemMessage(offer.cost() > 0 ? ShopTexts.USED_ON_WITH_COST.apply(offerName, targetName, offer.cost()) : ShopTexts.USED_ON.apply(offerName, targetName));

		PlayerSet targetPlayers = PlayerSet.copyOf(targets.asPlayers(game));

		if (offer.flavor() == ShopOfferFlavor.SABOTAGE) {
			targetPlayers.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.4f, 1.5f);
			targetPlayers.sendMessage(ShopTexts.SABOTAGED_BY.apply(buyer.getDisplayName(), offerName));
		} else {
			targetPlayers.playSound(SoundRegistry.PACKAGE_RECEIVE.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
			targetPlayers.sendMessage(ShopTexts.BONUS_FROM.apply(buyer.getDisplayName(), offerName));
		}

		Component broadcast = ShopTexts.USE_ON_BROADCAST.apply(buyer.getDisplayName(), offerName, targetName).withStyle(ChatFormatting.GRAY);
		for (ServerPlayer player : game.allPlayers()) {
			if (player != buyer && !targetPlayers.contains(player)) {
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
		if (lastShop != null && shop.balance > lastShop.balance) {
			PlayerSet.of(player).playSound(SoundRegistry.COINS.value(), SoundSource.PLAYERS, 1.0f, game.random().nextFloat() * 0.4f + 1.3f);
		}

		long now = game.ticks();
		List<ShopClientState.Offer> clientOffers = IntStream.range(0, shop.offers.size())
				.mapToObj(i -> {
					Map<UUID, Integer> ticksLeft = new HashMap<>();
					shop.cooldownEnds.get(i).forEach((target, end) -> ticksLeft.put(target, (int) (end - now)));
					return shop.offers.get(i).asClientOffer(shop.remainingStock.get(i), ticksLeft);
				})
				.toList();
		boolean anyHasCost = shop.offers.stream().anyMatch(offer -> offer.cost() > 0);

		GameClientState.sendToPlayer(new ShopClientState(anyHasCost ? Optional.of(shop.balance) : Optional.empty(), clientOffers, shop.playerTargets, shop.teamTargets), player);
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
		List<ShopClientState.Target> playerTargets = getPlayerTargets(player).stream()
				.map(target -> new ShopClientState.Target(target.getUUID(), target.getDisplayName(), getPoints(target)))
				.sorted(Comparator.comparingInt(ShopClientState.Target::points).reversed())
				.toList();
		List<ShopClientState.Target> teamTargets = getTeamTargets(player).stream()
				.map(target -> new ShopClientState.Target(target.uuid(), target.styledName(), 0))
				.sorted(Comparator.comparingInt(ShopClientState.Target::points).reversed())
				.toList();
		List<Optional<Integer>> remainingStock = offers.stream().map(offer -> getRemainingStock(player, offer)).toList();
		List<Map<UUID, Long>> cooldownEnds = offers.stream()
				.map(offer -> getActiveCooldownEnds(offer, offer.targetTeam() ? teamTargets : playerTargets))
				.toList();
		return new SentShop(getBalance(player), offers, remainingStock, cooldownEnds, playerTargets, teamTargets);
	}

	/// The ends of the cooldowns rather than the time left, so that the shop only changes when a cooldown starts or ends
	private Map<UUID, Long> getActiveCooldownEnds(ShopOffer offer, List<ShopClientState.Target> targets) {
		if (offer.targetCooldownTicks().isEmpty()) {
			return Map.of();
		}
		Map<UUID, Long> ends = new HashMap<>();
		for (ShopClientState.Target target : targets) {
			long end = getCooldownEndingTick(target.id(), offer);
			if (end > game.ticks()) {
				ends.put(target.id(), end);
			}
		}
		return ends;
	}

	/// @return how many more times the player can buy the offer
	private Optional<Integer> getRemainingStock(ServerPlayer player, ShopOffer offer) {
		return offer.stock().map(stock -> {
			Object2IntOpenHashMap<String> bought = purchases.get(player.getUUID());
			return Math.max(stock - (bought != null ? bought.getInt(offer.id()) : 0), 0);
		});
	}

	private boolean isSoldOut(ServerPlayer player, ShopOffer offer) {
		return getRemainingStock(player, offer).map(remaining -> remaining <= 0).orElse(false);
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

	/// @param remainingStock for each offer, how many more times the player can buy it
	/// @param cooldownEnds   for each offer, the game tick from which it can be used on each player or team that is on cooldown
	private record SentShop(
			int balance,
			List<ShopOffer> offers,
			List<Optional<Integer>> remainingStock,
			List<Map<UUID, Long>> cooldownEnds,
			List<ShopClientState.Target> playerTargets,
			List<ShopClientState.Target> teamTargets
	) {
	}

	/// @param subjects what the offer's actions are applied to
	/// @param id       the player or team the offer is used on, as shown in the shop, which its cooldown applies to - `null` for offers used on oneself or globally
	private record ResolvedTarget(ActionSubjects<?> subjects, @Nullable UUID id) {
		static ResolvedTarget of(ServerPlayer player) {
			return new ResolvedTarget(ActionSubjects.ofPlayer(player), player.getUUID());
		}
	}
}
