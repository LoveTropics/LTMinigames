package com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum DdrLevelDifficulty implements StringRepresentable {
	EASY("easy", 0.2f, 0),
	MEDIUM("medium", 0.4f, 1),
	HARD("hard", 0.5f, 2);

	public static Codec<DdrLevelDifficulty> CODEC = StringRepresentable.fromEnum(DdrLevelDifficulty::values);
	private static final IntFunction<DdrLevelDifficulty> BY_ID = ByIdMap.continuous(
			DdrLevelDifficulty::id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
	);
	public static final StreamCodec<ByteBuf, DdrLevelDifficulty> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, DdrLevelDifficulty::id);

	private final String name;
	private final float scoreMultiplier;
	private final int id;

	DdrLevelDifficulty(String name, float scoreMultiplier, int id) {
		this.name = name;
		this.scoreMultiplier = scoreMultiplier;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public float getScoreMultiplier() {
		return scoreMultiplier;
	}

	public int id(){
		return this.id;
	}

	@Override
	public String getSerializedName() {
		return getName();
	}
}
