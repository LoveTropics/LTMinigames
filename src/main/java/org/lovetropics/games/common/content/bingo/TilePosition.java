package org.lovetropics.games.common.content.bingo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public record TilePosition(int row, int column) {
	public static final Codec<TilePosition> CODEC = Codec.STRING.comapFlatMap(TilePosition::parse, TilePosition::toString);

	private static DataResult<TilePosition> parse(String string) {
		String[] parts = string.split(":");
		// lets be verbose in case we do whoopsies live!
		if (parts.length != 2) {
			return DataResult.error(() -> "Expected a tile position as \"row:column\", got \"" + string + "\"");
		}
		try {
			int row = Integer.parseInt(parts[0].trim());
			int column = Integer.parseInt(parts[1].trim());
			if (row < 0 || column < 0) {
				return DataResult.error(() -> "Tile position can't be negative: \"" + string + "\"");
			}
			return DataResult.success(new TilePosition(row, column));
		} catch (NumberFormatException e) {
			return DataResult.error(() -> "Expected a tile position as \"row:column\", got \"" + string + "\"");
		}
	}

	public boolean isOnBoard(int rows, int columns) {
		return row < rows && column < columns;
	}

	public int slot(int columns) {
		return row * columns + column;
	}

	@Override
	public String toString() {
		return row + ":" + column;
	}
}
