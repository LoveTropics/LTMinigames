package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.MapCodec;
import net.minecraft.SharedConstants;

import java.util.function.Supplier;

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
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.SET_TOTAL_TIME;
	}
}
