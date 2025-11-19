package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record StatisticThresholdTrigger(
		StatisticKey<Integer> statistic,
		int threshold,
		GameActionList actions
) implements IGameBehavior {
	public static final MapCodec<StatisticThresholdTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(StatisticThresholdTrigger::statistic),
			Codec.INT.fieldOf("threshold").forGetter(StatisticThresholdTrigger::threshold),
			GameActionList.MAP_CODEC.forGetter(StatisticThresholdTrigger::actions)
	).apply(i, StatisticThresholdTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		actions.register(game, events);

		// TODO: Ideally we would have an event for tracking statistic changes
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);

		Set<UUID> triggeredPlayers = new HashSet<>();
		Set<GameTeamKey> triggeredTeams = new HashSet<>();

		events.listen(GamePhaseEvents.TICK, () -> {
			GameStatistics statistics = game.statistics();
			for (ServerPlayer player : game.allPlayers()) {
				if (statistics.forPlayer(player).getInt(statistic) < threshold) {
					continue;
				}
				if (triggeredPlayers.add(player.getUUID())) {
					actions.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
				}
			}
			if (teams != null) {
				for (GameTeamKey team : teams.getTeamKeys()) {
					if (statistics.forTeam(team).getInt(statistic) < threshold) {
						continue;
					}
					if (triggeredTeams.add(team)) {
						actions.apply(game, ContextMap.EMPTY, ActionSubjects.ofTeam(team));
					}
				}
			}
		});
	}
}
