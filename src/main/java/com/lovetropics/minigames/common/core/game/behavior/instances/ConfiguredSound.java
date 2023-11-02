package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.ForgeRegistries;

public record ConfiguredSound(SoundEvent sound, SoundSource source, float volume, float pitch) {
	public static final Codec<ConfiguredSound> CODEC = RecordCodecBuilder.create(i -> i.group(
			ForgeRegistries.SOUND_EVENTS.getCodec().fieldOf("sound").forGetter(ConfiguredSound::sound),
			MoreCodecs.stringVariants(SoundSource.values(), SoundSource::getName).optionalFieldOf("source", SoundSource.AMBIENT).forGetter(ConfiguredSound::source),
			Codec.FLOAT.optionalFieldOf("volume", 1.0f).forGetter(ConfiguredSound::volume),
			Codec.FLOAT.optionalFieldOf("pitch", 1.0f).forGetter(ConfiguredSound::pitch)
	).apply(i, ConfiguredSound::new));
}
