package org.lovetropics.games.common.content.biodiversity_blitz.behavior;

import org.lovetropics.games.common.content.biodiversity_blitz.behavior.event.BbEvents;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.plant.PlantItemType;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record BbGivePlantsBehavior(List<PlantConfig> plants) implements IGameBehavior {
	public static final MapCodec<BbGivePlantsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			PlantConfig.CODEC.listOf().fieldOf("plants").forGetter(c -> c.plants)
	).apply(i, BbGivePlantsBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(BbEvents.ASSIGN_PLOT, (player, plot) -> {
			for (PlantConfig plant : plants) {
				ItemStack stack = plant.create(game);
				if (!stack.isEmpty()) {
					player.addItem(stack);
				}
			}
		});
	}

	record PlantConfig(PlantItemType item, int count) {
		public static final Codec<PlantConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
				PlantItemType.CODEC.fieldOf("item").forGetter(c -> c.item),
				Codec.INT.optionalFieldOf("count", 1).forGetter(c -> c.count)
		).apply(i, PlantConfig::new));

		ItemStack create(IGamePhase game) {
			ItemStack stack = game.invoker(BbEvents.CREATE_PLANT_ITEM).createPlantItem(item);
			if (!stack.isEmpty()) {
				stack.setCount(count);
			}
			return stack;
		}
	}
}
