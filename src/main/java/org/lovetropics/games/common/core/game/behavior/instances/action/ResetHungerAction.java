package org.lovetropics.games.common.core.game.behavior.instances.action;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.instances.tweak.HungerResetter;
import com.mojang.serialization.MapCodec;

public record ResetHungerAction() implements IGameBehavior {
	public static final MapCodec<ResetHungerAction> CODEC = MapCodec.unit(ResetHungerAction::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToPlayers(game, (context, target) -> {
			HungerResetter.reset(target);
			return true;
		});
	}
}
