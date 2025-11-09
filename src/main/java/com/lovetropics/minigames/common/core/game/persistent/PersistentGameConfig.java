package com.lovetropics.minigames.common.core.game.persistent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record PersistentGameConfig(
		ResourceLocation id,
		ResourceLocation dimension,
		List<PersistentBehaviorTemplate> behaviors
) {
	public static Codec<PersistentGameConfig> codec(ResourceLocation id) {
		return RecordCodecBuilder.create(instance -> instance.group(
				ResourceLocation.CODEC.fieldOf("dimension").forGetter(PersistentGameConfig::dimension),
				PersistentBehaviorTemplate.CODEC.listOf().fieldOf("behaviors").forGetter(PersistentGameConfig::behaviors)
		).apply(instance, (dim, behaviors) -> {
			return new PersistentGameConfig(id, dim, behaviors);
		}));
	}
}
