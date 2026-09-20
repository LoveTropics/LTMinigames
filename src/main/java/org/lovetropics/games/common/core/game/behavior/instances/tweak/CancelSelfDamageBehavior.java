package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;

public record CancelSelfDamageBehavior() implements IGameBehavior {
	public static final MapCodec<CancelSelfDamageBehavior> CODEC = MapCodec.unit(CancelSelfDamageBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePlayerEvents.DAMAGE_AMOUNT, (player, damageSource, amount, originalAmount) ->
				damageSource.getEntity() == player ? 0.0f : amount
		);
	}
}
