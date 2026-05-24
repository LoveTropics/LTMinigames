package com.lovetropics.minigames.common.core.game.behavior.instances.world;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Collection;
import java.util.List;

public class FillChestsByMarkerBehavior extends ChunkGeneratingBehavior {
	public static final MapCodec<FillChestsByMarkerBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BuiltInRegistries.BLOCK.byNameCodec().fieldOf("marker").forGetter(c -> c.marker),
			WeightedList.codec(ResourceKey.codec(Registries.LOOT_TABLE)).fieldOf("loot_tables").forGetter(c -> c.lootTables),
			Codec.FLOAT.optionalFieldOf("percentage", 1.0f).forGetter(c -> c.percentage),
			Codec.INT.optionalFieldOf("max_per_chunk", Integer.MAX_VALUE).forGetter(c -> c.maxPerChunk),
			Codec.INT.optionalFieldOf("max_per_section", Integer.MAX_VALUE).forGetter(c -> c.maxPerSection)
	).apply(i, FillChestsByMarkerBehavior::new));

	private final Block marker;
	private final WeightedList<ResourceKey<LootTable>> lootTables;
	private final float percentage;
	private final int maxPerChunk;
	private final int maxPerSection;

	public FillChestsByMarkerBehavior(Block marker, WeightedList<ResourceKey<LootTable>> lootTables, float percentage, int maxPerChunk, int maxPerSection) {
		this.marker = marker;
		this.lootTables = lootTables;
		this.percentage = percentage;
		this.maxPerChunk = maxPerChunk;
		this.maxPerSection = maxPerSection;
	}

	@Override
	protected void generateChunk(IGamePhase game, ServerLevel world, LevelChunk chunk) {
		ObjectArrayList<Chest> chests = collectChests(chunk);
		if (chests.isEmpty()) {
			return;
		}

		RandomSource random = world.getRandom();
		Util.shuffle(chests, random);

		if (percentage < 1.0f) {
			int index = Mth.ceil(chests.size() * percentage);
			trimChests(world, chests, index);
		}

		trimChests(world, chests, maxPerChunk);

		for (ObjectList<Chest> sectionChests : partitionBySection(chests)) {
			trimChests(world, sectionChests, maxPerSection);

			for (Chest chest : sectionChests) {
				world.setBlockAndUpdate(chest.pos, Blocks.AIR.defaultBlockState());
				lootTables.getRandom(random).ifPresent(lootTable -> {
					setChest(world, chest.pos.below(), chest, lootTable);
				});
			}
		}
	}

	private static Collection<ObjectList<Chest>> partitionBySection(ObjectList<Chest> chests) {
		if (chests.isEmpty()) {
			return List.of();
		}
		Int2ObjectMap<ObjectList<Chest>> partitioned = new Int2ObjectArrayMap<>();
		for (Chest chest : chests) {
			int sectionY = SectionPos.blockToSectionCoord(chest.pos.getY());
			partitioned.computeIfAbsent(sectionY, i -> new ObjectArrayList<>()).add(chest);
		}
		return partitioned.values();
	}

	private static void trimChests(ServerLevel level, ObjectList<Chest> positions, int count) {
		for (int i = positions.size() - 1; i >= count; i--) {
			BlockPos pos = positions.remove(i).pos();
			level.removeBlock(pos, false);
			level.removeBlock(pos.below(), false);
		}
	}

	private ObjectArrayList<Chest> collectChests(LevelChunk chunk) {
		ObjectArrayList<Chest> chestPositions = new ObjectArrayList<>();
		for (BlockPos pos : chunk.getBlockEntitiesPos()) {
			if (chunk.getBlockEntity(pos) instanceof ChestBlockEntity blockEntity) {
				if (chunk.getBlockState(pos.below()).is(marker)) {
					chestPositions.add(new Chest(pos, blockEntity.getBlockState()));
				}
			}
		}
		return chestPositions;
	}

	private void setChest(ServerLevel world, BlockPos pos, Chest chest, ResourceKey<LootTable> lootTable) {
		world.setBlockAndUpdate(pos, chest.blockState);
		if (world.getBlockEntity(pos) instanceof RandomizableContainer blockEntity) {
			blockEntity.setLootTable(lootTable, world.getRandom().nextLong());
		}
		world.getChunkSource().getLightEngine().checkBlock(pos);
	}

	private record Chest(BlockPos pos, BlockState blockState) {
	}
}
