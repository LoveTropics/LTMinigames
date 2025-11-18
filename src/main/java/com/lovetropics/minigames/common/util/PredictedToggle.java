package com.lovetropics.minigames.common.util;

import com.lovetropics.minigames.LoveTropics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public record PredictedToggle(
		boolean enabled,
		// When enabled: gameTime from which we are ticking up from 0
		// When enabled: gameTime at which we will tick down to 0
		long time
) {
	public static final PredictedToggle DISABLED = new PredictedToggle(false, 0);

	public static final DeferredRegister<EntityDataSerializer<?>> REGISTER = DeferredRegister.create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, LoveTropics.ID);

	public static final StreamCodec<ByteBuf, PredictedToggle> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, PredictedToggle::enabled,
			ByteBufCodecs.VAR_LONG, PredictedToggle::time,
			PredictedToggle::new
	);
	public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<PredictedToggle>> SERIALIZER = REGISTER.register("predicted_toggle", () -> EntityDataSerializer.forValueType(STREAM_CODEC));

	public static PredictedToggle of(long gameTime, int currentTicks, boolean enabled) {
		if (currentTicks == 0 && !enabled) {
			return DISABLED;
		}
		if (enabled) {
			long tickUpStartTime = gameTime - currentTicks;
			return new PredictedToggle(true, tickUpStartTime);
		} else {
			long tickDownEndTime = gameTime + currentTicks;
			return new PredictedToggle(false, tickDownEndTime);
		}
	}

	public int getCurrentTicks(long gameTime, int totalTicks) {
		if (enabled) {
			return (int) Mth.clamp(gameTime - time, 0, totalTicks);
		} else {
			return (int) Mth.clamp(time - gameTime, 0, totalTicks);
		}
	}

	public float getCurrentProgress(long gameTime, float partialTicks, int totalTicks) {
		if (enabled) {
			return Mth.clamp((gameTime - time) + partialTicks, 0.0f, totalTicks) / totalTicks;
		} else {
			return Mth.clamp((time - gameTime) - partialTicks, 0.0f, totalTicks) / totalTicks;
		}
	}
}
