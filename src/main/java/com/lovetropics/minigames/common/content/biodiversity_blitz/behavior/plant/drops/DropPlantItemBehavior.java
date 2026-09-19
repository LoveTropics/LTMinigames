package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.drops;

import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantItemType;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public record DropPlantItemBehavior(PlantItemType plant) implements IGameBehavior {
	public static final MapCodec<DropPlantItemBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			PlantItemType.CODEC.fieldOf("plant").forGetter(c -> c.plant)
	).apply(i, DropPlantItemBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(BbPlantEvents.BREAK, (player, plot, plant, pos) -> {
			ItemStack plantItem = game.invoker(BbEvents.CREATE_PLANT_ITEM).createPlantItem(this.plant);
			Block.popResource(plot.level, pos, plantItem);
		});
	}
}
