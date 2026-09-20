package org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.placement;

import com.lovetropics.lib.codec.MoreCodecs;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.Plot;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.plant.PlantPlacement;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public record PlaceSinglePlantBehavior(BlockState block) implements IGameBehavior {
	public static final MapCodec<PlaceSinglePlantBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MoreCodecs.BLOCK_STATE.fieldOf("block").forGetter(c -> c.block)
	).apply(i, PlaceSinglePlantBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(BbPlantEvents.PLACE, (player, plot, pos) -> new PlantPlacement()
				.covers(pos)
				.places((level, coverage) -> {
					level.setBlockAndUpdate(pos, getPlaceBlock(plot));
					return true;
				}));
	}

	private BlockState getPlaceBlock(Plot plot) {
		BlockState block = this.block;
		if (block.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			block = block.setValue(BlockStateProperties.HORIZONTAL_FACING, plot.forward);
		}
		return block;
	}
}
