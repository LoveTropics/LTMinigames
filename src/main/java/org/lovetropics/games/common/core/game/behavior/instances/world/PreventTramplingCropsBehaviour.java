package org.lovetropics.games.common.core.game.behavior.instances.world;

import com.lovetropics.lib.BlockBox;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameLivingEntityEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public record PreventTramplingCropsBehaviour (
		List<String> regions
) implements IGameBehavior {

	public static final MapCodec<PreventTramplingCropsBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.listOf().optionalFieldOf("regions", List.of()).forGetter(c -> c.regions)
	).apply(i, PreventTramplingCropsBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		List<BlockBox> blockBoxes = game.mapRegions().getAll(regions);
		events.listen(GameLivingEntityEvents.FARMLAND_TRAMPLE, (level, entity, blockPos, blockState) -> {
			if (level != game.level()) {
				return TriState.DEFAULT;
			}
			return onTrampleFarmland(blockBoxes, entity, blockPos, blockState);
		});
	}

	private TriState onTrampleFarmland(List<BlockBox> blockBoxes, Entity entity, BlockPos blockPos, BlockState blockState) {
		if(blockBoxes.isEmpty()){
			return TriState.FALSE;
		} else {
			if(blockBoxes.stream().anyMatch(bb -> bb.contains(blockPos))){
				return TriState.FALSE;
			}
		}
		return TriState.DEFAULT;
	}
}
