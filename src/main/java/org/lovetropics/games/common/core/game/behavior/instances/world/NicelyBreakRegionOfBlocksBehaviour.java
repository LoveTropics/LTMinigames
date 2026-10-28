package org.lovetropics.games.common.core.game.behavior.instances.world;

import com.lovetropics.lib.BlockBox;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.game.util.GameScheduler;

import java.util.Collection;

public record NicelyBreakRegionOfBlocksBehaviour(
		RegionQuery region
) implements IGameBehavior {

	public static final MapCodec<NicelyBreakRegionOfBlocksBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("region").forGetter(NicelyBreakRegionOfBlocksBehaviour::region)
	).apply(i, NicelyBreakRegionOfBlocksBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Collection<Region> regionToBreak = region.all(game);
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			regionToBreak.forEach(region -> {
				findNeighboursOfTypeAndDestroyWithinRegion(game.scheduler(), region.level(), region.centerBlock(), null, region.box());
			});
			return true;
		});
	}

	private static void findNeighboursOfTypeAndDestroyWithinRegion(GameScheduler scheduler, ServerLevel level, BlockPos pos, @Nullable Block blockType, BlockBox regionToBreak) {
		for (Direction direction : Direction.values()) {
			BlockPos relative = pos.relative(direction);
			if(!regionToBreak.contains(relative)) {
				continue;
			}
			BlockState blockState = level.getBlockState(relative);
			if (!blockState.isAir()) {
				if (blockType == null) {
					blockType = blockState.getBlock();
				}
				if (blockState.is(blockType)) {
					level.destroyBlock(relative, false);
					Block finalBlockType = blockType;
					scheduler.runAfterSeconds(0.5f, () -> {
						findNeighboursOfTypeAndDestroyWithinRegion(scheduler, level, relative, finalBlockType, regionToBreak);
					});
				}
			}
		}
	}
}
