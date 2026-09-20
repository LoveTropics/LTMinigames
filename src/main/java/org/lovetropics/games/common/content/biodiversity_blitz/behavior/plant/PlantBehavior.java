package org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant;

import org.lovetropics.games.common.content.biodiversity_blitz.behavior.event.BbEvents;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.event.PlacePlantResult;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.tutorial.TutorialState;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.Plot;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.PlotsState;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.plant.Plant;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.plant.PlantFamily;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.plant.PlantPlacement;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.plant.PlantType;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameEventListeners;
import org.lovetropics.games.common.core.game.behavior.event.GameEventType;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.List;

public final class PlantBehavior implements IGameBehavior {
	public static final MapCodec<PlantBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			PlantType.CODEC.fieldOf("id").forGetter(c -> c.plantType),
			PlantFamily.CODEC.fieldOf("family").forGetter(c -> c.family),
			Codec.DOUBLE.optionalFieldOf("value", 0.0).forGetter(c -> c.value),
			IGameBehavior.CODEC.optionalFieldOf("behaviors", IGameBehavior.EMPTY).forGetter(c -> c.behavior)
	).apply(i, PlantBehavior::new));

	private final PlantType plantType;
	private final PlantFamily family;
	private final double value;
	private final IGameBehavior behavior;

	private final GameEventListeners plantEvents = new GameEventListeners();

	private IGamePhase game;
	private PlotsState plots;
	private TutorialState tutorial;

	public PlantBehavior(PlantType plantType, PlantFamily family, double value, IGameBehavior behavior) {
		this.plantType = plantType;
		this.behavior = behavior;
		this.family = family;
		this.value = value;
	}

	private static boolean shouldPlantBehaviorHandle(GameEventType<?> type) {
		return type == BbPlantEvents.ADD || type == BbPlantEvents.TICK
				|| type == BbPlantEvents.PLACE || type == BbPlantEvents.BREAK;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		this.game = game;
		plots = game.state().getOrThrow(PlotsState.KEY);
		tutorial = game.state().getOrThrow(TutorialState.KEY);

		events.listen(BbEvents.PLACE_PLANT, this::placePlant);
		events.listen(BbEvents.BREAK_PLANT, this::breakPlant);

		events.listen(GamePlayerEvents.BREAK_BLOCK, this::onBreakBlock);

		events.listen(BbEvents.TICK_PLOT, this::onTickPlot);

		EventRegistrar plantEvents = events.redirect(PlantBehavior::shouldPlantBehaviorHandle, this.plantEvents);
		behavior.register(game, plantEvents);
	}

	private PlacePlantResult placePlant(ServerPlayer player, Plot plot, BlockPos pos, PlantType plantType) {
		if (!this.plantType.equals(plantType)) {
			return PlacePlantResult.PASS;
		}

		PlantPlacement placement = plantEvents.invoker(BbPlantEvents.PLACE).placePlant(player, plot, pos);
		if (placement == null) {
			return PlacePlantResult.PASS;
		}

		if (placement.getFunctionalCoverage() == null) {
			return new PlacePlantResult.Fail();
		}

		Plant plant = plot.plants.addPlant(plantType, family, value, placement);
		if (plant == null) {
			return new PlacePlantResult.CannotFit();
		}

		if (placement.place(plot.level, plant.coverage())) {
			plantEvents.invoker(BbPlantEvents.ADD).onAddPlant(player, plot, plant);
			game.invoker(BbEvents.PLANTS_CHANGED).onPlantsChanged(player, plot);

			return new PlacePlantResult.Success(plant);
		} else {
			plot.plants.removePlant(plant);
			return new PlacePlantResult.CannotFit();
		}
	}

	private boolean breakPlant(ServerPlayer player, Plot plot, Plant plant) {
		if (!plantType.equals(plant.type())) {
			return false;
		}

		for (BlockPos plantPos : plant.coverage()) {
			FluidState fluidState = plot.level.getFluidState(plantPos);
			plot.level.setBlock(plantPos, fluidState.createLegacyBlock(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		}

		boolean removed = plot.plants.removePlant(plant);
		if (!removed) {
			return false;
		}

		game.invoker(BbEvents.PLANTS_CHANGED).onPlantsChanged(player, plot);

		return true;
	}

	private TriState onBreakBlock(ServerPlayer player, BlockPos pos, BlockState state, InteractionHand hand) {
		if (!tutorial.isTutorialFinished()) {
			return TriState.FALSE;
		}

		Plot plot = plots.getPlotFor(player);
		if (plot == null) {
			return TriState.DEFAULT;
		}

		Tool tool = player.getItemInHand(hand).get(DataComponents.TOOL);
		if (tool != null && !tool.canDestroyBlocksInCreative()) {
			return TriState.FALSE;
		}

		Plant plant = plot.plants.getPlantAt(pos, plantType);
		if (plant != null) {
			return onBreakPlantBlock(player, pos, plot, plant);
		} else {
			return TriState.DEFAULT;
		}
	}

	private TriState onBreakPlantBlock(ServerPlayer player, BlockPos pos, Plot plot, Plant plant) {
		plantEvents.invoker(BbPlantEvents.BREAK).breakPlant(player, plot, plant, pos);
		game.invoker(BbEvents.BREAK_PLANT).breakPlant(player, plot, plant);

		return TriState.FALSE;
	}

	private void onTickPlot(Plot plot, PlayerSet players) {
		List<Plant> plants = plot.plants.getPlantsByType(plantType);
		if (!plants.isEmpty()) {
			plantEvents.invoker(BbPlantEvents.TICK).onTickPlants(players, plot, plants);
		}
	}
}
