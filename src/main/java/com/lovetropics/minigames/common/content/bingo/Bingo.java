package com.lovetropics.minigames.common.content.bingo;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class Bingo {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameEventType<RequestNewTile> REQUEST_NEW_TILE_EVENT = GameEventType.create(RequestNewTile.class, listeners -> (icon, title) -> {
		for (var listener : listeners) {
			var index = listener.requestTile(icon, title);
			if (index >= 0) return index;
		}
		return -1;
	});

	public static final GameEventType<CompleteBingoTile> COMPLETE_BINGO_TILE_EVENT = GameEventType.create(CompleteBingoTile.class, listeners -> (tile, player) -> {
		for (var listener : listeners) {
			listener.onCompleted(tile, player);
		}
	});

	public static final GameEventType<CaptureBingoTile> CAPTURE_TILE_EVENT = GameEventType.create(CaptureBingoTile.class, listeners -> (tile) -> {
		for (var listener : listeners) {
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

	public static void init() {
	}

	public interface RequestNewTile {
		/// @return the new tile index, or `-1` if a new tile could not be added
		int requestTile(ItemStack icon, Component title);
	}

	public interface CaptureBingoTile {
		void capture(int tileIndex);
	}

	public interface CompleteBingoTile {
		void onCompleted(int tile, ServerPlayer completingPlayer);
	}
}
