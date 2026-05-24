package com.lovetropics.minigames.client.particle_line;

import com.lovetropics.minigames.LoveTropics;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DrawParticleLineMessage(ParticleOptions particle, Vec3 from, Vec3 to, float spacing) implements CustomPacketPayload {
	public static final Type<DrawParticleLineMessage> TYPE = new Type<>(LoveTropics.id("draw_particle_line"));

	private static final StreamCodec<ByteBuf, Vec3> VEC3_STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.DOUBLE, Vec3::x,
			ByteBufCodecs.DOUBLE, Vec3::y,
			ByteBufCodecs.DOUBLE, Vec3::z,
			Vec3::new
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, DrawParticleLineMessage> STREAM_CODEC = StreamCodec.composite(
			ParticleTypes.STREAM_CODEC, DrawParticleLineMessage::particle,
			VEC3_STREAM_CODEC, DrawParticleLineMessage::from,
			VEC3_STREAM_CODEC, DrawParticleLineMessage::to,
			ByteBufCodecs.FLOAT, DrawParticleLineMessage::spacing,
			DrawParticleLineMessage::new
	);

	public static void handle(DrawParticleLineMessage message, IPayloadContext context) {
		ParticleLineSpawner.spawnParticleLine(message.particle, message.from, message.to, message.spacing);
	}

	@Override
	public Type<DrawParticleLineMessage> type() {
		return TYPE;
	}
}
