package org.lovetropics.games.common.content.bingo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * LTMinigames
 * FIRE EMOJI, FIRE EMOJI, FIRE EMOJI
 */
public record BingoTileType(
		String name,
		int color,
		int lockedColor,
		int completedColor,
		int borderColor,
		int lockedBorderColor,
		int completedBorderColor,
		int textColor,
		int lockedTextColor,
		int completedTextColor
) {

	public static final Codec<BingoTileType> CODEC = Codec.STRING.comapFlatMap(name -> DataResult.success(getTileType(name)), BingoTileType::name);

	public static final Map<String, BingoTileType> REGISTRY = new HashMap<>();

	public static final BingoTileType COLLECT_ITEM = register(
			"collect_item",
			0xAA222222,
			0x66000000,
			0xAA61be29,
			0xFFFFFFFF,
			0xFF555555,
			0xFFFFFFFF,
			0xFFFFFFFF,
			0xFF888888,
			0xFFFFFFFF
	);

	public static final BingoTileType COLUMN_MULTIPLIER = register(
			"column_multiplier",
			0xFF45293F,
			0xFF2E222F,
			0xAAFFC033,
			0xAAE95D21,
			0xFF555555,
			0xFFFFC033,
			0xFFFFFFFF,
			0xFF888888,
			0xFFFFFFFF
	);


	public static final BingoTileType DEFAULT = COLLECT_ITEM;

	public static BingoTileType getTileType(String name) {
		return REGISTRY.getOrDefault(name, DEFAULT);
	}

	// First three are for the tile fill, the rest should be self-explanatory.
	public static BingoTileType register(String name, int color, int lockedColor, int completedColor, int borderColor, int lockedBorderColor, int completedBorderColor, int textColor, int lockedTextColor, int completedTextColor) {
		BingoTileType tileType = new BingoTileType(name, color, lockedColor, completedColor, borderColor, lockedBorderColor, completedBorderColor, textColor, lockedTextColor, completedTextColor);
		REGISTRY.put(name, tileType);
		return tileType;
	}
}
