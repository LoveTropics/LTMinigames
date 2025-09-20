package com.lovetropics.minigames.common.core.map;

import com.lovetropics.minigames.LoveTropics;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.dimension.DimensionDefaults;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class VoidChunkGenerator extends ChunkGenerator {
	public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> REGISTER = DeferredRegister.create(Registries.CHUNK_GENERATOR, LoveTropics.ID);

	public static final MapCodec<VoidChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Biome.CODEC.stable().fieldOf("biome").forGetter(g -> g.biome)
	).apply(i, i.stable(VoidChunkGenerator::new)));

	static {
		REGISTER.register("void", () -> CODEC);
	}

	private final Holder<Biome> biome;

	public VoidChunkGenerator(final Holder<Biome> biome) {
		super(new FixedBiomeSource(biome));
		this.biome = biome;
	}

	public VoidChunkGenerator(Registry<Biome> biomeRegistry) {
		this(biomeRegistry, Biomes.THE_VOID);
	}

	public VoidChunkGenerator(MinecraftServer server) {
		this(server.registryAccess().lookupOrThrow(Registries.BIOME));
	}

	public VoidChunkGenerator(Registry<Biome> biomeRegistry, ResourceKey<Biome> biome) {
		this(biomeRegistry.getOrThrow(biome));
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	@Override
	public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState structureState, StructureManager structureManager, ChunkAccess chunk, StructureTemplateManager structureTemplateManager, ResourceKey<Level> level) {
	}

	@Override
	public void createReferences(WorldGenLevel world, StructureManager structures, ChunkAccess chunk) {
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(final Blender blender, final RandomState randomState, final StructureManager structures, final ChunkAccess chunk) {
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public int getSeaLevel() {
		return 0;
	}

	@Override
	public int getMinY() {
		return 0;
	}

	@Override
	public int getBaseHeight(final int x, final int z, final Heightmap.Types heightmap, final LevelHeightAccessor level, final RandomState randomState) {
		return level.getMinY();
	}

	@Override
	public NoiseColumn getBaseColumn(final int x, final int z, final LevelHeightAccessor level, final RandomState randomState) {
		final BlockState[] blocks = new BlockState[level.getHeight()];
		Arrays.fill(blocks, Blocks.AIR.defaultBlockState());
		return new NoiseColumn(level.getMinY(), blocks);
	}

	@Override
	public void addDebugScreenInfo(final List<String> info, final RandomState randomState, final BlockPos pos) {
	}

	@Override
	public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
	}

	@Override
	public void buildSurface(final WorldGenRegion region, final StructureManager structures, final RandomState randomState, final ChunkAccess chunk) {
	}

	@Override
	public void spawnOriginalMobs(final WorldGenRegion region) {
	}

	@Override
	public int getGenDepth() {
		return DimensionDefaults.OVERWORLD_LEVEL_HEIGHT;
	}
}
