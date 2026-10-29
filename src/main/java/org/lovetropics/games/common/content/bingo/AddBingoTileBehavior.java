package org.lovetropics.games.common.content.bingo;

import com.mojang.serialization.MapCodec;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;

public record AddBingoTileBehavior(BingoTileDefinition tile) implements IGameBehavior {
	public static final MapCodec<AddBingoTileBehavior> CODEC = BingoTileDefinition.MAP_CODEC.xmap(AddBingoTileBehavior::new, AddBingoTileBehavior::tile);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			BingoBoard board = game.instanceState().getOrNull(BingoBoard.KEY);
			return board != null && board.addTile(tile) >= 0;
		});
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
