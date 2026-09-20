package org.lovetropics.games.common.core.game.behavior.instances.action;

import org.lovetropics.games.common.core.game.GameStopReason;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.MapCodec;

public record CloseGameAction() implements IGameBehavior {
	public static final MapCodec<CloseGameAction> CODEC = MapCodec.unit(CloseGameAction::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			game.requestStop(GameStopReason.finished());
			return true;
		});
	}
}
