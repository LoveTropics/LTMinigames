package org.lovetropics.games.common.core.game.behavior.instances;

import com.mojang.serialization.MapCodec;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.GameWinner;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameLogicEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;

public record NoneRemainingEndTrigger() implements IGameBehavior {
	public static final MapCodec<NoneRemainingEndTrigger> CODEC = MapCodec.unit(NoneRemainingEndTrigger::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		MutableBoolean triggered = new MutableBoolean();
		events.listen(GameLogicEvents.GAME_OVER, _ -> triggered.setTrue());
		events.listen(GamePlayerEvents.SET_ROLE, (_, _, lastRole) -> {
			if (!triggered.get() && lastRole == PlayerRole.PARTICIPANT && game.participants().isEmpty()) {
				game.invoker(GameLogicEvents.GAME_OVER).onGameOver(new GameWinner.Nobody());
			}
		});
	}
}
