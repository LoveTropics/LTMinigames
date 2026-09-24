package org.lovetropics.games.common.content.bingo;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.SubGameEvents;
import org.lovetropics.games.common.core.game.state.GameStateMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.function.Supplier;

/// Sets up a [BingoBoard] for this game. Its tiles are tracked for players in this phase and in any of its sub-phases.
public final class BingoBehavior implements IGameBehavior {
	public static final MapCodec<BingoBehavior> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			Codec.INT.fieldOf("rows").forGetter(b -> b.rows),
			Codec.INT.fieldOf("columns").forGetter(b -> b.columns),
			Codec.FLOAT.listOf(1, Integer.MAX_VALUE).fieldOf("position_reward_multipler").forGetter(b -> b.positionRewardMultiplier),
			GameActionList.CODEC.optionalFieldOf("on_tile_completed", GameActionList.EMPTY).forGetter(b -> b.onTileCompleted),
			GameActionList.CODEC.optionalFieldOf("on_bingo", GameActionList.EMPTY).forGetter(b -> b.onBingo),
			BingoTileDefinition.CODEC.listOf().optionalFieldOf("tile_pool", List.of()).forGetter(b -> b.tilePool),
			Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("initial_tiles", 0).forGetter(b -> b.initialTiles),
			Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("unlock_on_clear", 0).forGetter(b -> b.unlockOnClear)
	).apply(in, BingoBehavior::new));

	private final int rows, columns;
	private final List<Float> positionRewardMultiplier;
	private final GameActionList onTileCompleted, onBingo;
	private final List<BingoTileDefinition> tilePool;
	private final int initialTiles;
	private final int unlockOnClear;

	private BingoBoard board;

	/// @param tilePool      locked tiles, which are unlocked at random by `ltminigames:bingo/unlock_tiles`
	/// @param initialTiles  how many tiles from the pool are unlocked when the game starts
	/// @param unlockOnClear how many tiles from the pool are unlocked for everyone once a player completes every unlocked tile
	public BingoBehavior(int rows, int columns, List<Float> positionRewardMultiplier, GameActionList onTileCompleted, GameActionList onBingo, List<BingoTileDefinition> tilePool, int initialTiles, int unlockOnClear) {
		this.rows = rows;
		this.columns = columns;
		this.positionRewardMultiplier = positionRewardMultiplier;
		this.onTileCompleted = onTileCompleted;
		this.onBingo = onBingo;
		this.tilePool = tilePool;
		this.initialTiles = initialTiles;
		this.unlockOnClear = unlockOnClear;
	}

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		board = instanceState.register(BingoBoard.KEY, new BingoBoard(game, rows, columns, positionRewardMultiplier, onTileCompleted, onBingo, tilePool, unlockOnClear));
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		onTileCompleted.register(game, events);
		onBingo.register(game, events);

		board.addPhase(game, events);

		events.listen(GamePhaseEvents.START, _ -> {
			board.unlockTiles(initialTiles);
			board.updateTiles();
		});
		events.listen(GamePlayerEvents.ADD, board::updatePlayer);
		events.listen(GamePlayerEvents.REMOVE, board::removeFromPlayer);

		// Players moving into a sub-phase are removed from this one, which clears their board - send it again once they arrive
		events.listen(SubGameEvents.CREATE, (subGame, subEvents) -> {
			board.addPhase(subGame, subEvents);
			subEvents.listen(GamePlayerEvents.ADD, board::updatePlayer);
			subEvents.listen(GamePhaseEvents.STOP, _ -> board.removePhase(subGame));
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return Bingo.BINGO;
	}
}
