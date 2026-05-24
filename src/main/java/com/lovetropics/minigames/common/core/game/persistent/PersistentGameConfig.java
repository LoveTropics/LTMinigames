package com.lovetropics.minigames.common.core.game.persistent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record PersistentGameConfig(
		Identifier id,
		Identifier dimension,
		List<PersistentBehaviorTemplate> behaviors
) {
	public static Codec<PersistentGameConfig> codec(Identifier id) {
		return RecordCodecBuilder.create(instance -> instance.group(
				Identifier.CODEC.fieldOf("dimension").forGetter(PersistentGameConfig::dimension),
				PersistentBehaviorTemplate.CODEC.listOf().fieldOf("behaviors").forGetter(PersistentGameConfig::behaviors)
		).apply(instance, (dim, behaviors) -> {
			return new PersistentGameConfig(id, dim, behaviors);
		}));
	}
}
