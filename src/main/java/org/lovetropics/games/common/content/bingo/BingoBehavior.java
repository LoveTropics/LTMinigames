package org.lovetropics.games.common.content.bingo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.SubGameEvents;
import org.lovetropics.games.common.core.game.state.GameStateMap;

import java.util.List;
import java.util.Map;

/// Sets up a [BingoBoard] for this game. Its tiles are tracked for players in this phase and in any of its sub-phases.
public final class BingoBehavior implements IGameBehavior {
	public static final MapCodec<BingoBehavior> CODEC = RecordCodecBuilder.<BingoBehavior>mapCodec(in -> in.group(
			Codec.INT.fieldOf("rows").forGetter(b -> b.rows),
			Codec.INT.fieldOf("columns").forGetter(b -> b.columns),
			Codec.FLOAT.listOf(1, Integer.MAX_VALUE).fieldOf("position_reward_multipler").forGetter(b -> b.positionRewardMultiplier),
			GameActionList.CODEC.optionalFieldOf("on_tile_completed", GameActionList.EMPTY).forGetter(b -> b.onTileCompleted),
			GameActionList.CODEC.optionalFieldOf("on_bingo", GameActionList.EMPTY).forGetter(b -> b.onBingo),
			Codec.unboundedMap(TilePosition.CODEC, BingoTileDefinition.CODEC).optionalFieldOf("tiles", Map.of()).forGetter(b -> b.tiles),
			Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("initial_tiles", 0).forGetter(b -> b.initialTiles),
			Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("unlock_on_clear", 0).forGetter(b -> b.unlockOnClear)
	).apply(in, BingoBehavior::new)).validate(BingoBehavior::validate);

	private final int rows, columns;
	private final List<Float> positionRewardMultiplier;
	private final GameActionList onTileCompleted, onBingo;
	private final Map<TilePosition, BingoTileDefinition> tiles;
	private final int initialTiles;
	private final int unlockOnClear;

	private BingoBoard board;

	/// @param tiles         the tiles at their position on the board
	/// @param initialTiles  how many tiles are unlocked when the game starts
	/// @param unlockOnClear how many tiles are unlocked for everyone once a player completes every unlocked tile
	public BingoBehavior(int rows, int columns, List<Float> positionRewardMultiplier, GameActionList onTileCompleted, GameActionList onBingo, Map<TilePosition, BingoTileDefinition> tiles, int initialTiles, int unlockOnClear) {
		this.rows = rows;
		this.columns = columns;
		this.positionRewardMultiplier = positionRewardMultiplier;
		this.onTileCompleted = onTileCompleted;
		this.onBingo = onBingo;
		this.tiles = tiles;
		this.initialTiles = initialTiles;
		this.unlockOnClear = unlockOnClear;
	}

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		board = instanceState.register(BingoBoard.KEY, new BingoBoard(game, rows, columns, positionRewardMultiplier, onTileCompleted, onBingo, tiles, unlockOnClear));
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		onTileCompleted.register(game, events);
		onBingo.register(game, events);

		events.listen(GamePhaseEvents.START, _ -> {
			board.start();
			board.unlockTiles(initialTiles);
			board.updateTiles();
		});
		events.listen(GamePhaseEvents.STOP, _ -> board.stop());
		events.listen(GamePlayerEvents.ADD, board::updatePlayer);
		events.listen(GamePlayerEvents.SET_ROLE, (player, _, _) -> board.updatePlayer(player));
		events.listen(GamePlayerEvents.OVERLORD_CHANGE, (player, _) -> board.updatePlayer(player));
		events.listen(GamePlayerEvents.REMOVE, board::removeFromPlayer);

		// Players moving into a sub-phase are removed from this one, which clears their board - send it again once they arrive
		events.listen(SubGameEvents.CREATE, (_, subEvents) -> subEvents.listen(GamePlayerEvents.ADD, board::updatePlayer));
	}

	private static DataResult<BingoBehavior> validate(BingoBehavior behavior) {
		List<TilePosition> outside = behavior.tiles.keySet().stream()
				.filter(position -> !position.isOnBoard(behavior.rows, behavior.columns))
				.toList();
		if (!outside.isEmpty()) {
			return DataResult.error(() -> "Tiles outside of the " + behavior.rows + "x" + behavior.columns + " board: " + outside);
		}
		return DataResult.success(behavior);
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
