package org.lovetropics.games.common.content.biodiversity_blitz.plot.plant;

import com.mojang.serialization.Codec;

public record PlantType(String id) {
	public static final Codec<PlantType> CODEC = Codec.STRING.xmap(PlantType::new, PlantType::id);
}
