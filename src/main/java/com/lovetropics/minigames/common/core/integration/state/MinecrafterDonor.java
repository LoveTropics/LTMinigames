package com.lovetropics.minigames.common.core.integration.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;

import java.util.List;
import java.util.UUID;

public record MinecrafterDonor(
	double total,
	String minecraftName,
	UUID minecraftUuid
) {

	public static final MapCodec<MinecrafterDonor> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.DOUBLE.optionalFieldOf("total", 0.0).forGetter(MinecrafterDonor::total),
			Codec.STRING.optionalFieldOf("minecraft_name", "").forGetter(MinecrafterDonor::minecraftName),
			UUIDUtil.AUTHLIB_CODEC.lenientOptionalFieldOf("minecraft_uuid", Util.NIL_UUID).forGetter(MinecrafterDonor::minecraftUuid)
	).apply(i, MinecrafterDonor::new));

	public static final Codec<MinecrafterDonor> CODEC = MAP_CODEC.codec();

	public static final Codec<List<MinecrafterDonor>> LIST_CODEC = MinecrafterDonor.CODEC.listOf();
}
