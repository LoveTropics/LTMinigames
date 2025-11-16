package com.lovetropics.minigames.common.core.integration.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record MinecrafterDonor(
	double total,
	String minecraftName,
	String minecraftUuid
) {

	public static final MapCodec<MinecrafterDonor> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.DOUBLE.optionalFieldOf("total", 0.0).forGetter(MinecrafterDonor::total),
			Codec.STRING.optionalFieldOf("minecraft_name", "").forGetter(MinecrafterDonor::minecraftName),
			Codec.STRING.optionalFieldOf("minecraft_uuid", "").forGetter(MinecrafterDonor::minecraftUuid)
	).apply(i, MinecrafterDonor::new));

	public static final Codec<List<MinecrafterDonor>> LIST_CODEC = MinecrafterDonor.CODEC.listOf();

	public static final Codec<MinecrafterDonor> CODEC = MAP_CODEC.codec();
}
