package com.lovetropics.minigames.common.content.biodiversity_blitz.entity;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl.BbCreeperEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl.BbDrownedEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl.BbHuskEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl.BbPillagerEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl.BbVindicatorEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl.BbZoglinEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl.BbZombieEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl.BbZombiePiglinEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

public final class BbMobSpawner {
	public static Set<Entity> spawnWaveEntities(ServerLevel world, RandomSource random, Plot plot, int count, int waveIndex, WaveSelector waveSelector, BbEvents.ModifyWaveMobs modifier) {
		Set<Entity> entities = Collections.newSetFromMap(new WeakHashMap<>());

		modifier.modifyWave(entities, random, world, plot, waveIndex);

		for (Entity entity : entities) {
			BlockBox mobSpawn = Util.getRandom(plot.mobSpawns, random);

			spawnEntity(world, random, mobSpawn, plot, entity);
		}

		for (int i = 0; i < count; i++) {
			int plotIdx = random.nextInt(plot.mobSpawns.size());
			Mob entity = waveSelector.selectEntityForWave(random, world, plot, plotIdx, waveIndex);

			entities.add(entity);
			spawnEntity(world, random, plot.mobSpawns.get(plotIdx), plot, entity);
		}

		return entities;
	}

	public static void spawnEntity(ServerLevel world, RandomSource random, BlockBox mobSpawn, Plot plot, Entity entity) {
		AABB spawnBounds = mobSpawn.asAabb().inflate(-entity.getBbWidth(), 0.0f, -entity.getBbWidth());
		double x = spawnBounds.minX + spawnBounds.getXsize() * random.nextFloat();
		double y = spawnBounds.minY;
		double z = spawnBounds.minZ + spawnBounds.getZsize() * random.nextFloat();
		BlockPos pos = BlockPos.containing(x, y, z);
		Direction direction = plot.forward.getOpposite();
		entity.snapTo(x, y, z, direction.toYRot(), 0);

		world.addFreshEntity(entity);

		if (entity instanceof Mob mob) {
			mob.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
		}
	}

	// TODO: data-drive, more entity types & getting harder as time goes on
	public static Mob selectEntityForWave(RandomSource random, Level world, Plot plot, int plotIndex, int waveIndex) {
		PlotWaveState waveState = plot.waveState;

		// Devious, awful, no good hardcoded mob spawning

		boolean isJungle = plotIndex <= 1;
		boolean isWater = plotIndex == 2 || plotIndex == 3;

		if (!isWater && random.nextInt(6) == 0 && waveIndex > 4 && plot.nextCurrencyIncrement >= 4) {
			return new BbZoglinEntity(EntityType.ZOGLIN, world, plot);
		}

		if (!isWater && random.nextInt(5) == 0 && waveIndex > 4 && plot.nextCurrencyIncrement >= 3) {
			return new BbVindicatorEntity(EntityType.VINDICATOR, world, plot);
		}

		if (!isWater && random.nextInt(5) == 0 && waveIndex > 4 && plot.nextCurrencyIncrement >= 2) {
			return new BbZombiePiglinEntity(EntityType.ZOMBIFIED_PIGLIN, world, plot);
		}

		if (random.nextInt(6) == 0 && waveIndex > 4 && plot.nextCurrencyIncrement >= 3) {
			return new BbCreeperEntity(EntityType.CREEPER, world, plot);
		}

		if (random.nextInt(3) == 0 && waveIndex > 2 && plot.nextCurrencyIncrement >= 2) {
			return new BbPillagerEntity(EntityType.PILLAGER, world, plot);
		}

		// Zombies in jungle
		if (isJungle) {
			return new BbZombieEntity(EntityType.ZOMBIE, world, plot);
		}

		// Drowned in water
		if (plotIndex == 2 || plotIndex == 3) {
			return new BbDrownedEntity(EntityType.DROWNED, world, plot);
		}

		// Husks in desert
		return new BbHuskEntity(EntityType.HUSK, world, plot);
	}

	@FunctionalInterface
	public interface WaveSelector {
		Mob selectEntityForWave(RandomSource random, Level world, Plot plot, int plotIndex, int waveIndex);
	}

	public enum BbEntityTypes implements StringRepresentable {
		CREEPER(EntityType.CREEPER, BbCreeperEntity::new, "Creeper"),
		PILLAGER(EntityType.PILLAGER, BbPillagerEntity::new, "Pillager"),
		VINDICATOR(EntityType.VINDICATOR, BbVindicatorEntity::new, "Vindicator"),
		PIGMAN(EntityType.ZOMBIFIED_PIGLIN, BbZombiePiglinEntity::new, "Piglin"),
		ZOGLIN(EntityType.ZOGLIN, BbZoglinEntity::new, "Zoglin"),
		HUSK(EntityType.HUSK, BbHuskEntity::new, "Husk");

		public static final Codec<BbEntityTypes> CODEC = StringRepresentable.fromEnum(BbEntityTypes::values);

		private final EntityType<?> entityType;
		private final Creator<?> creator;
		private final String englishName;

		<T extends Mob> BbEntityTypes(EntityType<T> entityType, Creator<T> creator, String englishName) {
			this.creator = creator;
			this.entityType = entityType;
			this.englishName = englishName;
		}

		public Mob create(Level level, Plot plot) {
			return creator.create((EntityType) entityType, level, plot);
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}

		public MutableComponent getName() {
			return Component.translatable(getTranslationKey());
		}

		public String getTranslationKey() {
			return "ltminigames.bb_entities." + getSerializedName();
		}

		public String getEnglishName() {
			return englishName;
		}

		interface Creator<T extends Mob> {
			Mob create(EntityType<T> type, Level level, Plot plot);
		}
	}
}
