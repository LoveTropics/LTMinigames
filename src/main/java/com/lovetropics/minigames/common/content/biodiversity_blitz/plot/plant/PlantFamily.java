package com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant;

import com.google.common.collect.ImmutableSet;
import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.serialization.Codec;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum PlantFamily {
	CROP("crop", true, 8),
	PLANT("plant", true, 6),
	SAPLING("sapling", false, 0),
	TREE("tree", true, 2);

	public static final Codec<PlantFamily> CODEC = MoreCodecs.stringVariants(PlantFamily.values(), PlantFamily::friendlyName);

	public static final ImmutableSet<PlantFamily> BIODIVERSITY_VALUES = ImmutableSet.copyOf(Arrays.stream(values())
			.filter(f -> f.countsForBiodiversity)
			.collect(Collectors.toList()));

	private final String name;
	private final boolean countsForBiodiversity;
	private final int minBeforeMonoculture;

	PlantFamily(String name, boolean countsForBiodiversity, int minBeforeMonoculture) {
		this.name = name;
		this.countsForBiodiversity = countsForBiodiversity;
		this.minBeforeMonoculture = minBeforeMonoculture;
	}

	public String friendlyName() {
		return name;
	}

	public int getMinBeforeMonoculture() {
		return minBeforeMonoculture;
	}
}
