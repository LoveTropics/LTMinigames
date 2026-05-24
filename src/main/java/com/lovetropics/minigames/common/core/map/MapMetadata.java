package com.lovetropics.minigames.common.core.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;

public record MapMetadata(Identifier id, MapWorldSettings settings, MapRegions regions) {
	public static final Codec<MapMetadata> CODEC = RecordCodecBuilder.create(i -> i.group(
			Identifier.CODEC.fieldOf("id").forGetter(MapMetadata::id),
			MapWorldSettings.CODEC.fieldOf("settings").forGetter(MapMetadata::settings),
			MapRegions.CODEC.fieldOf("regions").forGetter(MapMetadata::regions)
	).apply(i, MapMetadata::new));

	public CompoundTag write() {
		return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
	}

	public static MapMetadata read(CompoundTag root) {
		return CODEC.parse(NbtOps.INSTANCE, root).getOrThrow();
	}
}
