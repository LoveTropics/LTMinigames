package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;

import java.util.List;
import java.util.Objects;

public record TopPlayerTrigger(List<GameActionList> actionsByPlace, GameActionList fallbackActions) implements IGameBehavior {
	public static final MapCodec<TopPlayerTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameActionList.CODEC.listOf().fieldOf("by_place").forGetter(TopPlayerTrigger::actionsByPlace),
			GameActionList.CODEC.optionalFieldOf("fallback", GameActionList.EMPTY).forGetter(TopPlayerTrigger::fallbackActions)
	).apply(i, TopPlayerTrigger::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) throws GameException {
		for (final GameActionList actions : actionsByPlace) {
			actions.register(game, events);
		}
		fallbackActions.register(game, events);

		events.listen(GamePhaseEvents.FINISH, () -> {
			final GameStatistics statistics = game.statistics();
			for (final ServerPlayer player : game.allPlayers()) {
				final int placement = Objects.requireNonNullElse(statistics.forPlayer(player).get(StatisticKey.PLACEMENT), 0);
				final GameActionList actions;
				if (placement > 0 && placement <= actionsByPlace.size()) {
					actions = actionsByPlace.get(placement - 1);
				} else {
					actions = fallbackActions;
				}
				actions.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
			}
		});
	}
}
