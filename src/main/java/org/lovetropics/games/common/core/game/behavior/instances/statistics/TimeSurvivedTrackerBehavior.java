package org.lovetropics.games.common.core.game.behavior.instances.statistics;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.state.statistics.GameStatistics;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public final class TimeSurvivedTrackerBehavior implements IGameBehavior {
	public static final MapCodec<TimeSurvivedTrackerBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			TriggerAfterConfig.CODEC.optionalFieldOf("trigger", TriggerAfterConfig.EMPTY).forGetter(c -> c.trigger)
	).apply(i, TimeSurvivedTrackerBehavior::new));

	private final TriggerAfterConfig trigger;

	private long startTime = -1;

	public TimeSurvivedTrackerBehavior(TriggerAfterConfig trigger) {
		this.trigger = trigger;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		trigger.awaitThen(game, events, () -> {
			startTime = game.ticks();

			events.listen(GamePhaseEvents.FINISH, () -> onFinish(game));
			events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
				if (lastRole == PlayerRole.PARTICIPANT) {
					onPlayerEliminated(game, player);
				}
			});
		});
	}

	private void onFinish(IGamePhase game) {
		GameStatistics statistics = game.statistics();

		int secondsSurvived = getSecondsSurvived(game);
		for (ServerPlayer player : game.participants()) {
			statistics.forPlayer(player).set(StatisticKey.TIME_SURVIVED, secondsSurvived);
		}

		statistics.global().set(StatisticKey.TOTAL_TIME, secondsSurvived);
	}

	private InteractionResult onPlayerEliminated(IGamePhase game, ServerPlayer player) {
		GameStatistics statistics = game.statistics();
		statistics.forPlayer(player).set(StatisticKey.TIME_SURVIVED, getSecondsSurvived(game));

		return InteractionResult.PASS;
	}

	private int getSecondsSurvived(IGamePhase game) {
		return (int) ((game.ticks() - startTime) / SharedConstants.TICKS_PER_SECOND);
	}
}
