package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.mixin.client.AbstractSoundInstanceAccess;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(Dist.CLIENT)
public record SoundVolumeModifier(
		Map<Holder<SoundEvent>, Float> volumes
) implements GameClientState {
	public static final MapCodec<SoundVolumeModifier> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(SoundEvent.CODEC, ExtraCodecs.NON_NEGATIVE_FLOAT).fieldOf("volumes").forGetter(SoundVolumeModifier::volumes)
	).apply(i, SoundVolumeModifier::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, SoundVolumeModifier> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.holderRegistry(Registries.SOUND_EVENT), ByteBufCodecs.FLOAT), SoundVolumeModifier::volumes,
			SoundVolumeModifier::new
	);

	@SubscribeEvent
	public static void onPlaySound(PlaySoundEvent event) {
		if (!(event.getSound() instanceof AbstractSoundInstance sound)) {
			return;
		}
		SoundVolumeModifier soundModifier = ClientGameStateManager.getOrNull(GameClientStateTypes.SOUND_VOLUME_MODIFIER);
		if (soundModifier == null) {
			return;
		}
		Holder.Reference<SoundEvent> soundEvent = BuiltInRegistries.SOUND_EVENT.get(sound.getLocation()).orElse(null);
		Float volumeModifier = soundModifier.volumes.get(soundEvent);
		if (volumeModifier != null) {
			AbstractSoundInstanceAccess soundAccess = (AbstractSoundInstanceAccess) sound;
			soundAccess.setBaseVolume(soundAccess.getBaseVolume() * volumeModifier);
		}
	}

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.SOUND_VOLUME_MODIFIER.get();
	}
}
