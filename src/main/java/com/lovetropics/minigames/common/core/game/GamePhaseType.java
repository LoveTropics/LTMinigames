package com.lovetropics.minigames.common.core.game;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

import java.util.function.IntFunction;

public enum GamePhaseType {
	PLAYING, // Players are loaded into and playing in a game world
	WAITING, // Players are loaded into the 'waiting world' before being loaded into a game world
	;

	private static final IntFunction<GamePhaseType> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
	public static final StreamCodec<ByteBuf, GamePhaseType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
}
