package com.lovetropics.minigames.common.content.trash_dive;

import com.lovetropics.minigames.common.content.block.LoveTropicsBlocks;
import com.lovetropics.minigames.common.content.block.TrashBlock;
import com.lovetropics.minigames.common.content.block.TrashBlock.Attachment;
import com.lovetropics.minigames.common.content.block.TrashType;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Optional;

public record PlaceTrashBehavior(ResourceLocation positionData, int centerY, int range, int density) implements IGameBehavior {
	public static final MapCodec<PlaceTrashBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ResourceLocation.CODEC.fieldOf("positionData").forGetter(c -> c.positionData),
			Codec.INT.optionalFieldOf("centerY", 75).forGetter(c -> c.centerY),
			Codec.INT.optionalFieldOf("range", 50).forGetter(c -> c.range),
			Codec.INT.optionalFieldOf("density", 4).forGetter(c -> c.density)
	).apply(i, PlaceTrashBehavior::new));

	private static final TrashType[] TRASH_TYPES = TrashType.values();

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Long2ObjectMap<LongList> trashByChunk = loadTrashByChunk(game);

		events.listen(GameWorldEvents.CHUNK_LOAD, (chunk) -> {
			LongList positions = trashByChunk.remove(chunk.getPos().toLong());
			if (positions == null) {
				return;
			}

			RandomSource random = game.random();
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

			LongListIterator iterator = positions.iterator();
			while (iterator.hasNext()) {
				pos.set(iterator.nextLong());

				int count = random.nextInt(density);
				for (int i = 0; i < count; i++) {
					// range defines 3 standard deviations from the mean (centerY)
					pos.setY(centerY + (int) ((random.nextGaussian() / 3) * range));
					tryPlaceTrash(chunk, pos, random);
				}
			}
		});
	}

	private Long2ObjectMap<LongList> loadTrashByChunk(IGamePhase game) {
		Optional<Resource> resource = game.server().getResourceManager().getResource(positionData);
		if (resource.isEmpty()) {
			throw new GameException(Component.literal("Missing trash position data"));
		}

		LongBuffer candidatePositions;
		try (InputStream in = resource.get().open()) {
			final byte[] data = new byte[8];
			final ByteBuffer buf = ByteBuffer.allocate(in.available());
			candidatePositions = buf.asLongBuffer();
			while (in.read(data) > 0) {
				buf.put(data);
			}
			candidatePositions.position(0);
		} catch (Exception e) {
			throw new GameException(Component.literal("Unexpected error reading trash position data"), e);
		}

		Long2ObjectMap<LongList> trashByChunk = new Long2ObjectOpenHashMap<>();
		while (candidatePositions.hasRemaining()) {
			long candidatePos = candidatePositions.get();

			long sectionKey = SectionPos.blockToSection(candidatePos);
			long chunkKey = ChunkPos.asLong(SectionPos.x(sectionKey), SectionPos.z(sectionKey));

			trashByChunk.computeIfAbsent(chunkKey, l -> new LongArrayList()).add(candidatePos);
		}

		return trashByChunk;
	}

	private void tryPlaceTrash(ChunkAccess chunk, BlockPos pos, RandomSource random) {
		if (chunk.getBlockState(pos).is(Blocks.WATER)) {
			TrashType trashType = Util.getRandom(TRASH_TYPES, random);
			chunk.setBlockState(pos, LoveTropicsBlocks.TRASH.get(trashType).getDefaultState()
							.setValue(TrashBlock.WATERLOGGED, true)
							.setValue(TrashBlock.ATTACHMENT, Block.canSupportRigidBlock(chunk, pos.below()) ? Attachment.FLOOR : Attachment.random(random))
							.setValue(TrashBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random))
			);
		}
	}
}
