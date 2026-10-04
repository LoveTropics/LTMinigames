package org.lovetropics.games.common.content.bingo;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.behavior.event.GameEventType;
import org.lovetropics.games.common.core.game.util.TranslationCollector;
import org.lovetropics.games.common.util.registry.GameBehaviorEntry;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class Bingo {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.bingo.");

	public static final TranslationCollector.Fun2 TILE_COMPLETED = KEYS.add2("tile_completed", "%s completed tile %s!");
	public static final TranslationCollector.Fun1 TILES_UNLOCKED = KEYS.add1("tiles_unlocked", "New bingo tiles unlocked: %s");
	public static final TranslationCollector.Fun1 LOCKED_HINT_CLEAR = KEYS.add1("locked_hint.clear", "Complete every tile to unlock %s more!");

	// Board display
	public static final Component BOARD_TITLE = KEYS.add("board.title", "Bingo");
	public static final TranslationCollector.Fun3 BOARD_HEADER = KEYS.add3("board.header", "%s points - %s/%s tiles");
	public static final TranslationCollector.Fun1 BOARD_LOCKED_COUNT = KEYS.add1("board.locked_count", "%s locked");
	public static final Component TILE_LOCKED = KEYS.add("tile.locked", "Locked");
	public static final Component TILE_DONE = KEYS.add("tile.done", "Done!");
	public static final TranslationCollector.Fun1 TILE_REWARD = KEYS.add1("tile.reward", "+%s points");
	public static final TranslationCollector.Fun1 TILE_COMPLETIONS = KEYS.add1("tile.completions", "Done by %s");

	public static final GameEventType<CaptureBingoTile> CAPTURE_TILE_EVENT = GameEventType.create(CaptureBingoTile.class, listeners -> (tile) -> {
		for (CaptureBingoTile listener : listeners) {
			listener.capture(tile);
		}
	});

	/// Fired in the phase that owns the board once a player has completed every tile of a full board
	public static final GameEventType<BoardCompleted> BOARD_COMPLETED = GameEventType.create(BoardCompleted.class, listeners -> (player) -> {
		for (BoardCompleted listener : listeners) {
			listener.onBoardCompleted(player);
		}
	});

	public static final GameBehaviorEntry<BingoBehavior> BINGO = REGISTRATE.object("bingo")
			.behavior(BingoBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<AddBingoTileBehavior> ADD_BINGO_TILE = REGISTRATE.object("bingo/add_tile")
			.behavior(AddBingoTileBehavior.CODEC)
			.register();
	public static final GameBehaviorEntry<CompleteBingoTileBehavior> COMPLETE_BINGO_TILE = REGISTRATE.object("bingo/complete_tile")
			.behavior(CompleteBingoTileBehavior.CODEC)
			.register();
	public static final GameBehaviorEntry<UnlockBingoTilesAction> UNLOCK_TILES = REGISTRATE.object("bingo/unlock_tiles")
			.behavior(UnlockBingoTilesAction.CODEC)
			.register();

	public static void init() {
	}

	public interface CaptureBingoTile {
		void capture(int tileIndex);
	}

	public interface BoardCompleted {
		void onBoardCompleted(ServerPlayer player);
	}
}
