package org.lovetropics.games.common.content.bingo;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.MapCodec;

import java.util.function.Supplier;

public final class CompleteBingoTileBehavior implements IGameBehavior {
	public static final MapCodec<CompleteBingoTileBehavior> CODEC = MapCodec.unit(CompleteBingoTileBehavior::new);

	private int tile = -1;

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(Bingo.CAPTURE_TILE_EVENT, capturedTile -> tile = capturedTile);
		events.applyToPlayers(game, (_, target) -> {
			BingoBoard board = game.instanceState().getOrNull(BingoBoard.KEY);
			if (board != null && tile >= 0) {
				board.complete(tile, target);
				return true;
			}
			return false;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return Bingo.COMPLETE_BINGO_TILE;
	}
}
