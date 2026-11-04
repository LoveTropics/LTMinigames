package org.lovetropics.games.common.content.build_battle;

import com.google.common.collect.Iterables;
import com.lovetropics.lib.BlockBox;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import org.lovetropics.games.common.core.game.command.GameCommandRegistrar;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.game.state.Overlords;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
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
import java.util.Set;
import java.util.UUID;

public final class BuildBattleBehavior implements IGameBehavior {
	private static final long REVIEW_DELAY_TICKS = 10 * SharedConstants.TICKS_PER_SECOND;

	public static final MapCodec<BuildBattleBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("plots_name").forGetter(c -> c.plotRegionsName),
			Codec.LONG.fieldOf("build_time").orElse((long) (5 * 60 * SharedConstants.TICKS_PER_SECOND)).forGetter(c -> c.buildEndTime)
	).apply(i, BuildBattleBehavior::new));

	private static final Logger LOGGER = LogUtils.getLogger();

	private final RegionQuery plotRegionsName;
	private long buildEndTime;

	private final HashMap<UUID, Region> playerPlots = new HashMap<>();
	private final Deque<UUID> plotsToReview = new ArrayDeque<>();
	private final List<UUID> reviewedPlayers = new ArrayList<>();
	private final HashMap<UUID, Integer> overlordPoints = new HashMap<>();
	private final Map<UUID, PlayerKey> playerKeys = new HashMap<>();
	private @Nullable PlayerKey winningPlayer;

	private long currentTime;
	private boolean paused;

	private int revieweeIndex = -1;
	private Overlords overlords;
	private SelectorItems<Integer> selectorItems;

	private GameBossBar bar;
	private boolean building = true;

	public BuildBattleBehavior(RegionQuery plotRegionsName, long buildEndTime) {
		this.plotRegionsName = plotRegionsName;
		this.buildEndTime = buildEndTime;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		selectorItems = new SelectorItems<>(new VoteItemsHandlers(this), List.of(1, 2, 3, 4, 5, 6));

		GameWidgets widgets = GameWidgets.getOrRegister(game, events);

		events.listen(GamePhaseEvents.START, initiator -> {
			bar = widgets.openBossBar(Component.empty(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
			game.allPlayers().forEach(bar::addPlayer);
			building = true;
			refreshBuildingTimeBar(0);
			overlords = Overlords.get(game);
			game.allPlayers().showTitle(BuildBattleTexts.BUILDING_START, BuildBattleTexts.BUILDING_START_SUBTITLE, 5, SharedConstants.TICKS_PER_SECOND * 2, 5);
		});
		events.listen(GamePhaseEvents.DESTROY, () -> {
			bar.close();
		});
		events.listen(GamePhaseEvents.TICK, () -> {
			if (currentTime <= buildEndTime) {
				refreshBuildingTimeBar(currentTime);
			}
			if (currentTime == buildEndTime) {
				game.allPlayers().showTitle(BuildBattleTexts.BUILDING_END, 5, SharedConstants.TICKS_PER_SECOND * 2, 5);
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
				game.allPlayers().showTitle(BuildBattleTexts.REVIEW_TIME, BuildBattleTexts.REVIEW_TIME_SUBTITLE, 5, SharedConstants.TICKS_PER_SECOND * 2, 5);
			}
			if (currentTime == buildEndTime + REVIEW_DELAY_TICKS) {
				nextReviewee(game, widgets);
			}

			game.participants().forEach(player -> {
				if (!playerPlots.containsKey(player.getUUID())) {
					return;
				}
				Region plot = getCurrentPlotToShow(player.getUUID());
				if (plot == null) {
					LOGGER.error("Player {} has no plot assigned!", reviewedPlayers.get(revieweeIndex));
					return;
				}
				if (!plot.contains(player)) {
					SpawnBuilder spawn = new SpawnBuilder(player);
					game.invoker(GamePlayerEvents.SPAWN).onSpawn(player.getUUID(), spawn, null);
					spawn.teleportAndApply(player);
				}
			});

			if (!paused) {
				currentTime++;
			}
		});

		events.listen(GameLogicEvents.GAME_OVER, winner ->
				winningPlayer = Iterables.getFirst(winner.asPlayerKeys(game), null)
		);

		events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (participants, spectators) -> assignPlots(participants, plotRegionsName.all(game)));
		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> spawnPlayer(playerId, spawn));

		// players cannot interact with blocks outside their plot
		events.listen(GamePlayerEvents.BREAK_BLOCK, (player, pos, state, hand) -> canInteract(player.getUUID(), player.level(), pos) ? TriState.DEFAULT : TriState.FALSE);
		events.listen(GamePlayerEvents.PLACE_BLOCK, (player, pos, placed, placedOn, placedItemStack) -> canInteract(player.getUUID(), player.level(), pos) ? TriState.DEFAULT : TriState.FALSE);
		events.listen(GamePlayerEvents.USE_BLOCK, (player, level, pos, hand, result) -> canInteract(player.getUUID(), level, pos) ? InteractionResult.PASS : InteractionResult.FAIL);
		events.listen(GamePlayerEvents.USE_ITEM, (player, hand) -> onUseItem(game, player, hand, widgets));

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

	private @Nullable Region getCurrentPlotToShow(UUID playerId) {
		if (winningPlayer != null) {
			return playerPlots.get(winningPlayer.id());
		} else if (revieweeIndex > -1) {
			return playerPlots.get(reviewedPlayers.get(revieweeIndex));
		} else {
			return playerPlots.get(playerId);
		}
	}

	public void assignPlots(Set<PlayerKey> players, Collection<Region> plots) {
		if (plots.size() < players.size()) {
			throw new GameException(Component.literal("Not enough plots for all players in Build Battle game (" + plots.size() + " plots named \"" + plotRegionsName + "\" for " + players.size() + " players)"));
		}
		Iterator<PlayerKey> iterator = players.iterator();
		for (Region plot : plots) {
			if (!iterator.hasNext()) {
				break;
			}
			PlayerKey player = iterator.next();
			playerPlots.put(player.id(), plot);
			playerKeys.put(player.id(), player);
		}
	}

	public boolean canInteract(UUID playerId, ServerLevel level, BlockPos pos) {
		return building && playerPlots.containsKey(playerId) && playerPlots.get(playerId).contains(level, pos);
	}

	private void spawnPlayer(UUID playerId, SpawnBuilder builder) {
		Region plot = getCurrentPlotToShow(playerId);
		if (plot == null) {
			return;
		}
		ServerLevel level = plot.level();
		builder.teleportTo(level, tryFindEmptyPos(level, level.getRandom(), plot.box()));
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
			plotsToReview.addAll(playerPlots.keySet());
			UUID nextPlot = plotsToReview.poll();
			if (nextPlot != null) {
				reviewedPlayers.add(nextPlot);
				revieweeIndex = 0;
				refreshReviewee(game, widgets);
			}
			return;
		}

		UUID currentRevieweeId = reviewedPlayers.get(revieweeIndex);
		revieweeIndex++;

		if (revieweeIndex < reviewedPlayers.size()) {
			// Just moving back to a reviewee we saw before
			refreshReviewee(game, widgets);
			return;
		}

		// Assign score and move to next reviewee
		assignPointsForReviewee(game, currentRevieweeId);

		UUID nextPlot = plotsToReview.poll();
		if (nextPlot != null) {
			reviewedPlayers.add(nextPlot);
			refreshReviewee(game, widgets);
		} else {
			// No more plots to review, trigger win!
			revieweeIndex = -2;
			announceWinner(game);
		}
	}

	private void assignPointsForReviewee(IGamePhase game, UUID revieweeId) {
		int points = 0;
		for (Map.Entry<UUID, Integer> overlordPoints : overlordPoints.entrySet()) {
			points += overlordPoints.getValue();
		}
		PlayerKey playerKey = playerKeys.get(revieweeId);
		game.statistics().forPlayer(playerKey).incrementInt(StatisticKey.POINTS, points);
		overlordPoints.clear();
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
		for (ServerPlayer player : game.allPlayers()) {
			SpawnBuilder spawn = new SpawnBuilder(player);
			game.invoker(GamePlayerEvents.SPAWN).onSpawn(player.getUUID(), spawn, null);
			spawn.teleportAndApply(player);
		}
		for (ServerPlayer overlord : overlords) {
			overlord.getInventory().clearContent();
			ItemStack previous = new ItemStack(Items.SPONGE);
			previous.set(DataComponents.ITEM_NAME, BuildBattleTexts.ITEM_PREVIOUS);
			overlord.addItem(previous);
			if (revieweeIndex == reviewedPlayers.size() - 1) {
				selectorItems.giveSelectorsTo(overlord);
			}
			ItemStack next = new ItemStack(Items.GOLD_BLOCK);
			next.set(DataComponents.ITEM_NAME, BuildBattleTexts.ITEM_NEXT);
			overlord.addItem(next);
		}

		//TODO put participants in spectator

		String playerName = "unknown player";
		ServerPlayer player = game.allPlayers().getPlayerBy(reviewedPlayers.get(revieweeIndex));
		if (player != null) {
			playerName = player.getScoreboardName();
		}

		bar.close();
		bar = widgets.openBossBar(BuildBattleTexts.BAR_REVIEWING.apply(playerName).withStyle(ChatFormatting.YELLOW), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
		bar.setProgress(revieweeIndex / (float) playerPlots.size());
		game.allPlayers().forEach(bar::addPlayer);
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

	private record VoteItemsHandlers(BuildBattleBehavior behavior) implements SelectorItems.Handlers<Integer> {
		@Override
		public void onPlayerSelected(ServerPlayer player, Integer value) {
			if (behavior.overlords.contains(player.getUUID())) {
				behavior.overlordPoints.put(player.getUUID(), value);
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
