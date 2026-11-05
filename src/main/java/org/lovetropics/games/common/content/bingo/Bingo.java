package org.lovetropics.games.common.content.bingo;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.behavior.event.GameEventType;
import org.lovetropics.games.common.core.game.util.TranslationCollector;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

import java.util.List;

public class Bingo {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.bingo.");

	public static final TranslationCollector.Fun2 TILE_COMPLETED = KEYS.add2("tile_completed", "%s completed tile %s!");
	public static final TranslationCollector.Fun1 TILES_UNLOCKED = KEYS.add1("tiles_unlocked", "New bingo tiles unlocked: %s");
	public static final TranslationCollector.Fun1 LOCKED_HINT_CLEAR = KEYS.add1("locked_hint.clear", "Complete every tile to unlock %s more!");

	// Board display
	public static final Component BOARD_TITLE = KEYS.add("board.title", "Bingo");
	public static final TranslationCollector.Fun1 OPEN_BOARD_HINT = KEYS.add1("board.open_hint", "[%s] Bingo");
	public static final TranslationCollector.Fun1 TOGGLE_BINGO_HUD = KEYS.add1("bingo.toggle_hud", "[%s] Toggle HUD");
	public static final TranslationCollector.Fun3 BOARD_HEADER = KEYS.add3("board.header", "%s points - %s/%s tiles");
	public static final TranslationCollector.Fun1 BOARD_LOCKED_COUNT = KEYS.add1("board.locked_count", "%s locked");
	public static final Component TILE_LOCKED = KEYS.add("tile.locked", "Locked");
	public static final List<Component> TILE_PLACES = List.of(
			KEYS.add("tile.place.1", "1st"),
			KEYS.add("tile.place.2", "2nd"),
			KEYS.add("tile.place.3", "3rd"),
			KEYS.add("tile.place.4", "4th"),
			KEYS.add("tile.place.5", "5th"),
			KEYS.add("tile.place.6", "6th")
	);
	public static final TranslationCollector.Fun1 TILE_PLACE = KEYS.add1("tile.place", "%sth");
	public static final TranslationCollector.Fun2 TILE_PLACE_REWARD = KEYS.add2("tile.place_reward", "%s: %s points");

	/// Fired in the phase that owns the board once a player has completed every tile of a full board
	public static final GameEventType<BoardCompleted> BOARD_COMPLETED = GameEventType.create(BoardCompleted.class, listeners -> (player) -> {
		for (BoardCompleted listener : listeners) {
			listener.onBoardCompleted(player);
		}
	});

	public static void init() {
		REGISTRATE.registerBehavior("bingo", BingoBehavior.CODEC);
		REGISTRATE.registerBehavior("bingo/add_tile", AddBingoTileBehavior.CODEC);
		REGISTRATE.registerBehavior("bingo/unlock_tiles", UnlockBingoTilesAction.CODEC);
	}

	public interface BoardCompleted {
		void onBoardCompleted(ServerPlayer player);
	}
}
