package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.context.ContextMap;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.integration.game_actions.GamePackage;

import java.util.Optional;

public record IncrementStatisticAction(
		StatisticKey<Integer> statistic,
		int amount,
		// TODO: These are weird - we should generalise that
		Optional<StatisticKey<Integer>> fromStatistic,
		boolean fromParticipantCount,
		// Packages without an amount, such as those sent without one by command, use the amount instead
		Optional<Double> perDonatedAmount,
		SetStatisticAction.Scope scope
) implements IGameBehavior {
	public static final MapCodec<IncrementStatisticAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(IncrementStatisticAction::statistic),
			Codec.INT.optionalFieldOf("amount", 1).forGetter(IncrementStatisticAction::amount),
			StatisticKey.INT_CODEC.optionalFieldOf("from_statistic").forGetter(IncrementStatisticAction::fromStatistic),
			Codec.BOOL.optionalFieldOf("from_participant_count", false).forGetter(IncrementStatisticAction::fromParticipantCount),
			Codec.DOUBLE.optionalFieldOf("per_donated_amount").forGetter(IncrementStatisticAction::perDonatedAmount),
			SetStatisticAction.Scope.CODEC.fieldOf("scope").forGetter(IncrementStatisticAction::scope)
	).apply(i, IncrementStatisticAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY, (context, targets) ->
				scope.applyTo(game, targets, statistics -> {
					int amount;
					if (fromStatistic.isPresent()) {
						amount = statistics.getInt(fromStatistic.get());
					} else if (fromParticipantCount) {
						amount = game.participants().size();
					} else if (perDonatedAmount.isPresent() && getDonatedAmount(context).isPresent()) {
						amount = (int) Math.floor(getDonatedAmount(context).get() * perDonatedAmount.get());
					} else {
						amount = this.amount;
					}
					statistics.incrementInt(statistic, amount);
				})
		);
	}

	private static Optional<Double> getDonatedAmount(ContextMap context) {
		return Optional.ofNullable(context.getOptional(GameActionContextKeys.PACKAGE)).flatMap(GamePackage::amount);
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
