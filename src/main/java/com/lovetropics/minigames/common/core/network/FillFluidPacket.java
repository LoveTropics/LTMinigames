package com.lovetropics.minigames.common.core.network;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.util.FluidFiller;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FillFluidPacket(FluidFiller.Type fillType, BlockPos min, BlockPos max) implements CustomPacketPayload {
	public static final Type<FillFluidPacket> TYPE = new Type<>(LoveTropics.id("fill_fluid"));

	public static final StreamCodec<ByteBuf, FillFluidPacket> STREAM_CODEC = StreamCodec.composite(
			FluidFiller.Type.STREAM_CODEC, FillFluidPacket::fillType,
			BlockPos.STREAM_CODEC, FillFluidPacket::min,
			BlockPos.STREAM_CODEC, FillFluidPacket::max,
			FillFluidPacket::new
	);

	public static void handle(final FillFluidPacket packet, final IPayloadContext context) {
		final ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		final BlockPos min = packet.min;
		final BlockPos max = packet.max;
		final int minChunkX = SectionPos.blockToSectionCoord(min.getX());
		final int minChunkZ = SectionPos.blockToSectionCoord(min.getZ());
		final int maxChunkX = SectionPos.blockToSectionCoord(max.getX());
		final int maxChunkZ = SectionPos.blockToSectionCoord(max.getZ());
		for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
			for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
				final LevelChunk chunk = level.getChunk(chunkX, chunkZ);
				final ChunkPos chunkPos = chunk.getPos();
				if (chunkPos.x() != chunkX || chunkPos.z() != chunkZ) {
					// TODO: Some kind of race condition can happen here with leaving the dimension while rising is happening :(
					LoveTropics.LOGGER.error("Tried to fill chunk with fluid, but position didn't match. Expected [{}, {}] but got {}", chunkX, chunkZ, chunkPos);
					return;
				}
				FluidFiller.fillChunk(packet.fillType, min.getX(), min.getZ(), max.getX(), max.getZ(), chunk, min.getY(), max.getY());
			}
		}
	}

	@Override
	public Type<FillFluidPacket> type() {
		return TYPE;
	}
}
