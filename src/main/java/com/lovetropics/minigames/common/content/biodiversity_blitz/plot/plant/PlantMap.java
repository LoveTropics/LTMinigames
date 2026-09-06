package com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class PlantMap implements Iterable<Plant> {
	private final List<Plant> plants = new ArrayList<>();
	private final Map<PlantType, List<Plant>> plantsByType = new Object2ObjectOpenHashMap<>();

	private final Long2ObjectMap<Plant> plantByPos = new Long2ObjectOpenHashMap<>();

	public @Nullable Plant addPlant(PlantType type, PlantFamily family, double value, PlantPlacement placement) {
		PlantCoverage functionalCoverage = placement.getFunctionalCoverage();
		if (functionalCoverage == null || !canAddPlantAt(functionalCoverage)) {
			return null;
		}

		PlantCoverage decoration = placement.getDecorationCoverage();
		if (decoration != null) {
			decoration = removeDecorationIntersection(decoration);
		}

		Plant plant = new Plant(type, functionalCoverage, decoration, family, value);
		addPlant(plant);
		return plant;
	}

	public void addPlant(Plant plant) {
		plants.add(plant);
		plantsByType.computeIfAbsent(plant.type(), t -> new ArrayList<>())
				.add(plant);

		for (BlockPos pos : plant.functionalCoverage()) {
			plantByPos.put(pos.asLong(), plant);
		}

		PlantCoverage decoration = plant.decorationCoverage();
		if (decoration != null) {
			for (BlockPos pos : decoration) {
				plantByPos.put(pos.asLong(), plant);
			}
		}
	}

	private PlantCoverage removeDecorationIntersection(PlantCoverage decoration) {
		LongSet intersection = new LongOpenHashSet();
		for (BlockPos pos : decoration) {
			long posKey = pos.asLong();
			if (plantByPos.containsKey(posKey)) {
				intersection.add(posKey);
			}
		}

		if (!intersection.isEmpty()) {
			return decoration.removeIntersection(intersection);
		} else {
			return decoration;
		}
	}

	public boolean removePlant(Plant plant) {
		if (plants.remove(plant)) {
			List<Plant> plantsByType = this.plantsByType.get(plant.type());
			if (plantsByType != null) {
				plantsByType.remove(plant);
			}

			for (BlockPos pos : plant.functionalCoverage()) {
				plantByPos.remove(pos.asLong(), plant);
			}

			PlantCoverage decoration = plant.decorationCoverage();
			if (decoration != null) {
				for (BlockPos pos : decoration) {
					plantByPos.remove(pos.asLong(), plant);
				}
			}

			return true;
		} else {
			return false;
		}
	}

	public @Nullable Plant getPlantAt(long pos) {
		return plantByPos.get(pos);
	}

	public @Nullable Plant getPlantAt(BlockPos pos) {
		return getPlantAt(pos.asLong());
	}

	public @Nullable Plant getPlantAt(BlockPos pos, PlantType type) {
		Plant plant = getPlantAt(pos);
		return plant != null && plant.type().equals(type) ? plant : null;
	}

	public boolean hasPlantAt(BlockPos pos) {
		return getPlantAt(pos) != null;
	}

	public boolean canAddPlantAt(BlockPos pos) {
		return plantByPos.get(pos.asLong()) == null;
	}

	public boolean canAddPlantAt(PlantCoverage coverage) {
		for (BlockPos pos : coverage) {
			Plant plant = plantByPos.get(pos.asLong());
			if (plant != null) {
				return false;
			}
		}
		return true;
	}

	public List<Plant> getPlantsByType(PlantType type) {
		return plantsByType.getOrDefault(type, Collections.emptyList());
	}

	@Override
	public Iterator<Plant> iterator() {
		return plants.iterator();
	}
}
