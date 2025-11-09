package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Input;

public record DdrInput(
		boolean forward,
		boolean back,
		boolean left,
		boolean right
) {
	public static final MapCodec<DdrInput> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.fieldOf("forward").forGetter(DdrInput::forward),
			Codec.BOOL.fieldOf("back").forGetter(DdrInput::back),
			Codec.BOOL.fieldOf("left").forGetter(DdrInput::left),
			Codec.BOOL.fieldOf("right").forGetter(DdrInput::right)
	).apply(i, DdrInput::new));
	public static final Codec<DdrInput> CODEC = MAP_CODEC.codec();

	private static final int FORWARD_BIT = 1;
	private static final int BACK_BIT = 2;
	private static final int LEFT_BIT = 4;
	private static final int RIGHT_BIT = 8;

	public static final StreamCodec<ByteBuf, DdrInput> STREAM_CODEC = ByteBufCodecs.BYTE.map(
			flags -> new DdrInput(
					(flags & FORWARD_BIT) != 0,
					(flags & BACK_BIT) != 0,
					(flags & LEFT_BIT) != 0,
					(flags & RIGHT_BIT) != 0
			),
			input -> (byte) ((input.forward ? FORWARD_BIT : 0)
					| (input.back ? BACK_BIT : 0)
					| (input.left ? LEFT_BIT : 0)
					| (input.right ? RIGHT_BIT : 0))
	);

	public static final DdrInput NONE = new DdrInput(false, false, false, false);

	public static DdrInput fromKeyPresses(Input keyPresses) {
		return new DdrInput(
				keyPresses.forward(),
				keyPresses.backward(),
				keyPresses.left(),
				keyPresses.right()
		);
	}

	public boolean overlaps(DdrInput other) {
		return (forward && other.forward)
				|| (back && other.back)
				|| (left && other.left)
				|| (right && other.right);
	}

	public DdrInput subtract(DdrInput other) {
		return new DdrInput(
				forward && !other.forward,
				back && !other.back,
				left && !other.left,
				right && !other.right
		);
	}

	public boolean isEmpty() {
		return equals(NONE);
	}

	@Override
	public String toString() {
		return (forward ? "FORWARD\n" : "") + (left ? "LEFT\n" : "") + (back ? "BACK\n" : "") + (right ? "RIGHT\n" : "");
	}
}
