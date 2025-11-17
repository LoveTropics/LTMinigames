package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.command.argument.GameConfigArgument;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.PendingSubPhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.command.GameCommandRegistrar;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

public final class MinigameCompetitionBehavior implements IGameBehavior {
	public static final MapCodec<MinigameCompetitionBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.nonEmptyList(ResourceLocation.CODEC.listOf()).fieldOf("games").forGetter(b -> b.gameIds),
			Codec.BOOL.optionalFieldOf("shuffle", false).forGetter(b -> b.shuffle)
	).apply(i, MinigameCompetitionBehavior::new));

	private final List<ResourceLocation> gameIds;
	private final boolean shuffle;

	private final Deque<GameConfig> gameQueue = new ArrayDeque<>();

	@Nullable
	private IGamePhase currentGame;
	@Nullable
	private PendingSubPhase pendingGame;

	public MinigameCompetitionBehavior(List<ResourceLocation> gameIds, boolean shuffle) {
		this.gameIds = gameIds;
		this.shuffle = shuffle;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		List<GameConfig> gameConfigs = new ArrayList<>(gameIds.size());
		for (ResourceLocation configId : gameIds) {
			GameConfig config = GameConfigs.REGISTRY.get(configId);
			if (config == null) {
				throw new GameException(Component.literal("Missing minigame config with id: " + configId));
			}
			gameConfigs.add(config);
		}
		if (shuffle) {
			Util.shuffle(gameConfigs, game.random());
		}
		gameQueue.addAll(gameConfigs);

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
				registerGlobalCommands(game, commands)
		);

		queueNextGame(game);

		events.listen(GamePhaseEvents.START, initiator -> {
			if (currentGame != null) {
				game.transferPlayersTo(game.allPlayers(), currentGame);
			} else if (pendingGame != null) {
				pendingGame.queuePlayers(game.allPlayers());
			}
		});

		events.listen(GamePlayerEvents.JOIN, player -> {
			if (currentGame != null) {
				game.transferPlayerTo(player, currentGame);
			} else if (pendingGame != null) {
				pendingGame.queuePlayer(player);
			}
		});
	}

	private void registerGlobalCommands(IGamePhase topGame, GameCommandRegistrar commands) {
		commands.register(Commands.literal("queue")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(GameConfigArgument.argument("game").executes(context -> {
					GameConfig config = GameConfigArgument.get(context, "game");
					gameQueue.add(config);
					context.getSource().sendSuccess(() -> Component.literal("Added " + config.id() + " to queue"), false);
					if (currentGame == null && pendingGame == null) {
						queueNextGame(topGame);
					}
					return 1;
				}))
		);
	}

	private void queueNextGame(IGamePhase topGame) {
		IGamePhase lastGame = currentGame;
		currentGame = null;
		pendingGame = null;

		GameConfig nextConfig = gameQueue.poll();
		if (nextConfig == null) {
			if (lastGame != null) {
				lastGame.returnToParent(lastGame.allPlayers());
			}
			return;
		}

		pendingGame = topGame.createSubPhase(nextConfig);
		pendingGame.queuePlayers(topGame.allPlayers());
		if (lastGame != null) {
			pendingGame.queuePlayers(lastGame.allPlayers());
		}

		pendingGame.whenCreated((subGame, subEvents) -> {
			pendingGame = null;
			currentGame = subGame;
			subEvents.listen(GamePhaseEvents.STOP, reason ->
					queueNextGame(topGame)
			);
			subEvents.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
					registerGlobalCommands(topGame, commands)
			);
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.MINIGAME_COMPETITION;
	}
}
