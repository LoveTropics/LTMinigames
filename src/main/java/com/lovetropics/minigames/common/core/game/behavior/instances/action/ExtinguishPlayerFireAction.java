package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.MapCodec;

public record ExtinguishPlayerFireAction() implements IGameBehavior {
	public static final MapCodec<ExtinguishPlayerFireAction> CODEC = MapCodec.unit(ExtinguishPlayerFireAction::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY_TO_PLAYER, (context, target) -> {
			if (target.isOnFire()) {
				target.extinguishFire();
				return true;
			}
			return false;
		});
	}
}
