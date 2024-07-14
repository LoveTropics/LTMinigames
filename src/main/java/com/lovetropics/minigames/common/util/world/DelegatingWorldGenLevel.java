package com.lovetropics.minigames.common.util.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTickAccess;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class DelegatingWorldGenLevel implements WorldGenLevel {
	protected final WorldGenLevel parent;

	protected DelegatingWorldGenLevel(WorldGenLevel parent) {
		this.parent = parent;
	}

	@Override
	public long getSeed() {
		return parent.getSeed();
	}

	@Override
	public ServerLevel getLevel() {
		return parent.getLevel();
	}

	@Override
	public long nextSubTickCount() {
		return parent.nextSubTickCount();
	}

	@Override
	public LevelTickAccess<Block> getBlockTicks() {
		return parent.getBlockTicks();
	}

	@Override
	public LevelTickAccess<Fluid> getFluidTicks() {
		return parent.getFluidTicks();
	}

	@Override
	public LevelData getLevelData() {
		return parent.getLevelData();
	}

	@Override
	public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
		return parent.getCurrentDifficultyAt(pos);
	}

	@Nullable
	@Override
	public MinecraftServer getServer() {
		return parent.getServer();
	}

	@Override
	public ChunkSource getChunkSource() {
		return parent.getChunkSource();
	}

	@Override
	public RandomSource getRandom() {
		return parent.getRandom();
	}

	@Override
	public void playSound(@Nullable Player player, BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch) {
		parent.playSound(player, pos, sound, category, volume, pitch);
	}

	@Override
	public void addParticle(ParticleOptions particle, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		parent.addParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed);
	}

	@Override
	public void levelEvent(@Nullable Player player, int type, BlockPos pos, int data) {
		parent.levelEvent(player, type, pos, data);
	}

	@Override
	public void gameEvent(Holder<GameEvent> event, Vec3 position, GameEvent.Context context) {
		parent.gameEvent(event, position, context);
	}

	@Override
	public void gameEvent(@Nullable final Entity entity, final Holder<GameEvent> event, final BlockPos pos) {
		parent.gameEvent(entity, event, pos);
	}

	@Override
	public RegistryAccess registryAccess() {
		return parent.registryAccess();
	}

	@Override
	public FeatureFlagSet enabledFeatures() {
		return parent.enabledFeatures();
	}

	@Override
	public float getShade(Direction direction, boolean b) {
		return parent.getShade(direction, b);
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return parent.getLightEngine();
	}

	@Override
	public WorldBorder getWorldBorder() {
		return parent.getWorldBorder();
	}

	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos pos) {
		return parent.getBlockEntity(pos);
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		return parent.getBlockState(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return parent.getFluidState(pos);
	}

	@Override
	public List<Entity> getEntities(@Nullable Entity entity, AABB box, Predicate<? super Entity> predicate) {
		return parent.getEntities(entity, box, predicate);
	}

	@Override
	public <T extends Entity> List<T> getEntities(final EntityTypeTest<Entity, T> type, final AABB aabb, final Predicate<? super T> predicate) {
		return parent.getEntities(type, aabb, predicate);
	}

	@Override
	public List<? extends Player> players() {
		return parent.players();
	}

	@Nullable
	@Override
	public ChunkAccess getChunk(int x, int z, ChunkStatus requiredStatus, boolean create) {
		return parent.getChunk(x, z, requiredStatus, create);
	}

	@Override
	public int getHeight(Heightmap.Types type, int x, int z) {
		return parent.getHeight(type, x, z);
	}

	@Override
	public int getSkyDarken() {
		return parent.getSkyDarken();
	}

	@Override
	public BiomeManager getBiomeManager() {
		return parent.getBiomeManager();
	}

	@Override
	public Holder<Biome> getUncachedNoiseBiome(final int x, final int y, final int z) {
		return parent.getUncachedNoiseBiome(x, y, z);
	}

	@Override
	public boolean isClientSide() {
		return parent.isClientSide();
	}

	@Override
	public int getSeaLevel() {
		return parent.getSeaLevel();
	}

	@Override
	public DimensionType dimensionType() {
		return parent.dimensionType();
	}

	@Override
	public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft) {
		return parent.setBlock(pos, state, flags, recursionLeft);
	}

	@Override
	public boolean removeBlock(BlockPos pos, boolean isMoving) {
		FluidState fluid = getFluidState(pos);
		int flags = Block.UPDATE_ALL | (isMoving ? Block.UPDATE_MOVE_BY_PISTON : 0);
		return setBlock(pos, fluid.createLegacyBlock(), flags);
	}

	@Override
	public boolean destroyBlock(BlockPos pos, boolean dropBlock, @Nullable Entity entity, int recursionLeft) {
		BlockState block = getBlockState(pos);
		if (!block.isAir()) {
			FluidState fluid = getFluidState(pos);
			return setBlock(pos, fluid.createLegacyBlock(), Block.UPDATE_ALL, recursionLeft);
		}
		return false;
	}

	@Override
	public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> predicate) {
		return predicate.test(getBlockState(pos));
	}

	@Override
	public boolean isFluidAtPosition(final BlockPos pos, final Predicate<FluidState> predicate) {
		return parent.isFluidAtPosition(pos, predicate);
	}
}
