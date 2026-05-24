package com.lovetropics.minigames.common.content.river_race.microgames;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.context.ContextMap;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record StartMicrogamesAction(
		List<Identifier> gameConfigIds,
		int gamesPerRound,
		GameActionList onComplete
) implements IGameBehavior {

	public static final MapCodec<StartMicrogamesAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.nonEmptyList(Identifier.CODEC.listOf()).fieldOf("games").forGetter(StartMicrogamesAction::gameConfigIds),
			Codec.INT.optionalFieldOf("games_per_round", 1).forGetter(c -> c.gamesPerRound),
			GameActionList.CODEC.optionalFieldOf("on_complete", GameActionList.EMPTY).forGetter(StartMicrogamesAction::onComplete)
	).apply(i, StartMicrogamesAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		MicrogamesManager microgames = MicrogamesManager.get(game);

		onComplete.register(game, events);

		List<GameConfig> gameConfigs = new ArrayList<>(gameConfigIds.size());
		for (Identifier configId : gameConfigIds) {
			GameConfig config = GameConfigs.REGISTRY.get(configId);
			if (config == null) {
				throw new GameException(Component.literal("Missing microgame config with id: " + configId));
			}
			gameConfigs.add(config);
		}

		MutableBoolean scheduled = new MutableBoolean();
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			microgames.clearQueue();
			microgames.addToQueue(pickCountRandomly(gameConfigs, gamesPerRound));
			if (microgames.startQueueIfStopped()) {
				scheduled.setTrue();
			}
			return true;
		});

		events.listen(MicrogameEvents.MICROGAMES_ENDED, () -> {
			if (scheduled.isFalse()) {
				return;
			}
			scheduled.setFalse();
			onComplete.apply(game, ContextMap.EMPTY, ActionSubjects.EMPTY);
		});
	}

	private static <T> List<T> pickCountRandomly(List<T> candidates, int count) {
		List<T> shuffledCandidates = new ArrayList<>(candidates);
		Collections.shuffle(shuffledCandidates);
		return shuffledCandidates.subList(0, Math.min(shuffledCandidates.size(), count));
	}
}
