package org.lovetropics.games.common.content.bingo;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.function.Supplier;

/// Unlocks tiles from the bingo board's pool of locked tiles
public record UnlockBingoTilesAction(int count) implements IGameBehavior {
	public static final MapCodec<UnlockBingoTilesAction> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count", 1).forGetter(UnlockBingoTilesAction::count)
	).apply(in, UnlockBingoTilesAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			BingoBoard board = game.instanceState().getOrNull(BingoBoard.KEY);
			return board != null && board.unlockTiles(count) > 0;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return Bingo.UNLOCK_TILES;
	}
}
