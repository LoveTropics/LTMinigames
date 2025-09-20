package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event;

import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantItemType;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantType;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Set;

public final class BbEvents {
	public static final GameEventType<CreatePlot> CREATE_PLOT = GameEventType.create(CreatePlot.class, listeners -> plot -> {
		for (CreatePlot listener : listeners) {
			listener.onCreatePlot(plot);
		}
	});

	public static final GameEventType<AssignPlot> ASSIGN_PLOT = GameEventType.create(AssignPlot.class, listeners -> (player, plot) -> {
		for (AssignPlot listener : listeners) {
			listener.onAssignPlot(player, plot);
		}
	});

	public static final GameEventType<TickPlot> TICK_PLOT = GameEventType.create(TickPlot.class, listeners -> (plot, players) -> {
		for (TickPlot listener : listeners) {
			listener.onTickPlot(plot, players);
		}
	});

	public static final GameEventType<PlacePlant> PLACE_PLANT = GameEventType.create(PlacePlant.class, listeners -> (player, plot, pos, plantType) -> {
		for (PlacePlant listener : listeners) {
			PlacePlantResult result = listener.placePlant(player, plot, pos, plantType);
			if (!(result instanceof PlacePlantResult.Pass)) {
				return result;
			}
		}
		return PlacePlantResult.PASS;
	});

	public static final GameEventType<BreakPlant> BREAK_PLANT = GameEventType.create(BreakPlant.class, listeners -> (player, plot, plant) -> {
		for (BreakPlant listener : listeners) {
			if (listener.breakPlant(player, plot, plant)) {
				return true;
			}
		}
		return false;
	});

	public static final GameEventType<CreatePlantItem> CREATE_PLANT_ITEM = GameEventType.create(CreatePlantItem.class, listeners -> itemType -> {
		for (CreatePlantItem listener : listeners) {
			ItemStack stack = listener.createPlantItem(itemType);
			if (!stack.isEmpty()) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	});

	public static final GameEventType<PlantsChanged> PLANTS_CHANGED = GameEventType.create(PlantsChanged.class, listeners -> (player, plot) -> {
		for (PlantsChanged listener : listeners) {
			listener.onPlantsChanged(player, plot);
		}
	});

	public static final GameEventType<CurrencyChanged> CURRENCY_CHANGED = GameEventType.create(CurrencyChanged.class, listeners -> (player, value, lastValue) -> {
		for (CurrencyChanged listener : listeners) {
			listener.onCurrencyChanged(player, value, lastValue);
		}
	});

	public static final GameEventType<TeamCurrencyChanged> CURRENCY_ACCUMULATE = GameEventType.create(TeamCurrencyChanged.class, listeners -> (team, value, lastValue) -> {
		for (TeamCurrencyChanged listener : listeners) {
			listener.onCurrencyChanged(team, value, lastValue);
		}
	});

	public static final GameEventType<TeamCurrencyChanged> CURRENCY_INCREMENT_CHANGED = GameEventType.create(TeamCurrencyChanged.class, listeners -> (team, value, lastValue) -> {
		for (TeamCurrencyChanged listener : listeners) {
			listener.onCurrencyChanged(team, value, lastValue);
		}
	});

	public static final GameEventType<GamePlayerEvents.Death> BB_DEATH = GameEventType.create(GamePlayerEvents.Death.class, listeners -> (player, damageSource) -> {
		for (GamePlayerEvents.Death listener : listeners) {
			TriState result = listener.onDeath(player, damageSource);
			if (!result.isDefault()) {
				return result;
			}
		}
		return TriState.DEFAULT;
	});

	public static final GameEventType<ModifyWaveMobs> MODIFY_WAVE_MODS = GameEventType.create(ModifyWaveMobs.class, listeners -> (entities, random, world, plot, waveIndex) -> {
		for (final ModifyWaveMobs listener : listeners) {
			listener.modifyWave(entities, random, world, plot, waveIndex);
		}
	});

	private BbEvents() {
	}

	public interface CreatePlot {
		void onCreatePlot(Plot plot);
	}

	public interface AssignPlot {
		void onAssignPlot(ServerPlayer player, Plot plot);
	}

	public interface TickPlot {
		void onTickPlot(Plot plot, PlayerSet players);
	}

	public interface PlacePlant {
		PlacePlantResult placePlant(ServerPlayer player, Plot plot, BlockPos pos, PlantType plantType);
	}

	public interface BreakPlant {
		boolean breakPlant(ServerPlayer player, Plot plot, Plant plant);
	}

	public interface CreatePlantItem {
		ItemStack createPlantItem(PlantItemType itemType);
	}

	public interface PlantsChanged {
		void onPlantsChanged(ServerPlayer player, Plot plot);
	}

	public interface CurrencyChanged {
		void onCurrencyChanged(ServerPlayer player, int value, int lastValue);
	}

	public interface TeamCurrencyChanged {
		void onCurrencyChanged(GameTeamKey team, int value, int lastValue);
	}

	public interface ModifyWaveMobs {
		void modifyWave(Set<Entity> entities, RandomSource random, Level world, Plot plot, int waveIndex);
	}
}
