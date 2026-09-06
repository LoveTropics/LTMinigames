package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event;

import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantPlacement;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.Nullable;
import java.util.List;

public final class BbPlantEvents {
	public static final GameEventType<Add> ADD = GameEventType.create(Add.class, listeners -> (player, plot, plant) -> {
		for (Add listener : listeners) {
			listener.onAddPlant(player, plot, plant);
		}
	});

	public static final GameEventType<Tick> TICK = GameEventType.create(Tick.class, listeners -> (player, plot, plants) -> {
		for (Tick listener : listeners) {
			listener.onTickPlants(player, plot, plants);
		}
	});

	public static final GameEventType<Place> PLACE = GameEventType.create(Place.class, listeners -> (player, plot, pos) -> {
		for (Place listener : listeners) {
			PlantPlacement placement = listener.placePlant(player, plot, pos);
			if (placement != null) {
				return placement;
			}
		}
		return null;
	});

	public static final GameEventType<Break> BREAK = GameEventType.create(Break.class, listeners -> (player, plot, plant, pos) -> {
		for (Break listener : listeners) {
			listener.breakPlant(player, plot, plant, pos);
		}
	});

	private BbPlantEvents() {
	}

	public interface Add {
		void onAddPlant(ServerPlayer player, Plot plot, Plant plant);
	}

	public interface Tick {
		void onTickPlants(PlayerSet players, Plot plot, List<Plant> plants);
	}

	public interface Place {
		@Nullable PlantPlacement placePlant(ServerPlayer player, Plot plot, BlockPos pos);
	}

	public interface Break {
		void breakPlant(ServerPlayer player, Plot plot, Plant plant, BlockPos pos);
	}
}
