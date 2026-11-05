package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;

public record SetTotalTimeAction(
		int addSeconds
) implements IGameBehavior {
	public static final MapCodec<SetTotalTimeAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.optionalFieldOf("add_seconds", 0).forGetter(SetTotalTimeAction::addSeconds)
	).apply(i, SetTotalTimeAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.applyToPlayers(game, (context, target) -> {
			long gameSeconds = game.ticks() / SharedConstants.TICKS_PER_SECOND;
			game.statistics().forPlayer(target).set(StatisticKey.TOTAL_TIME, Math.max(0, (int) gameSeconds + addSeconds));
			return true;
		});
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
