package org.lovetropics.games.common.content.biodiversity_blitz.plot.plant;

import org.lovetropics.games.common.content.biodiversity_blitz.BiodiversityBlitz;
import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public record PlantItemType(String id) {
	public static final Codec<PlantItemType> CODEC = Codec.STRING.xmap(PlantItemType::new, PlantItemType::id);

	public boolean matches(ItemStack item) {
		return Objects.equals(this, item.get(BiodiversityBlitz.PLANT_COMPONENT));
	}
}
