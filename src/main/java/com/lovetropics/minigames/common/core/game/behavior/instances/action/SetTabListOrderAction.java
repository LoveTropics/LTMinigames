package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.util.duck.ServerPlayerExtension;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.function.Supplier;

public record SetTabListOrderAction(int order) implements IGameBehavior {
	public static final MapCodec<SetTabListOrderAction> CODEC = Codec.INT
			.fieldOf("order").xmap(SetTabListOrderAction::new, SetTabListOrderAction::order);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY_TO_PLAYER, (context, target) -> {
			((ServerPlayerExtension) target).lt$setPlayerListOrder(order);
			return true;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.SET_TAB_LIST_ORDER;
	}
}
