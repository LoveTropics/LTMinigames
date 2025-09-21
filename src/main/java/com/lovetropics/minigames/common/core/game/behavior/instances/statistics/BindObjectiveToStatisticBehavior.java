package com.lovetropics.minigames.common.core.game.behavior.instances.statistics;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;

import java.util.Map;

public record BindObjectiveToStatisticBehavior(Map<StatisticKey<Integer>, String> statisticToObjective) implements IGameBehavior {
	public static final MapCodec<BindObjectiveToStatisticBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(StatisticKey.typedCodec(Integer.class), Codec.STRING).fieldOf("objectives").forGetter(c -> c.statisticToObjective)
	).apply(i, BindObjectiveToStatisticBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePhaseEvents.STOP, reason -> {
			ServerScoreboard scoreboard = game.server().getScoreboard();

			for (Map.Entry<StatisticKey<Integer>, String> entry : statisticToObjective.entrySet()) {
				StatisticKey<Integer> key = entry.getKey();
				String objectiveKey = entry.getValue();

				Objective objective = scoreboard.getObjective(objectiveKey);
				if (objective != null) {
					applyFromObjective(game, key, objective);
				}
			}
		});
	}

	private void applyFromObjective(IGamePhase game, StatisticKey<Integer> key, Objective objective) {
		GameStatistics statistics = game.statistics();
		ServerScoreboard scoreboard = game.server().getScoreboard();

		for (ServerPlayer player : game.allPlayers()) {
			int score = scoreboard.getOrCreatePlayerScore(player, objective).get();
			statistics.forPlayer(player).set(key, score);
		}
	}
}
