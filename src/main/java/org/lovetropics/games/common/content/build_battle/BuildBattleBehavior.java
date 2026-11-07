package org.lovetropics.games.common.content.build_battle;

import com.google.common.collect.Multimap;
import com.lovetropics.lib.BlockBox;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.GameWinner;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.SpawnBuilder;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameLogicEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameTeamEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import org.lovetropics.games.common.core.game.command.GameCommandRegistrar;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.game.state.Overlords;
import org.lovetropics.games.common.core.game.state.statistics.GameStatistics;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;
import org.lovetropics.games.common.core.game.util.GameBossBar;
import org.lovetropics.games.common.core.game.util.GameWidgets;
import org.lovetropics.games.common.core.game.util.SelectorItems;
import org.lovetropics.games.common.util.Util;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class BuildBattleBehavior implements IGameBehavior {
	private static final long REVIEW_DELAY_TICKS = 10 * SharedConstants.TICKS_PER_SECOND;

	public static final MapCodec<BuildBattleBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("plots_name").forGetter(c -> c.plotRegionsName),
			Codec.LONG.fieldOf("build_time").orElse((long) (5 * 60 * SharedConstants.TICKS_PER_SECOND)).forGetter(c -> c.buildEndTime),
			ComponentSerialization.CODEC.fieldOf("theme").forGetter(c -> c.theme)
	).apply(i, BuildBattleBehavior::new));

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final int TITLE_DURATION = SharedConstants.TICKS_PER_SECOND * 3;

	private final RegionQuery plotRegionsName;
	private long buildEndTime;
	private final Component theme;

	private final List<Plot> plots = new ArrayList<>();
	private final Map<UUID, Plot> plotsByPlayer = new HashMap<>();

	private final Deque<Plot> plotsToReview = new ArrayDeque<>();
	private final List<Plot> reviewedPlots = new ArrayList<>();

	private @Nullable Plot winningPlot;

	private long currentTime;
	private boolean paused;

	private int revieweeIndex = -1;
	private Overlords overlords;
	private SelectorItems<Integer> selectorItems;

	private GameBossBar bar;
	private boolean building = true;

	public BuildBattleBehavior(RegionQuery plotRegionsName, long buildEndTime, Component theme) {
		this.plotRegionsName = plotRegionsName;
		this.buildEndTime = buildEndTime;
		this.theme = theme;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		selectorItems = new SelectorItems<>(new VoteItemsHandlers(this), List.of(1, 2, 3, 4, 5, 6));

		GameWidgets widgets = GameWidgets.getOrRegister(game, events);
		bar = widgets.openGlobalBossBar(Component.empty(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);

		events.listen(GamePhaseEvents.START, _ -> {
			building = true;
			refreshBuildingTimeBar(0);
			overlords = Overlords.get(game);
			Component styledTheme = ComponentUtils.mergeStyles(theme, Style.EMPTY.withColor(ChatFormatting.YELLOW));
			game.allPlayers().showTitle(BuildBattleTexts.BUILDING_START, BuildBattleTexts.BUILDING_START_SUBTITLE.apply(styledTheme), 5, TITLE_DURATION, 5);
			game.allPlayers().sendMessage(BuildBattleTexts.BUILDING_START_SUBTITLE.apply(styledTheme));
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			if (currentTime <= buildEndTime) {
				refreshBuildingTimeBar(currentTime);
			}
			if (currentTime == buildEndTime) {
				game.allPlayers().showTitle(BuildBattleTexts.BUILDING_END, 5, TITLE_DURATION, 5);
				building = false;
				bar.close();
				for (ServerPlayer player : game.allPlayers()) {
					player.getInventory().clearContent();
				}
			}
			if (currentTime == buildEndTime + REVIEW_DELAY_TICKS / 2) {
				for (ServerPlayer player : game.participants().shuffledCopy(game.random())) {
					game.setPlayerRole(player, PlayerRole.SPECTATOR);
				}
				game.allPlayers().showTitle(BuildBattleTexts.REVIEW_TIME, BuildBattleTexts.REVIEW_TIME_SUBTITLE, 5, TITLE_DURATION, 5);
			}
			if (currentTime == buildEndTime + REVIEW_DELAY_TICKS) {
				nextReviewee(game, widgets);
			}

			game.participants().forEach(player -> {
				if (!plotsByPlayer.containsKey(player.getUUID())) {
					return;
				}
				Plot plot = getCurrentPlotToShow(player.getUUID());
				if (plot == null) {
					LOGGER.error("Player {} has no plot assigned!", reviewedPlots.get(revieweeIndex));
					return;
				}
				if (!plot.region.contains(player)) {
					SpawnBuilder spawn = new SpawnBuilder(player);
					game.invoker(GamePlayerEvents.SPAWN).onSpawn(player.getUUID(), spawn, null);
					spawn.teleportAndApply(player);
				}
			});

			if (!paused) {
				currentTime++;
			}
		});

		events.listen(GameLogicEvents.GAME_OVER, winner -> {
			List<Plot> plots = winner.asPlayerKeys(game).stream()
					.map(player -> plotsByPlayer.get(player.id()))
					.filter(Objects::nonNull)
					.distinct()
					.toList();
			if (plots.isEmpty()) {
				LOGGER.warn("Found no plots for winner {}", winner);
				return;
			} else if (plots.size() > 1) {
				LOGGER.warn("Found multiple plots for winner {}: {}", winner, plots.stream().map(plot -> plot.getDisplayName(game).getString()).toList());
			}
			winningPlot = plots.getFirst();
		});

		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		if (teams != null) {
			events.listen(GameTeamEvents.TEAMS_ALLOCATED, (_, teamParticipants) ->
					assignTeamPlots(teams, teamParticipants, plotRegionsName.all(game))
			);
		} else {
			events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (participants, spectators) -> assignIndividualPlots(participants, plotRegionsName.all(game)));
		}

		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> spawnPlayer(playerId, spawn));

		// players cannot interact with blocks outside their plot
		events.listen(GamePlayerEvents.BREAK_BLOCK, (player, pos, state, hand) -> canInteract(player.getUUID(), player.level(), pos) ? TriState.DEFAULT : TriState.FALSE);
		events.listen(GamePlayerEvents.PLACE_BLOCK, (player, pos, placed, placedOn, placedItemStack) -> canInteract(player.getUUID(), player.level(), pos) ? TriState.DEFAULT : TriState.FALSE);
		events.listen(GamePlayerEvents.USE_BLOCK, (player, level, pos, hand, result) -> canInteract(player.getUUID(), level, pos) ? InteractionResult.PASS : InteractionResult.FAIL);
		events.listen(GamePlayerEvents.USE_ITEM, (player, hand) -> onUseItem(game, player, hand, widgets));

		events.listen(GameWorldEvents.EXPLOSION_DETONATE, (level, explosion, affectedBlocks, affectedEntities) ->
				affectedBlocks.removeIf(blockPos -> !plotRegionsName.contains(game, level, blockPos))
		);

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) -> {
			commands.register(Commands.literal("timer")
					.requires(GameCommandRegistrar.adminOrOverlord(game))
					.then(Commands.literal("add")
							.then(Commands.argument("time", TimeArgument.time(0))
									.executes(context -> {
										if (currentTime < buildEndTime) {
											int ticks = IntegerArgumentType.getInteger(context, "time");
											buildEndTime += ticks;
											context.getSource().sendSuccess(() -> Component.literal("Added " + (ticks / SharedConstants.TICKS_PER_SECOND) + " seconds"), false);
										} else {
											context.getSource().sendFailure(Component.literal("Build time already ended"));
										}
										return 1;
									})
							)
					)
					.then(Commands.literal("pause")
							.executes(context -> {
								paused = true;
								context.getSource().sendSuccess(() -> Component.literal("Paused build timer"), false);
								return 1;
							})
					)
					.then(Commands.literal("unpause")
							.executes(context -> {
								paused = false;
								context.getSource().sendSuccess(() -> Component.literal("Unpaused build timer"), false);
								return 1;
							})
					)
			);
		});

		selectorItems.applyTo(events);
	}

	private @Nullable Plot getCurrentPlotToShow(UUID playerId) {
		if (winningPlot != null) {
			return winningPlot;
		}
		Plot reviewingPlot = getReviewingPlot();
		if (reviewingPlot != null) {
			return reviewingPlot;
		} else {
			return plotsByPlayer.get(playerId);
		}
	}

	private @Nullable Plot getReviewingPlot() {
		if (revieweeIndex > -1) {
			return reviewedPlots.get(revieweeIndex);
		}
		return null;
	}

	private void assignTeamPlots(TeamState teams, Multimap<GameTeamKey, PlayerKey> teamParticipants, List<Region> plots) {
		Set<GameTeamKey> teamKeys = teamParticipants.keySet();
		if (plots.size() < teamKeys.size()) {
			throw new GameException(Component.literal("Not enough plots for all teams in Build Battle game (" + plots.size() + " plots named \"" + plotRegionsName + "\" for " + teams.size() + " teams)"));
		}
		Iterator<GameTeamKey> iterator = teamKeys.iterator();
		for (Region region : plots) {
			if (!iterator.hasNext()) {
				break;
			}
			GameTeamKey teamKey = iterator.next();
			GameTeam team = teams.getTeamByKey(teamKey);
			Collection<PlayerKey> players = teamParticipants.get(teamKey);
			Plot plot = new Plot(region, Set.copyOf(players), team);
			this.plots.add(plot);
			for (PlayerKey player : players) {
				plotsByPlayer.put(player.id(), plot);
			}
		}
	}

	private void assignIndividualPlots(Set<PlayerKey> players, List<Region> plots) {
		if (plots.size() < players.size()) {
			throw new GameException(Component.literal("Not enough plots for all players in Build Battle game (" + plots.size() + " plots named \"" + plotRegionsName + "\" for " + players.size() + " players)"));
		}
		Iterator<PlayerKey> iterator = players.iterator();
		for (Region region : plots) {
			if (!iterator.hasNext()) {
				break;
			}
			PlayerKey player = iterator.next();
			Plot plot = new Plot(region, Set.of(player), null);
			this.plots.add(plot);
			plotsByPlayer.put(player.id(), plot);
		}
	}

	public boolean canInteract(UUID playerId, ServerLevel level, BlockPos pos) {
		if (!building) {
			return false;
		}
		Plot plot = plotsByPlayer.get(playerId);
		return plot != null && plot.region.contains(level, pos);
	}

	private void spawnPlayer(UUID playerId, SpawnBuilder builder) {
		Plot plot = getCurrentPlotToShow(playerId);
		if (plot == null) {
			return;
		}
		ServerLevel level = plot.region.level();
		builder.teleportTo(level, tryFindEmptyPos(level, level.getRandom(), plot.region.box()));
	}

	private BlockPos tryFindEmptyPos(ServerLevel level, RandomSource random, BlockBox box) {
		for (int i = 0; i < 20; i++) {
			BlockPos pos = box.sample(random);
			if (level.isEmptyBlock(pos)) {
				return pos;
			}
		}
		LOGGER.debug("USING FALLBACK SPAWN POS");
		return box.centerBlock();
	}

	private void refreshBuildingTimeBar(long ticks) {
		long remainingTicks = buildEndTime - ticks;
		String timeRemaining = Util.formatTimer(remainingTicks / SharedConstants.TICKS_PER_SECOND);
		bar.setTitle(BuildBattleTexts.BAR_BUILDING.copy().withStyle(ChatFormatting.AQUA).append(" ").append(BuildBattleTexts.COUNTDOWN.apply(timeRemaining)));
		bar.setProgress(buildEndTime > 0 ? (float) remainingTicks / (float) buildEndTime : 0f);
	}

	private void nextReviewee(IGamePhase game, GameWidgets widgets) {
		if (revieweeIndex == -1) {
			plotsToReview.addAll(plots);
			Plot nextPlot = plotsToReview.poll();
			if (nextPlot != null) {
				reviewedPlots.add(nextPlot);
				revieweeIndex = 0;
				refreshReviewee(game, widgets);
			}
			return;
		}

		Plot currentRevieweePlot = reviewedPlots.get(revieweeIndex);
		revieweeIndex++;

		if (revieweeIndex < reviewedPlots.size()) {
			// Just moving back to a reviewee we saw before
			refreshReviewee(game, widgets);
			return;
		}

		// Assign score and move to next reviewee
		currentRevieweePlot.assignPoints(game.statistics());

		Plot nextPlot = plotsToReview.poll();
		if (nextPlot != null) {
			reviewedPlots.add(nextPlot);
			refreshReviewee(game, widgets);
		} else {
			// No more plots to review, trigger win!
			revieweeIndex = -2;
			announceWinner(game);
		}
	}

	private void previousReviewee(IGamePhase game, GameWidgets widgets) {
		if (revieweeIndex <= 0) {
			return;
		}
		revieweeIndex--;
		refreshReviewee(game, widgets);
	}

	private void announceWinner(IGamePhase game) {
		for (ServerPlayer player : game.allPlayers()) {
			player.getInventory().clearContent();
		}

		bar.close();

		if (!game.invoker(GameLogicEvents.REQUEST_GAME_OVER).requestGameOver()) {
			game.invoker(GameLogicEvents.GAME_OVER).onGameOver(new GameWinner.Nobody());
		}
	}

	private void refreshReviewee(IGamePhase game, GameWidgets widgets) {
		Plot plot = reviewedPlots.get(revieweeIndex);

		for (ServerPlayer player : game.allPlayers()) {
			SpawnBuilder spawn = new SpawnBuilder(player);
			game.invoker(GamePlayerEvents.SPAWN).onSpawn(player.getUUID(), spawn, null);
			spawn.teleportAndApply(player);
			if (overlords.contains(player)) {
				player.setGameMode(GameType.CREATIVE);
			} else if (plot.players.contains(PlayerKey.from(player))) {
				player.setGameMode(GameType.ADVENTURE);
			} else {
				player.setGameMode(GameType.SPECTATOR);
			}
		}
		for (ServerPlayer overlord : overlords) {
			overlord.getInventory().clearContent();
			ItemStack previous = new ItemStack(Items.SPONGE);
			previous.set(DataComponents.ITEM_NAME, BuildBattleTexts.ITEM_PREVIOUS);
			overlord.addItem(previous);
			if (revieweeIndex == reviewedPlots.size() - 1) {
				selectorItems.giveSelectorsTo(overlord);
			}
			ItemStack next = new ItemStack(Items.GOLD_BLOCK);
			next.set(DataComponents.ITEM_NAME, BuildBattleTexts.ITEM_NEXT);
			overlord.addItem(next);
		}

		Component displayName = plot.getDisplayName(game);

		bar.close();
		bar = widgets.openGlobalBossBar(BuildBattleTexts.BAR_REVIEWING.apply(displayName).withStyle(ChatFormatting.YELLOW), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
		bar.setProgress(revieweeIndex / (float) plotsByPlayer.size());
	}

	private InteractionResult onUseItem(IGamePhase game, ServerPlayer player, InteractionHand hand, GameWidgets widgets) {
		if (revieweeIndex < 0 || !overlords.contains(player.getUUID())) {
			return InteractionResult.PASS;
		}
		ItemStack heldStack = player.getItemInHand(hand);
		if (heldStack.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (heldStack.is(Items.SPONGE)) {
			previousReviewee(game, widgets);
			return InteractionResult.SUCCESS;
		}
		if (heldStack.is(Items.GOLD_BLOCK)) {
			nextReviewee(game, widgets);
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	private static class Plot {
		private final Region region;
		private final Set<PlayerKey> players;
		private final @Nullable GameTeam team;

		private final Object2IntMap<UUID> assignedPointsByOverlord = new Object2IntOpenHashMap<>();

		private Plot(Region region, Set<PlayerKey> players, @Nullable GameTeam team) {
			this.region = region;
			this.players = players;
			this.team = team;
		}

		public void assignPoints(GameStatistics statistics) {
			int points = countPoints();
			if (team != null) {
				statistics.forTeam(team).incrementInt(StatisticKey.POINTS, points);
			} else {
				for (PlayerKey player : players) {
					statistics.forPlayer(player).incrementInt(StatisticKey.POINTS, points);
				}
			}
		}

		private int countPoints() {
			int points = 0;
			for (Object2IntMap.Entry<UUID> entry : assignedPointsByOverlord.object2IntEntrySet()) {
				points += entry.getIntValue();
			}
			assignedPointsByOverlord.clear();
			return points;
		}

		public Component getDisplayName(IGamePhase game) {
			if (team != null) {
				return team.styledName();
			}
			return ComponentUtils.formatList(
					players,
					key -> Optional.ofNullable(game.allPlayers().getPlayerBy(key))
							.map(Player::getDisplayName)
							.orElseGet(() -> Component.literal(key.name()))
			);
		}
	}

	private record VoteItemsHandlers(BuildBattleBehavior behavior) implements SelectorItems.Handlers<Integer> {
		@Override
		public void onPlayerSelected(ServerPlayer player, Integer value) {
			Plot reviewingPlot = behavior.getReviewingPlot();
			if (behavior.overlords.contains(player.getUUID()) && reviewingPlot != null) {
				reviewingPlot.assignedPointsByOverlord.put(player.getUUID(), value.intValue());
				player.sendSystemMessage(BuildBattleTexts.GIVE_POINTS.apply(value).withStyle(ChatFormatting.GOLD), false);
			}
		}

		@Override
		public String getIdFor(Integer value) {
			return String.valueOf(value);
		}

		@Override
		public Component getNameFor(Integer value) {
			return BuildBattleTexts.ITEM_POINTS.apply(value);
		}

		@Override
		public Item getItemFor(Integer value) {
			return switch (value) {
				case 1 -> Items.WOOL.brown();
				case 2 -> Items.WOOL.red();
				case 3 -> Items.WOOL.lime();
				case 4 -> Items.WOOL.green();
				case 5 -> Items.WOOL.blue();
				case 6 -> Items.WOOL.yellow();

				default -> Items.WOOL.white();
			};
		}
	}
}
