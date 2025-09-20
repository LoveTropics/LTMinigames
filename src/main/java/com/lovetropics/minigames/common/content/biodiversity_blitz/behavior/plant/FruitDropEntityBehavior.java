package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class FruitDropEntityBehavior implements IGameBehavior {

	public static final MapCodec<FruitDropEntityBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("range").forGetter(o -> o.range),
			Codec.INT.fieldOf("interval").forGetter(o -> o.interval),
			BuiltInRegistries.BLOCK.byNameCodec().fieldOf("fruit").forGetter(o -> o.fruit),
			BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(o -> o.entity)
	).apply(i, FruitDropEntityBehavior::new));

	private final int range;
	private final int interval;
	private final Block fruit;
	private final EntityType<?> entity;

	private IGamePhase game;

	public FruitDropEntityBehavior(int range, int interval, Block fruit, EntityType<?> entity) {
		this.range = range;
		this.interval = interval;
		this.fruit = fruit;
		this.entity = entity;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		this.game = game;
		events.listen(BbPlantEvents.TICK, this::tick);
	}

	private void tick(PlayerSet players, Plot plot, List<Plant> plants) {
		if (game.ticks() % interval == 0) {
			for (Plant plant : plants) {
				updateCoconuts(plant);
			}
		}
	}

	private void updateCoconuts(Plant plant) {
		ServerLevel level = game.level();
		plant.functionalCoverage().stream()
				.flatMap(bp -> IntStream.range(0, 4).mapToObj(Direction::from2DDataValue).map(bp::relative))
				.filter(bp -> level.getBlockState(bp).getBlock() == fruit)
				.filter(bp -> {
					AABB bounds = new AABB(bp).inflate(range - 1, 15, range - 1);
					return !level.getEntities((Entity) null, bounds, (Predicate<? super Entity>) e -> (e instanceof BbMobEntity)).isEmpty();
				})
				.forEach(bp -> {
					level.setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());
					Entity spawnedEntity = entity.spawn(level, null, null, bp, EntitySpawnReason.TRIGGERED, false, false);
					if (spawnedEntity != null) {
						level.addFreshEntity(spawnedEntity);
					}
				});
	}
}
