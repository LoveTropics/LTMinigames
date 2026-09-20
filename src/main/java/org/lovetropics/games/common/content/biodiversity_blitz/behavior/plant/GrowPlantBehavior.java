package org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant;

import org.lovetropics.games.common.content.biodiversity_blitz.behavior.event.BbEvents;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.event.PlacePlantResult;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.Plot;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.plant.Plant;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.plant.PlantType;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.plant.state.PlantState;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public record GrowPlantBehavior(IntProvider time, PlantType growInto) implements IGameBehavior {
	public static final MapCodec<GrowPlantBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IntProviders.NON_NEGATIVE_CODEC.fieldOf("time").forGetter(c -> c.time),
			PlantType.CODEC.fieldOf("grow_into").forGetter(c -> c.growInto)
	).apply(i, GrowPlantBehavior::new));

	private static final int GROW_ATTEMPT_INTERVAL_TICKS = SharedConstants.TICKS_PER_SECOND;

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(BbPlantEvents.ADD, (player, plot, plant) ->
				plant.state().put(GrowTime.KEY, new GrowTime(game.ticks() + time.sample(game.random())))
		);

		events.listen(BbPlantEvents.TICK, (players, plot, plants) -> {
			long ticks = game.ticks();
			if (ticks % GROW_ATTEMPT_INTERVAL_TICKS != 0) {
				return;
			}

			List<Plant> growPlants = collectPlantsToGrow(plants, ticks);
			for (Plant plant : growPlants) {
				tryGrowPlant(game, players.iterator().next(), plot, plant);
			}
		});
	}

	private void tryGrowPlant(IGamePhase game, ServerPlayer player, Plot plot, Plant plant) {
		PlantSnapshot snapshot = removeAndSnapshot(plot.level, plot, plant);

		BlockPos origin = plant.coverage().getOrigin();
		PlacePlantResult result = game.invoker(BbEvents.PLACE_PLANT).placePlant(player, plot, origin, growInto);
		if (!(result instanceof PlacePlantResult.Success)) {
			restoreSnapshot(plot.level, plot, snapshot);
		}
	}

	private List<Plant> collectPlantsToGrow(List<Plant> plants, long ticks) {
		List<Plant> result = new ArrayList<>();
		for (Plant plant : plants) {
			GrowTime growTime = plant.state(GrowTime.KEY);
			if (growTime != null && ticks >= growTime.next) {
				result.add(plant);
			}
		}

		return result;
	}

	private PlantSnapshot removeAndSnapshot(ServerLevel level, Plot plot, Plant plant) {
		plot.plants.removePlant(plant);

		Long2ObjectMap<BlockState> blocks = new Long2ObjectOpenHashMap<>();
		for (BlockPos pos : plant.coverage()) {
			blocks.put(pos.asLong(), level.getBlockState(pos));
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		}

		return new PlantSnapshot(plant, blocks);
	}

	private void restoreSnapshot(ServerLevel level, Plot plot, PlantSnapshot snapshot) {
		plot.plants.addPlant(snapshot.plant);

		for (BlockPos pos : snapshot.plant.coverage()) {
			BlockState block = snapshot.blocks.get(pos.asLong());
			if (block != null) {
				level.setBlock(pos, block, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
			}
		}
	}

	record GrowTime(long next) {
		static final PlantState.Key<GrowTime> KEY = PlantState.Key.create();
	}

	record PlantSnapshot(Plant plant, Long2ObjectMap<BlockState> blocks) {
	}
}
