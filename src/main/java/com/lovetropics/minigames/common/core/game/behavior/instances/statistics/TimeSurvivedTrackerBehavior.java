package com.lovetropics.minigames.common.core.game.behavior.instances.statistics;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;

public final class TimeSurvivedTrackerBehavior implements IGameBehavior {
	public static final Codec<TimeSurvivedTrackerBehavior> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
				TriggerAfterConfig.CODEC.optionalFieldOf("trigger", TriggerAfterConfig.EMPTY).forGetter(c -> c.trigger)
		).apply(instance, TimeSurvivedTrackerBehavior::new);
	});

	private final TriggerAfterConfig trigger;

	private long startTime = -1;

	public TimeSurvivedTrackerBehavior(TriggerAfterConfig trigger) {
		this.trigger = trigger;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		trigger.awaitThen(events, () -> {
			startTime = game.ticks();

			events.listen(GamePhaseEvents.FINISH, () -> onFinish(game));
			events.listen(GamePlayerEvents.DEATH, (player, source) -> onPlayerDeath(game, player, source));
		});
	}

	private void onFinish(IGamePhase game) {
		GameStatistics statistics = game.getStatistics();

		int secondsSurvived = getSecondsSurvived(game);
		for (ServerPlayerEntity player : game.getParticipants()) {
			statistics.forPlayer(player).set(StatisticKey.TIME_SURVIVED, secondsSurvived);
		}

		statistics.global().set(StatisticKey.TOTAL_TIME, secondsSurvived);
	}

	private ActionResultType onPlayerDeath(IGamePhase game, ServerPlayerEntity player, DamageSource source) {
		GameStatistics statistics = game.getStatistics();
		statistics.forPlayer(player).set(StatisticKey.TIME_SURVIVED, getSecondsSurvived(game));

		return ActionResultType.PASS;
	}

	private int getSecondsSurvived(IGamePhase game) {
		return (int) ((game.ticks() - startTime) / 20);
	}
}
