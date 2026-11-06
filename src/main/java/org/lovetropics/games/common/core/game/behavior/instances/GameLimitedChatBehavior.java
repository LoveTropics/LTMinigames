package org.lovetropics.games.common.core.game.behavior.instances;

import com.mojang.serialization.MapCodec;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.player.PlayerSet;

public record GameLimitedChatBehavior() implements IGameBehavior {

	public static final MapCodec<GameLimitedChatBehavior> CODEC = MapCodec.unit(GameLimitedChatBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePlayerEvents.CHAT_SEND, (source, target) -> {
			PlayerSet allPlayers = game.allPlayers();
			if (!allPlayers.contains(source) && allPlayers.contains(target)) {
				return false;
			}
			return true;
		});
	}
}
