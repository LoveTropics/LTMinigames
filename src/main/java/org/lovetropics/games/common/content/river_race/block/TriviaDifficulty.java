package org.lovetropics.games.common.content.river_race.block;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum TriviaDifficulty implements StringRepresentable {
	EASY(0, "easy"),
	MEDIUM(1, "medium"),
	HARD(2, "hard"),
	;

	public static final Codec<TriviaDifficulty> CODEC = StringRepresentable.fromEnum(TriviaDifficulty::values);
	public static final StreamCodec<ByteBuf, TriviaDifficulty> STREAM_CODEC = ByteBufCodecs.idMapper(
			ByIdMap.continuous(d -> d.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO),
			d -> d.id
	);

	private final int id;
	private final String name;

	TriviaDifficulty(int id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
