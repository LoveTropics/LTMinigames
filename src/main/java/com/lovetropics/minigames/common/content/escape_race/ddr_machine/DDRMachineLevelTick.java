package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public record DDRMachineLevelTick(boolean forward, boolean left, boolean back, boolean right) {
	public static final MapCodec<DDRMachineLevelTick> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.fieldOf("forward").forGetter(DDRMachineLevelTick::forward),
			Codec.BOOL.fieldOf("left").forGetter(DDRMachineLevelTick::left),
			Codec.BOOL.fieldOf("back").forGetter(DDRMachineLevelTick::back),
			Codec.BOOL.fieldOf("right").forGetter(DDRMachineLevelTick::right)
	).apply(i, DDRMachineLevelTick::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, DDRMachineLevelTick> STREAM_CODEC = StreamCodec.of((output, definition) -> definition.encode(output), DDRMachineLevelTick::decode);

	public static DDRMachineLevelTick decode(RegistryFriendlyByteBuf buffer) {
		boolean forward = buffer.readBoolean();
		boolean left = buffer.readBoolean();
		boolean back = buffer.readBoolean();
		boolean right = buffer.readBoolean();
		return new DDRMachineLevelTick(forward, left, back, right);
	}

	public void encode(RegistryFriendlyByteBuf buffer) {
		buffer.writeBoolean(forward);
		buffer.writeBoolean(left);
		buffer.writeBoolean(back);
		buffer.writeBoolean(right);
	}

	@Override
	public @NotNull String toString() {
		return (forward ? "FORWARD\n" : "") + (left ? "LEFT\n" : "") + (back ? "BACK\n" : "") + (right ? "RIGHT\n" : "");
	}
}
