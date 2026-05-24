package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.command.argument.GameConfigArgument;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.PendingSubPhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.command.GameCommandRegistrar;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.Overlords;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.role.StreamHosts;
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
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Unit;
import net.minecraft.util.context.ContextMap;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

public final class MinigameCompetitionBehavior implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<MinigameCompetitionBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.nonEmptyList(QueueEntry.CODEC.listOf()).fieldOf("queue").forGetter(b -> b.initialQueue),
			StatisticKey.CODEC.listOf().optionalFieldOf("share_statistics", List.of()).forGetter(b -> b.shareStatistics),
			GameActionList.CODEC.optionalFieldOf("return_to_lobby_actions", GameActionList.EMPTY).forGetter(b -> b.returnToLobbyActions)
	).apply(i, MinigameCompetitionBehavior::new));

	private final List<QueueEntry> initialQueue;
	private final List<StatisticKey<?>> shareStatistics;
	private final GameActionList returnToLobbyActions;

	private final SubGameManager subGames = new SubGameManager();
	private boolean participantsLocked;

	public MinigameCompetitionBehavior(List<QueueEntry> initialQueue, List<StatisticKey<?>> shareStatistics, GameActionList returnToLobbyActions) {
		this.initialQueue = initialQueue;
		this.shareStatistics = shareStatistics;
		this.returnToLobbyActions = returnToLobbyActions;
	}

	@Override
	public void register(IGamePhase topGame, EventRegistrar events) throws GameException {
		returnToLobbyActions.register(topGame, events);

		subGames.queueAll(initialQueue);

		int maxParticipants = topGame.definition().getMaximumParticipantCount();
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
		if (reason.isFinished()) {
			topGame.statistics().copyFrom(subGame.statistics(), shareStatistics);
		}
		Tag statisticsTag = GameStatistics.CODEC.encodeStart(NbtOps.INSTANCE, topGame.statistics()).result().orElse(null);
		LOGGER.debug("Stopped {} in minigame competition. New statistics: {}", subGame.definition().name().getString(), statisticsTag);
	}

	private void onReturnToLobby(IGamePhase topGame) {
		returnToLobbyActions.apply(topGame, ContextMap.EMPTY, ActionSubjects.EMPTY);
	}

	private void registerGlobalCommands(IGamePhase topGame, IGamePhase game, GameCommandRegistrar commands) {
		Overlords overlords = Overlords.get(topGame);
		commands.register(Commands.literal("competition")
				.requires(source -> {
					if (source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
						return true;
					}
					ServerPlayer player = source.getPlayer();
					return player != null && overlords.contains(player);
				})
				.then(Commands.literal("start").executes(context -> {
					if (subGames.isPlaying()) {
						context.getSource().sendFailure(Component.literal("Already playing games!"));
						return 1;
					}
					subGames.startNextGame(topGame);
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
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.MINIGAME_COMPETITION;
	}

	private class SubGameManager {
		private final Deque<QueueEntry> queue = new ArrayDeque<>();

		@Nullable
		private IGamePhase currentGame;
		@Nullable
		private PendingSubPhase pendingGame;

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
			if (currentGame != null) {
				topGame.transferPlayerTo(player, currentGame);
			} else if (pendingGame != null) {
				pendingGame.queuePlayer(player);
			}
		}

		public void startNextGame(IGamePhase topGame) {
			IGamePhase lastGame = currentGame;
			currentGame = null;
			pendingGame = null;

			QueueEntry nextEntry = queue.poll();
			IGameDefinition nextGameConfig = nextEntry != null ? nextEntry.resolveGame() : null;
			if (nextGameConfig == null) {
				if (lastGame != null) {
					lastGame.returnToParent(lastGame.allPlayers());
					onReturnToLobby(topGame);
				}
				return;
			}

			pendingGame = topGame.createSubPhase(nextGameConfig);
			pendingGame.queuePlayers(topGame.allPlayers());
			if (lastGame != null) {
				pendingGame.queuePlayers(lastGame.allPlayers());
			}

			pendingGame.whenCreated((subGame, subEvents) -> {
				pendingGame = null;
				currentGame = subGame;
				subEvents.listen(GamePhaseEvents.STOP, reason -> {
							startNextGame(topGame);
							onStopSubGame(topGame, subGame, reason);
				});
				onCreateSubGame(topGame, subGame, subEvents);
			});
			pendingGame.whenErrored(exception -> {
				pendingGame = null;
				currentGame = null;
				topGame.allPlayers().sendMessage(Component.literal("An error occurred starting the last minigame"));
				queueFirst(new Lobby());
				startNextGame(topGame);
			});
		}

		@Nullable
		public IGameDefinition cancelCurrentGame() {
			IGamePhase currentGame = this.currentGame;
			if (currentGame != null) {
				currentGame.requestStop(GameStopReason.canceled());
				return currentGame.definition();
			}
			return null;
		}

		@Nullable
		public IGameDefinition restartCurrentGame() {
			IGamePhase currentGame = this.currentGame;
			if (currentGame != null) {
				// Note: because we pass by id, if we /reload this will fetch the new instance
				queueFirst(new Game(currentGame.definition().id()));
				currentGame.requestStop(GameStopReason.canceled());
				return currentGame.definition();
			}
			return null;
		}

		public boolean backToLobby() {
			IGamePhase currentGame = this.currentGame;
			if (currentGame != null) {
				queueFirst(new Game(currentGame.definition().id()));
				queueFirst(new Lobby());
				currentGame.requestStop(GameStopReason.canceled());
				return true;
			}
			return false;
		}

		public boolean isPlaying() {
			return currentGame != null || pendingGame != null;
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

		@Nullable
		IGameDefinition resolveGame();

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
}
