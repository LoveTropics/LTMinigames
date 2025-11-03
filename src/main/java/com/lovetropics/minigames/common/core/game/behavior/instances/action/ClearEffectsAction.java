package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.MapCodec;

public record ClearEffectsAction() implements IGameBehavior {
	public static final MapCodec<ClearEffectsAction> CODEC = MapCodec.unit(ClearEffectsAction::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY_TO_LIVING_ENTITY, (context, livingEntity) -> {
			if (livingEntity.getActiveEffects().isEmpty()) {
				return false;
			}
			livingEntity.removeAllEffects();
			return true;
		});
	}
}
