package com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import org.jspecify.annotations.Nullable;

public final class PlantPlacement {
	private @Nullable PlantCoverage functionalCoverage;
	private @Nullable PlantCoverage decorationCoverage;
	private @Nullable Place place;

	public PlantPlacement covers(BlockPos pos) {
		return covers(PlantCoverage.of(pos));
	}

	public PlantPlacement covers(PlantCoverage coverage) {
		functionalCoverage = coverage;
		return this;
	}

	public PlantPlacement decorationCovers(PlantCoverage coverage) {
		decorationCoverage = coverage;
		return this;
	}

	public PlantPlacement places(Place place) {
		this.place = place;
		return this;
	}

	public @Nullable PlantCoverage getFunctionalCoverage() {
		return functionalCoverage;
	}

	public @Nullable PlantCoverage getDecorationCoverage() {
		return decorationCoverage;
	}

	public boolean place(ServerLevel world, PlantCoverage coverage) {
		if (place == null) {
			return false;
		}
		return place.place(world, coverage);
	}

	public interface Place {
		boolean place(ServerLevel world, PlantCoverage coverage);
	}
}
