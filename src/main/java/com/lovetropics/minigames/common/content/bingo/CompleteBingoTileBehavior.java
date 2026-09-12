package com.lovetropics.minigames.common.content.bingo;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.MapCodec;

import java.util.function.Supplier;

public final class CompleteBingoTileBehavior implements IGameBehavior {
	public static final MapCodec<CompleteBingoTileBehavior> CODEC = MapCodec.unit(CompleteBingoTileBehavior::new);

	private int tile = -1;

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(Bingo.CAPTURE_TILE_EVENT, capturedTile -> this.tile = capturedTile);
		events.applyToPlayers(game, (_, target) -> {
			if (tile >= 0) {
				game.invoker(Bingo.COMPLETE_BINGO_TILE_EVENT).onCompleted(tile, target);
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
