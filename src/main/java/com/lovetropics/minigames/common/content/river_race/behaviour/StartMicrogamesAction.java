package com.lovetropics.minigames.common.content.river_race.behaviour;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContext;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.SubGameEvents;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.lovetropics.minigames.common.core.game.impl.GamePhase;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
		events.listen(GameActionEvents.APPLY, context -> {
			queueMicrogames(game, gameConfigs);
			startQueuedMicrogame(game);
			scheduled.setTrue();
			return true;
		});

		events.listen(SubGameEvents.RETURN_TO_TOP, () -> {
			if (scheduled.isTrue()) {
				scheduled.setFalse();
				onComplete.apply(game, GameActionContext.EMPTY);
			}
		});
	}

	public void queueMicrogames(IGamePhase game, List<GameConfig> gameConfigs) {
		// TODO: Don't do that!
		if (game.getTopPhase() instanceof GamePhase multiGamePhase) {
			multiGamePhase.clearQueuedGames();

			final List<GameConfig> configs = new ArrayList<>(gameConfigs);
			Collections.shuffle(configs);
			multiGamePhase.queueGames(configs.subList(0, Math.min(configs.size(), gamesPerRound)));
		}
	}

	public void startQueuedMicrogame(final IGamePhase game) {
		// TODO: Don't do that!
		if (game.getTopPhase() instanceof GamePhase multiGamePhase) {
			multiGamePhase.startNextQueuedMicrogame();
		}
	}
}
