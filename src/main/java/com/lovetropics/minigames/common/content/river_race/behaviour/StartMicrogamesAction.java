package com.lovetropics.minigames.common.content.river_race.behaviour;

import com.lovetropics.minigames.common.content.river_race.event.RiverRaceEvents;
import com.lovetropics.minigames.common.core.command.argument.GameConfigArgument;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.PendingSubPhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.command.GameCommandRegistrar;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.context.ContextMap;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public record StartMicrogamesAction(
		List<ResourceLocation> gameConfigIds,
		int gamesPerRound,
		GameActionList onComplete
) implements IGameBehavior {

	public static final MapCodec<StartMicrogamesAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.nonEmptyList(ResourceLocation.CODEC.listOf()).fieldOf("games").forGetter(StartMicrogamesAction::gameConfigIds),
			Codec.INT.optionalFieldOf("games_per_round", 1).forGetter(c -> c.gamesPerRound),
			GameActionList.CODEC.optionalFieldOf("on_complete", GameActionList.EMPTY).forGetter(StartMicrogamesAction::onComplete)
	).apply(i, StartMicrogamesAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		onComplete.register(game, events);

		List<GameConfig> gameConfigs = new ArrayList<>(gameConfigIds.size());
		for (ResourceLocation configId : gameConfigIds) {
			GameConfig config = GameConfigs.REGISTRY.get(configId);
			if (config == null) {
				throw new GameException(Component.literal("Missing microgame config with id: " + configId));
			}
			gameConfigs.add(config);
		}

		Queue<GameConfig> gameQueue = new ArrayDeque<>();
		MutableObject<IGamePhase> activeMicrogame = new MutableObject<>();

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			gameQueue.clear();
			gameQueue.addAll(pickCountRandomly(gameConfigs, gamesPerRound));
			if (activeMicrogame.getValue() == null) {
				queueNextSubGame(game, gameQueue, activeMicrogame);
			}
			return true;
		});

		events.listen(GamePlayerEvents.JOIN, player -> {
			IGamePhase microgame = activeMicrogame.getValue();
			if (microgame != null) {
				game.transferPlayerTo(player, microgame);
			}
		});

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
				registerGlobalCommands(game, commands, gameQueue, activeMicrogame)
		);
	}

	private void registerGlobalCommands(IGamePhase topGame, GameCommandRegistrar commands, Queue<GameConfig> gameQueue, MutableObject<IGamePhase> activeMicrogame) {
		commands.register(Commands.literal("queue")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(GameConfigArgument.argument("game").executes(context -> {
					GameConfig config = GameConfigArgument.get(context, "game");
					gameQueue.add(config);
					context.getSource().sendSuccess(() -> Component.literal("Added " + config.id() + " to queue"), false);
					if (activeMicrogame.getValue() == null) {
						queueNextSubGame(topGame, gameQueue, activeMicrogame);
					}
					return 1;
				}))
		);
	}

	private void queueNextSubGame(IGamePhase game, Queue<GameConfig> gameQueue, MutableObject<IGamePhase> activeMicrogame) {
		IGamePhase lastMicrogame = activeMicrogame.getValue();
		activeMicrogame.setValue(null);

		GameConfig config = gameQueue.poll();
		if (config == null) {
			if (lastMicrogame == null) {
				return;
			}
			onComplete.apply(game, ContextMap.EMPTY);
			lastMicrogame.returnToParent(lastMicrogame.allPlayers());
			game.invoker(RiverRaceEvents.MICROGAMES_ENDED).onMicrogamesEnded();
			return;
		}

		PlayerSet allPlayers = lastMicrogame != null ? lastMicrogame.allPlayers() : game.allPlayers();

		PendingSubPhase subPhase = game.createSubPhase(config);
		subPhase.queuePlayers(allPlayers);

		subPhase.whenCreated((subGame, subEvents) -> {
			activeMicrogame.setValue(subGame);
			subEvents.listen(GamePlayerEvents.ADD, player ->
					onPlayerJoinMicrogame(subGame, player)
			);
			subEvents.listen(GamePhaseEvents.STOP, reason ->
					queueNextSubGame(game, gameQueue, activeMicrogame)
			);
			subEvents.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
					registerGlobalCommands(game, commands, gameQueue, activeMicrogame)
			);
			game.invoker(RiverRaceEvents.CREATE_MICROGAME).onCreateMicrogame(subGame, subEvents);
		});
		subPhase.whenErrored(exception -> {
			activeMicrogame.setValue(null);
			queueNextSubGame(game, gameQueue, activeMicrogame);
		});
	}

	private static <T> List<T> pickCountRandomly(List<T> candidates, int count) {
		List<T> shuffledCandidates = new ArrayList<>(candidates);
		Collections.shuffle(shuffledCandidates);
		return shuffledCandidates.subList(0, Math.min(shuffledCandidates.size(), count));
	}

	private void onPlayerJoinMicrogame(IGamePhase subGame, ServerPlayer player) {
		IGameDefinition definition = subGame.definition();
		player.sendSystemMessage(Component.literal("Now Playing: ").append(definition.name()).withStyle(ChatFormatting.GREEN));
		PlayerSet.of(player).showTitle(Component.empty().append(definition.name()).withStyle(ChatFormatting.GREEN), definition.subtitle(), 10, 40, 10);
	}
}
