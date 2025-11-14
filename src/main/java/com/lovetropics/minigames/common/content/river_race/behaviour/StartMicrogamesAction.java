package com.lovetropics.minigames.common.content.river_race.behaviour;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContext;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.SubGameEvents;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public record StartMicrogamesAction(
		List<ResourceLocation> gameConfigIds,
		int gamesPerRound,
		GameActionList<Void> onComplete
) implements IGameBehavior {

	public static final MapCodec<StartMicrogamesAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.nonEmptyList(ResourceLocation.CODEC.listOf()).fieldOf("games").forGetter(StartMicrogamesAction::gameConfigIds),
			Codec.INT.optionalFieldOf("games_per_round", 1).forGetter(c -> c.gamesPerRound),
			GameActionList.VOID_CODEC.optionalFieldOf("on_complete", GameActionList.EMPTY_VOID).forGetter(StartMicrogamesAction::onComplete)
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

		MutableBoolean scheduled = new MutableBoolean();
		Queue<GameConfig> gameQueue = new ArrayDeque<>();

		events.listen(GameActionEvents.APPLY, context -> {
			gameQueue.clear();
			gameQueue.addAll(pickCountRandomly(gameConfigs, gamesPerRound));
			if (queueNextSubGame(game, gameQueue)) {
				scheduled.setTrue();
			}
			return true;
		});

		events.listen(SubGameEvents.CREATE, (subGame, subEvents) -> {
			if (scheduled.isFalse()) {
				return;
			}
			subEvents.listen(GamePhaseEvents.STOP, reason ->
					queueNextSubGame(game, gameQueue)
			);
		});

		events.listen(SubGameEvents.RETURN_TO_TOP, () -> {
			if (scheduled.isTrue()) {
				scheduled.setFalse();
				onComplete.apply(game, GameActionContext.EMPTY);
			}
		});
	}

	private boolean queueNextSubGame(IGamePhase game, Queue<GameConfig> gameQueue) {
		GameConfig config = gameQueue.poll();
		if (config != null) {
			game.queueSubGame(config);
			return true;
		}
		return false;
	}

	private static <T> List<T> pickCountRandomly(List<T> candidates, int count) {
		List<T> shuffledCandidates = new ArrayList<>(candidates);
		Collections.shuffle(shuffledCandidates);
		return shuffledCandidates.subList(0, Math.min(shuffledCandidates.size(), count));
	}
}
