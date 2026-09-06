package com.lovetropics.minigames.common.content.biodiversity_blitz.plot;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.PlotWalls;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.PlotWaveState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantMap;
import com.lovetropics.minigames.common.core.game.map.RegionPattern;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.phys.AABB;

import org.jspecify.annotations.Nullable;
import java.util.List;

public final class Plot {
	public final GameTeamKey team;
	public final BlockBox bounds;
	public final BlockBox plantBounds;
	public final BlockBox floorBounds;
	public final BlockBox spawn;
	public final BlockBox shop;
	public final BlockBox plantShop;
	public final BlockBox mobShop;
	public final List<BlockBox> mobSpawns;

	public final Direction forward;

	public final PlantMap plants = new PlantMap();
	public final PlotWalls walls;

	public int nextCurrencyIncrement;
	public final PlotWaveState waveState = new PlotWaveState();

	private Plot(
			GameTeamKey team,
			BlockBox bounds, BlockBox plantBounds, BlockBox floorBounds,
			BlockBox spawn, BlockBox shop, BlockBox plantShop, BlockBox mobShop,
			List<BlockBox> mobSpawns,
			Direction forward
	) {
		this.team = team;
		this.bounds = bounds;
		this.plantBounds = plantBounds;
		this.floorBounds = floorBounds;
		this.spawn = spawn;
		this.shop = shop;
		this.plantShop = plantShop;
		this.mobShop = mobShop;
		this.mobSpawns = mobSpawns;
		this.forward = forward;

		AABB walls = bounds.asAabb();
		for (BlockBox mobSpawn : mobSpawns) {
			walls = walls.minmax(mobSpawn.asAabb());
		}
		this.walls = new PlotWalls(walls);
	}

	public static Plot create(LevelHeightAccessor level, GameTeamKey team, Config config, RegionKeys regionKeys, MapRegions regions) {
		BlockBox plantBounds = regionKeys.plot.getOrThrow(regions, config.key);
		BlockBox bounds = BlockBox.of(
				new BlockPos(plantBounds.min().getX(), level.getMinY(), plantBounds.min().getZ()),
				new BlockPos(plantBounds.max().getX(), level.getMaxY(), plantBounds.max().getZ())
		);
		BlockBox floorBounds = BlockBox.of(
				new BlockPos(plantBounds.min().getX(), plantBounds.min().getY() - 1, plantBounds.min().getZ()),
				new BlockPos(plantBounds.max().getX(), plantBounds.min().getY() - 1, plantBounds.max().getZ())
		);
		BlockBox spawn = regionKeys.spawn.getOrThrow(regions, config.key);
		BlockBox shop = regionKeys.shop.getOrThrow(regions, config.key);
		BlockBox plantShop = regionKeys.plantShop.getOrThrow(regions, config.key);
		BlockBox mobShop = regionKeys.mobShop.getOrThrow(regions, config.key);
		List<BlockBox> mobSpawn = regionKeys.mobSpawns.stream().map(pattern -> pattern.getOrThrow(regions, config.key)).toList();

		return new Plot(
				team,
				bounds, plantBounds, floorBounds,
				spawn, shop, plantShop, mobShop,
				mobSpawn,
				config.forward
		);
	}

	public @Nullable BlockBox regionByName(String name) {
		// TODO: we need a better generic system for systems of region names that can be referenced
		return switch (name) {
			case "shop" -> shop;
			case "plant_shop" -> plantShop;
			case "mob_shop" -> mobShop;
			default -> null;
		};
	}

	public boolean canPlantAt(BlockPos pos) {
		return plantBounds.contains(pos) && !isMobSpawn(pos);
	}

	private boolean isMobSpawn(BlockPos pos) {
		for (BlockBox mobSpawn : mobSpawns) {
			if (mobSpawn.contains(pos)) {
				return true;
			}
		}
		return false;
	}

	public boolean isFloorAt(BlockPos pos) {
		return floorBounds.contains(pos);
	}

	public record RegionKeys(RegionPattern plot, RegionPattern spawn, RegionPattern shop, RegionPattern plantShop, RegionPattern mobShop, List<RegionPattern> mobSpawns) {
		public static final Codec<RegionKeys> CODEC = RecordCodecBuilder.create(i -> i.group(
				RegionPattern.CODEC.fieldOf("plot").forGetter(RegionKeys::plot),
				RegionPattern.CODEC.fieldOf("spawn").forGetter(RegionKeys::spawn),
				RegionPattern.CODEC.fieldOf("shop").forGetter(RegionKeys::shop),
				RegionPattern.CODEC.fieldOf("plant_shop").forGetter(RegionKeys::plantShop),
				RegionPattern.CODEC.fieldOf("mob_shop").forGetter(RegionKeys::mobShop),
				RegionPattern.CODEC.listOf().fieldOf("mob_spawns").forGetter(RegionKeys::mobSpawns)
		).apply(i, RegionKeys::new));
	}

	public record Config(String key, Direction forward) {
		public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("key").forGetter(Config::key),
				Direction.CODEC.fieldOf("forward").forGetter(Config::forward)
		).apply(i, Config::new));
	}
}
