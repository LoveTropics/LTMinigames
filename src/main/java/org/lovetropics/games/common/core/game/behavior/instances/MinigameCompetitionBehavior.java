package org.lovetropics.games.common.core.game.behavior.instances;

import com.lovetropics.lib.slideshow.SlideshowApi;
import com.lovetropics.lib.slideshow.SlideshowInstanceHandle;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Unit;
import net.minecraft.util.context.ContextMap;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.config.ConfigLT;
import org.lovetropics.games.common.content.MinigameTexts;
import org.lovetropics.games.common.core.command.argument.GameConfigArgument;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.GameStopReason;
import org.lovetropics.games.common.core.game.IGameDefinition;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.PendingSubPhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.MinigameCompetitionEvents;
import org.lovetropics.games.common.core.game.command.GameCommandRegistrar;
import org.lovetropics.games.common.core.game.config.GameConfig;
import org.lovetropics.games.common.core.game.config.GameConfigs;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.state.Overlords;
import org.lovetropics.games.common.core.game.state.statistics.GameStatistics;
import org.lovetropics.games.common.core.game.state.statistics.LeaderboardConfig;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.role.StreamHosts;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public final class MinigameCompetitionBehavior implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<MinigameCompetitionBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.nonEmptyList(QueueEntry.CODEC.listOf()).fieldOf("queue").forGetter(b -> b.initialQueue),
			StatisticKey.CODEC.listOf().optionalFieldOf("share_statistics", List.of()).forGetter(b -> b.shareStatistics),
			StatisticAccumulator.CODEC.listOf().optionalFieldOf("accumulate_statistics", List.of()).forGetter(b -> b.accumulateStatistics),
			GameActionList.CODEC.optionalFieldOf("return_to_lobby_actions", GameActionList.EMPTY).forGetter(b -> b.returnToLobbyActions),
			LeaderboardConfig.MAP_CODEC.codec().optionalFieldOf("return_to_lobby_leaderboard").forGetter(b -> b.returnToLobbyLeaderboard)
	).apply(i, MinigameCompetitionBehavior::new));

	private final List<QueueEntry> initialQueue;
	private final List<StatisticKey<?>> shareStatistics;
	private final List<StatisticAccumulator> accumulateStatistics;
	private final GameActionList returnToLobbyActions;
	private final Optional<LeaderboardConfig<?>> returnToLobbyLeaderboard;

	private @Nullable GameStatistics accumulatedStatistics;

	private final SubGameManager subGames = new SubGameManager();
	private boolean participantsLocked;

	public MinigameCompetitionBehavior(List<QueueEntry> initialQueue, List<StatisticKey<?>> shareStatistics, List<StatisticAccumulator> accumulateStatistics, GameActionList returnToLobbyActions, Optional<LeaderboardConfig<?>> returnToLobbyLeaderboard) {
		this.initialQueue = initialQueue;
		this.shareStatistics = shareStatistics;
		this.accumulateStatistics = accumulateStatistics;
		this.returnToLobbyActions = returnToLobbyActions;
		this.returnToLobbyLeaderboard = returnToLobbyLeaderboard;
	}

	@Override
	public void register(IGamePhase topGame, EventRegistrar events) throws GameException {
		returnToLobbyActions.register(topGame, events);

		subGames.queueAll(initialQueue);

		int maxParticipants = topGame.definition().maximumParticipants();
		events.listen(GamePlayerEvents.SELECT_ROLE_ON_JOIN, (player, requestedRole) -> {
			if (participantsLocked || requestedRole != PlayerRole.PARTICIPANT) {
				// Pass through to JoinLateWithRoleBehavior
				return null;
			}
			if (StreamHosts.isHost(player) || topGame.participants().size() < maxParticipants) {
				return PlayerRole.PARTICIPANT;
			}
			return PlayerRole.SPECTATOR;
		});

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
				registerGlobalCommands(topGame, topGame, commands)
		);

		events.listen(GamePhaseEvents.TICK, () -> subGames.tick(topGame));

		events.listen(GamePlayerEvents.JOIN, player ->
				subGames.onPlayerJoin(topGame, player)
		);
	}

	private void onCreateSubGame(IGamePhase topGame, IGamePhase subGame, EventRegistrar subEvents) {
		participantsLocked = true;

		subEvents.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
				registerGlobalCommands(topGame, subGame, commands)
		);

		subGame.statistics().copyFrom(topGame.statistics(), shareStatistics);
	}

	private void onStopSubGame(IGamePhase topGame, IGamePhase subGame, GameStopReason reason) {
		LOGGER.debug("Stopped {} in minigame competition due to {}", subGame.definition().name().getString(), reason);
		if (reason.isFinished()) {
			clearOverlordsFromGameStats(subGame);
			topGame.statistics().copyFrom(subGame.statistics(), shareStatistics);
			if (accumulatedStatistics == null) {
				accumulatedStatistics = new GameStatistics();
			}
			for (StatisticAccumulator accumulator : accumulateStatistics) {
				accumulator.accumulateTo(subGame.statistics(), accumulatedStatistics);
			}
			LOGGER.debug("Pending statistics: {}", encodeStatistics(accumulatedStatistics));
		}
	}

	private void onReturnToLobby(IGamePhase topGame) {
		if (accumulatedStatistics != null) {
			if (returnToLobbyLeaderboard.isPresent()) {
				topGame.allPlayers().sendMessage(MinigameTexts.COMPETITION_LOBBY_LEADERBOARD);
				returnToLobbyLeaderboard.get().buildDisplay(topGame, accumulatedStatistics, topGame.allPlayers()::sendMessage);
			}
			for (StatisticAccumulator accumulator : accumulateStatistics) {
				accumulator.addFromAccumulator(topGame.statistics(), accumulatedStatistics);
			}
			clearOverlordsFromGameStats(topGame);
			accumulatedStatistics = null;
		}
		LOGGER.debug("Returned to lobby in minigame competition. New statistics: {}", encodeStatistics(topGame.statistics()));

		returnToLobbyActions.apply(topGame, ContextMap.EMPTY, ActionSubjects.EMPTY);
		topGame.invoker(MinigameCompetitionEvents.RETURN_TO_LOBBY).onReturnToLobby();
	}

	// TODO: why do we need this, pls.
	private void clearOverlordsFromGameStats(IGamePhase game) {
		for (ServerPlayer player : Overlords.get(game)) {
			game.statistics().clearPlayer(player);
		}
	}

	private @Nullable Tag encodeStatistics(GameStatistics statistics) {
		return GameStatistics.CODEC.encodeStart(NbtOps.INSTANCE, statistics).result().orElse(null);
	}

	private void registerGlobalCommands(IGamePhase topGame, IGamePhase game, GameCommandRegistrar commands) {
		commands.register(Commands.literal("competition")
				.requires(GameCommandRegistrar.adminOrOverlord(topGame))
				.then(Commands.literal("start").executes(context -> {
					if (!subGames.start(topGame)) {
						context.getSource().sendFailure(Component.literal("Already playing games!"));
					}
					return 1;
				}))
				.then(Commands.literal("backToLobby").executes(context -> {
					if (subGames.backToLobby()) {
						context.getSource().sendSuccess(() -> Component.literal("Returning to lobby - will resume this game after!"), true);
					} else {
						context.getSource().sendFailure(Component.literal("Not playing games!"));
					}
					return 1;
				}))
				.then(Commands.literal("skipThis").executes(context -> {
					IGameDefinition currentGame = subGames.cancelCurrentGame();
					if (currentGame != null) {
						context.getSource().sendSuccess(() -> Component.translatable("Skipping %s", currentGame.name()), true);
					} else {
						context.getSource().sendFailure(Component.literal("There is no minigame currently active!"));
					}
					return 1;
				}))
				.then(Commands.literal("restartThis").executes(context -> {
					IGameDefinition currentGame = subGames.restartCurrentGame();
					if (currentGame != null) {
						context.getSource().sendSuccess(() -> Component.translatable("Restarting %s", currentGame.name()), true);
					} else {
						context.getSource().sendFailure(Component.literal("There is no minigame currently active!"));
					}
					return 1;
				}))
				.then(Commands.literal("queue")
						.then(Commands.literal("addLobby").executes(context -> {
							subGames.queueFirst(new Lobby());
							context.getSource().sendSuccess(() -> Component.literal("Will return to lobby after this game!"), true);
							return 1;
						}))
						.then(Commands.literal("addFirst")
								.then(GameConfigArgument.argument("game").executes(context ->
										addToQueue(context, GameConfigArgument.get(context, "game"), true))
								)
						)
						.then(Commands.literal("addLast")
								.then(GameConfigArgument.argument("game").executes(context ->
										addToQueue(context, GameConfigArgument.get(context, "game"), false))
								)
						)
						.then(Commands.literal("remove")
								.then(GameConfigArgument.argument("game").executes(context -> {
									GameConfig config = GameConfigArgument.get(context, "game");
									if (subGames.removeFromQueue(new Game(config.id()))) {
										context.getSource().sendSuccess(() -> Component.literal("Removed " + config.id() + " from queue"), true);
									} else {
										context.getSource().sendFailure(Component.literal(config.id() + " is not in the queue"));
									}
									return 1;
								}))
						)
						.then(Commands.literal("clear").executes(context -> {
							int queueSize = subGames.queue.size();
							subGames.queue.clear();
							context.getSource().sendSuccess(() -> Component.literal("Cleared " + queueSize + " games from the queue"), true);
							return 1;
						}))
						.then(Commands.literal("list").executes(context -> {
							context.getSource().sendSuccess(() -> Component.translatable("The following games are in the queue: %s",
									ComponentUtils.formatList(subGames.queue, QueueEntry::getName)
							), false);
							return 1;
						}))
				)
		);
	}

	private int addToQueue(CommandContext<CommandSourceStack> context, GameConfig config, boolean first) {
		if (first) {
			subGames.queueFirst(new Game(config.id()));
		} else {
			subGames.queueLast(new Game(config.id()));
		}
		context.getSource().sendSuccess(() -> Component.literal("Added " + config.id() + " to queue"), true);
		return 1;
	}

	@Override
	public MapCodec<MinigameCompetitionBehavior> codec() {
		return CODEC;
	}

	private class SubGameManager {
		private final Deque<QueueEntry> queue = new ArrayDeque<>();

		private State state = new State.Lobby();

		public void queueFirst(QueueEntry entry) {
			queue.addFirst(entry);
		}

		public void queueLast(QueueEntry entry) {
			queue.addLast(entry);
		}

		public void queueAll(Collection<QueueEntry> entries) {
			queue.addAll(entries);
		}

		public boolean removeFromQueue(QueueEntry entry) {
			return queue.remove(entry);
		}

		public void onPlayerJoin(IGamePhase topGame, ServerPlayer player) {
			switch (state) {
				case State.Game(IGamePhase currentGame) -> topGame.transferPlayerTo(player, currentGame);
				case State.Pending(PendingSubPhase pendingGame) -> pendingGame.queuePlayer(player);
				case State.Slideshow(_, SlideshowInstanceHandle slideshow) -> slideshow.addPlayer(player);
				case State.Lobby _ -> {
				}
			}
		}

		private State updateState(State newState) {
			State lastState = state;
			state = newState;
			return lastState;
		}

		private @Nullable SlideshowInstanceHandle openIntroSlideshow(IGameDefinition game) {
			if (ConfigLT.GENERAL.skipIntroSlideshows.get()) {
				return null;
			}
			Identifier slideshowId = game.introSlideshow();
			SlideshowInstanceHandle slideshow = slideshowId != null ? SlideshowApi.open(slideshowId) : null;
			if (slideshow != null) {
				slideshow.play();
			}
			return slideshow;
		}

		public boolean start(IGamePhase topGame) {
			if (!(state instanceof State.Lobby)) {
				return false;
			}

			IGameDefinition nextGameConfig = pollNextGame();
			if (nextGameConfig == null) {
				return true;
			}

			SlideshowInstanceHandle introSlideshow = openIntroSlideshow(nextGameConfig);
			if (introSlideshow != null) {
				topGame.allPlayers().forEach(introSlideshow::addPlayer);
				state = new State.Slideshow(nextGameConfig, introSlideshow);
			} else {
				startNextGame(topGame, nextGameConfig);
			}

			return true;
		}

		public void tick(IGamePhase topGame) {
			if (state instanceof State.Slideshow slideshow && slideshow.isReadyToStart()) {
				startNextGame(topGame, slideshow.nextGame);
			}
		}

		private @Nullable IGameDefinition pollNextGame() {
			QueueEntry nextEntry = queue.poll();
			return nextEntry != null ? nextEntry.resolveGame() : null;
		}

		private void startNextGame(IGamePhase topGame, IGameDefinition nextGameConfig) {
			PendingSubPhase pendingGame = topGame.createSubPhase(nextGameConfig);
			pendingGame.queuePlayers(topGame.allPlayers());

			State lastState = updateState(new State.Pending(pendingGame));
			if (lastState instanceof State.Game(IGamePhase lastGame)) {
				pendingGame.queuePlayers(lastGame.allPlayers());
			}

			pendingGame.whenCreated((subGame, subEvents) -> {
				state = new State.Game(subGame);
				subEvents.listen(GamePhaseEvents.STOP, reason -> {
					// Make sure statistics are copied back before we start the next game and potentially lose that data
					onStopSubGame(topGame, subGame, reason);
					continueToNextSubGame(topGame);
				});
				onCreateSubGame(topGame, subGame, subEvents);
			});
			pendingGame.whenErrored(_ -> {
				// Players are already transferred to the top phase by this point, we just need to match that state
				state = new State.Lobby();
				topGame.allPlayers().sendMessage(Component.literal("An error occurred starting the last minigame"));
				onReturnToLobby(topGame);
			});
		}

		private void continueToNextSubGame(IGamePhase topGame) {
			IGameDefinition nextGameConfig = pollNextGame();
			if (nextGameConfig == null) {
				State lastState = updateState(new State.Lobby());
				if (lastState instanceof State.Game(IGamePhase lastGame)) {
					lastGame.returnToParent(lastGame.allPlayers());
				}
				onReturnToLobby(topGame);
				return;
			}

			// It's only really safe to show a slideshow when transferring from the lobby - skip it :)
			startNextGame(topGame, nextGameConfig);
		}

		public @Nullable IGameDefinition cancelCurrentGame() {
			if (state instanceof State.Game(IGamePhase currentGame)) {
				currentGame.requestStop(GameStopReason.canceled());
				return currentGame.definition();
			}
			return null;
		}

		public @Nullable IGameDefinition restartCurrentGame() {
			if (state instanceof State.Game(IGamePhase currentGame)) {
				// Note: because we pass by id, if we /reload this will fetch the new instance
				queueFirst(new Game(currentGame.definition().id()));
				currentGame.requestStop(GameStopReason.canceled());
				return currentGame.definition();
			}
			return null;
		}

		public boolean backToLobby() {
			if (state instanceof State.Game(IGamePhase currentGame)) {
				queueFirst(new Game(currentGame.definition().id()));
				queueFirst(new Lobby());
				currentGame.requestStop(GameStopReason.canceled());
				return true;
			}
			return false;
		}

		private sealed interface State {
			record Lobby() implements State {
			}

			record Slideshow(
					IGameDefinition nextGame,
					SlideshowInstanceHandle slideshow
			) implements State {
				private static final double SLIDESHOW_BUFFER_SECONDS = 0.5;

				public boolean isReadyToStart() {
					return slideshow.currentTime() >= slideshow.totalTime() - SLIDESHOW_BUFFER_SECONDS;
				}
			}

			record Pending(
					PendingSubPhase pendingGame
			) implements State {
			}

			record Game(
					IGamePhase game
			) implements State {
			}
		}
	}

	public sealed interface QueueEntry {
		Codec<QueueEntry> CODEC = Codec.either(Game.CODEC, Lobby.CODEC).xmap(
				Either::unwrap,
				entry -> switch (entry) {
					case Game game -> Either.left(game);
					case Lobby lobby -> Either.right(lobby);
				}
		);

		@Nullable IGameDefinition resolveGame();

		Component getName();
	}

	public record Game(Identifier game) implements QueueEntry {
		public static final Codec<Game> CODEC = RecordCodecBuilder.create(i -> i.group(
				Identifier.CODEC.fieldOf("game").forGetter(Game::game)
		).apply(i, Game::new));

		@Override
		public @Nullable IGameDefinition resolveGame() {
			GameConfig config = GameConfigs.REGISTRY.get(game);
			if (config == null) {
				LOGGER.error("No game with id: {}, cannot queue", game);
			}
			return config;
		}

		@Override
		public Component getName() {
			return Component.literal(game.toString());
		}
	}

	public record Lobby() implements QueueEntry {
		public static final Codec<Lobby> CODEC = RecordCodecBuilder.create(i -> i.group(
				MapCodec.unit(Unit.INSTANCE).fieldOf("lobby").forGetter(lobby -> Unit.INSTANCE)
		).apply(i, unit -> new Lobby()));

		@Override
		public @Nullable IGameDefinition resolveGame() {
			return null;
		}

		@Override
		public Component getName() {
			return Component.literal("<Lobby>");
		}
	}

	public record StatisticAccumulator(
			StatisticKey<Integer> statistic,
			int returnToLobbyMinimum
	) {
		public static final Codec<StatisticAccumulator> CODEC = RecordCodecBuilder.create(i -> i.group(
				StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(StatisticAccumulator::statistic),
				Codec.INT.optionalFieldOf("return_to_lobby_minimum", 0).forGetter(StatisticAccumulator::returnToLobbyMinimum)
		).apply(i, StatisticAccumulator::new));

		public void accumulateTo(GameStatistics subStats, GameStatistics accumulatorStats) {
			accumulatorStats.accumulateFrom(subStats, List.of(statistic));
		}

		public void addFromAccumulator(GameStatistics topStats, GameStatistics accumulatorStats) {
			topStats.global().incrementInt(statistic, Math.max(accumulatorStats.global().getInt(statistic), returnToLobbyMinimum));
			for (PlayerKey player : topStats.getPlayers()) {
				topStats.forPlayer(player).incrementInt(statistic, Math.max(accumulatorStats.forPlayer(player).getInt(statistic), returnToLobbyMinimum));
			}
			for (GameTeamKey team : topStats.getTeams()) {
				topStats.forTeam(team).incrementInt(statistic, Math.max(accumulatorStats.forTeam(team).getInt(statistic), returnToLobbyMinimum));
			}
		}
	}
}
