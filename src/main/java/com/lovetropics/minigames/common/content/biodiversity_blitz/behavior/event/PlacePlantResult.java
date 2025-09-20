package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event;

import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;

public sealed interface PlacePlantResult {
	PlacePlantResult PASS = new Pass();
	PlacePlantResult CANNOT_FIT = new CannotFit();
	PlacePlantResult FAIL = new Fail();

	record Success(Plant plant) implements PlacePlantResult {
	}

	record Pass() implements PlacePlantResult {
	}

	record CannotFit() implements PlacePlantResult {
	}

	record Fail() implements PlacePlantResult {
	}
}
