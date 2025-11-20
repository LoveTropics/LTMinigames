package com.lovetropics.minigames.common.core.game.behavior.instances.world;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.escape_race.behaviours.ItemFrameCodeBehaviour;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.util.GameScheduler;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public record NicelyBreakRegionOfBlocksBehaviour(
		String region
) implements IGameBehavior {

	public static final MapCodec<NicelyBreakRegionOfBlocksBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("region").forGetter(NicelyBreakRegionOfBlocksBehaviour::region)
	).apply(i, NicelyBreakRegionOfBlocksBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Collection<BlockBox> regionToBreak = game.mapRegions().getAll(region);
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			regionToBreak.forEach(blockBox -> {
				findNeighboursOfTypeAndDestroyWithinRegion(game.scheduler(), game.level(), blockBox.centerBlock(), null, blockBox);
			});
			return true;
		});
	}

	private static void findNeighboursOfTypeAndDestroyWithinRegion(GameScheduler scheduler, ServerLevel world, BlockPos pos, @Nullable Block blockType, BlockBox regionToBreak) {
		for (Direction direction : Direction.values()) {
			BlockPos relative = pos.relative(direction);
			if(!regionToBreak.contains(relative)) {
				continue;
			}
			BlockState blockState = world.getBlockState(relative);
			if (!blockState.isAir()) {
				if (blockType == null) {
					blockType = blockState.getBlock();
				}
				if (blockState.is(blockType)) {
					world.destroyBlock(relative, false);
					Block finalBlockType = blockType;
					scheduler.runAfterSeconds(0.5f, () -> {
						findNeighboursOfTypeAndDestroyWithinRegion(scheduler, world, relative, finalBlockType, regionToBreak);
					});
				}
			}
		}
	}
}
