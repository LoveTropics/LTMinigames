package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.MapCodec;
import net.minecraft.SharedConstants;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;

public record SetTotalTimeAction() implements IGameBehavior {
	public static final MapCodec<SetTotalTimeAction> CODEC = MapCodec.unit(SetTotalTimeAction::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.applyToPlayers(game, (context, target) -> {
			game.statistics().forPlayer(target).set(StatisticKey.TOTAL_TIME, (int) (game.ticks() / SharedConstants.TICKS_PER_SECOND));
			return true;
		});
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
