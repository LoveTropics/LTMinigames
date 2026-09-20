package org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.placement;

import com.lovetropics.lib.codec.MoreCodecs;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.plant.PlantCoverage;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.plant.PlantPlacement;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

public record PlaceDoublePlantBehavior(BlockState block) implements IGameBehavior {
	public static final MapCodec<PlaceDoublePlantBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MoreCodecs.BLOCK_STATE.fieldOf("block").forGetter(c -> c.block)
	).apply(i, PlaceDoublePlantBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(BbPlantEvents.PLACE, (player, plot, pos) -> new PlantPlacement()
				.covers(PlantCoverage.ofDouble(pos))
				.places((level, coverage) -> {
					DoublePlantBlock.placeAt(level, block, pos, Block.UPDATE_ALL);
					return true;
				}));
	}
}
