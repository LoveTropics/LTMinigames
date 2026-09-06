package com.lovetropics.minigames.common.content.river_race.microgames;

import com.lovetropics.minigames.common.core.command.argument.GameConfigArgument;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.PendingSubPhase;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.command.GameCommandRegistrar;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

public class MicrogamesManager implements IGameState {
	public static final GameStateKey<MicrogamesManager> KEY = GameStateKey.create("Microgames");

	private final IGamePhase topGame;
	private final Queue<IGameDefinition> gameQueue = new ArrayDeque<>();

	private @Nullable PendingSubPhase pendingMicrogame;
	private @Nullable IGamePhase microgame;

	public MicrogamesManager(IGamePhase topGame) {
		this.topGame = topGame;
	}

	public static MicrogamesManager get(IGamePhase topGame) {
		return topGame.instanceState().getOrThrow(KEY);
	}

	public void clearQueue() {
		gameQueue.clear();
	}

	public void addToQueue(Collection<? extends IGameDefinition> games) {
		gameQueue.addAll(games);
	}

	public boolean startQueueIfStopped() {
		if (microgame != null || pendingMicrogame != null) {
			return false;
		}
		IGameDefinition definition = gameQueue.poll();
		if (definition != null) {
			startMicrogame(definition);
			return true;
		}
		return false;
	}

	public void register(EventRegistrar events) {
		events.listen(GamePlayerEvents.JOIN, player -> {
			if (microgame != null) {
				topGame.transferPlayerTo(player, microgame);
			} else if (pendingMicrogame != null) {
				pendingMicrogame.queuePlayer(player);
			}
		});

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
				registerGlobalCommands(commands)
		);
	}

	private void registerGlobalCommands(GameCommandRegistrar commands) {
		commands.register(Commands.literal("microgame")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("queue")
						.then(GameConfigArgument.argument("game").executes(context -> {
							GameConfig config = GameConfigArgument.get(context, "game");
							gameQueue.add(config);
							context.getSource().sendSuccess(() -> Component.literal("Added " + config.id() + " to microgame queue"), true);
							return 1;
						}))
				)
				.then(Commands.literal("clearQueue")
						.executes(context -> {
							gameQueue.clear();
							return 1;
						})
				)
				.then(Commands.literal("start")
						.executes(context -> {
							if (pendingMicrogame != null || microgame != null) {
								context.getSource().sendFailure(Component.literal("A microgame is already in progress"));
								return 1;
							}
							IGameDefinition config = gameQueue.poll();
							if (config == null) {
								context.getSource().sendFailure(Component.literal("The microgame queue is empty"));
								return 1;
							}
							context.getSource().sendSuccess(() -> Component.literal("Starting next microgame"), true);
							startMicrogame(config);
							return 1;
						})
				)
				.then(Commands.literal("cancelAll")
						.executes(context -> {
							gameQueue.clear();
							if (microgame != null) {
								microgame.requestStop(GameStopReason.canceled());
							}
							return 1;
						})
				)
		);
	}

	private void moveToNextInQueue() {
		IGameDefinition definition = gameQueue.poll();
		if (definition != null) {
			startMicrogame(definition);
		} else if (microgame != null) {
			microgame.returnToParent(microgame.allPlayers());
			topGame.invoker(MicrogameEvents.MICROGAMES_ENDED).onMicrogamesEnded();
			microgame = null;
		}
	}

	private void startMicrogame(IGameDefinition definition) {
		pendingMicrogame = topGame.createSubPhase(definition);
		pendingMicrogame.queuePlayers(topGame.allPlayers());

		if (microgame != null) {
			pendingMicrogame.queuePlayers(microgame.allPlayers());
			microgame.requestStop(GameStopReason.canceled());
		}

		pendingMicrogame.whenCreated((subGame, subEvents) -> {
			microgame = subGame;
			pendingMicrogame = null;
			subEvents.listen(GamePhaseEvents.STOP, reason ->
					moveToNextInQueue()
			);
			subEvents.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
					registerGlobalCommands(commands)
			);
			topGame.invoker(MicrogameEvents.CREATE_MICROGAME).onCreateMicrogame(subGame, subEvents);
		});
		pendingMicrogame.whenErrored(exception -> {
			pendingMicrogame = null;
			microgame = null;
			topGame.allPlayers().sendMessage(Component.literal("An error occurred while starting the last microgame"));
			moveToNextInQueue();
		});
	}
}
