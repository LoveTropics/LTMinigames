package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.util.duck.ServerPlayerExtension;

public record SetTabListOrderAction(int order) implements IGameBehavior {
	public static final MapCodec<SetTabListOrderAction> CODEC = Codec.INT
			.fieldOf("order").xmap(SetTabListOrderAction::new, SetTabListOrderAction::order);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToPlayers(game, (context, target) -> {
			((ServerPlayerExtension) target).lt$setPlayerListOrder(order);
			return true;
		});
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
