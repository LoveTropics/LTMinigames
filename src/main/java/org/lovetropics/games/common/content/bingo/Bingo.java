package org.lovetropics.games.common.content.bingo;

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

	public static final GameEventType<CaptureBingoTile> CAPTURE_TILE_EVENT = GameEventType.create(CaptureBingoTile.class, listeners -> (tile) -> {
		for (CaptureBingoTile listener : listeners) {
			listener.capture(tile);
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
}
