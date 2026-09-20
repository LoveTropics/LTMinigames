package org.lovetropics.games.common.core.network;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.util.FluidFiller;
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

	public static void handle(FillFluidPacket packet, IPayloadContext context) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		BlockPos min = packet.min;
		BlockPos max = packet.max;
		int minChunkX = SectionPos.blockToSectionCoord(min.getX());
		int minChunkZ = SectionPos.blockToSectionCoord(min.getZ());
		int maxChunkX = SectionPos.blockToSectionCoord(max.getX());
		int maxChunkZ = SectionPos.blockToSectionCoord(max.getZ());
		for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
			for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
				LevelChunk chunk = level.getChunk(chunkX, chunkZ);
				ChunkPos chunkPos = chunk.getPos();
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
