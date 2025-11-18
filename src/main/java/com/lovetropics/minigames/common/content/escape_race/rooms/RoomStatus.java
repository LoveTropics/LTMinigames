package com.lovetropics.minigames.common.content.escape_race.rooms;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum RoomStatus implements StringRepresentable {
	LOCKED("locked", 0),
	UNLOCKED("unlocked", 1),
	COMPLETED("completed", 2),;

	public static final Codec<RoomStatus> CODEC = StringRepresentable.fromEnum(RoomStatus::values);
	private static final IntFunction<RoomStatus> BY_ID = ByIdMap.continuous(
			RoomStatus::id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
	);
	public static final StreamCodec<ByteBuf, RoomStatus> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, RoomStatus::id);

	private final String name;
	private final int id;

	RoomStatus(String name, int id) {
		this.name = name;
		this.id = id;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public int id(){
		return this.id;
	}


}
