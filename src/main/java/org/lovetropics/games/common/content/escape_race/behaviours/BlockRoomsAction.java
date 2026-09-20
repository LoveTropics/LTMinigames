package org.lovetropics.games.common.content.escape_race.behaviours;

import org.lovetropics.games.common.content.escape_race.event.EscapeRaceEvents;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BlockRoomsAction(
		boolean blocked
) implements IGameBehavior {
	public static final MapCodec<BlockRoomsAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.fieldOf("blocked").forGetter(BlockRoomsAction::blocked)
	).apply(i, BlockRoomsAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			game.invoker(EscapeRaceEvents.BLOCK_ROOMS).setRoomsBlocked(blocked);
			return true;
		});
	}
}
