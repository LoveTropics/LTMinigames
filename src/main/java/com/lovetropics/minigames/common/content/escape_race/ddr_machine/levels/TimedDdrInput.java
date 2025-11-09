package com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels;

import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record TimedDdrInput(long tick, DdrInput input) {
	public static final StreamCodec<ByteBuf, TimedDdrInput> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_LONG, TimedDdrInput::tick,
			DdrInput.STREAM_CODEC, TimedDdrInput::input,
			TimedDdrInput::new
	);
}
