package com.lovetropics.minigames.common.util.world;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.util.GameScheduler;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.FillCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class BlockPlacer {

	static final BlockInput HOLLOW_CORE = new BlockInput(Blocks.AIR.defaultBlockState(), Collections.emptySet(), null);

	public enum Mode {
		REPLACE((_, _, blockInput, _) -> blockInput),
		OUTLINE((boundingBox, blockPos, blockInput, _) ->
				blockPos.getX() != boundingBox.minX() && blockPos.getX() != boundingBox.maxX() && blockPos.getY() != boundingBox.minY() && blockPos.getY() != boundingBox.maxY() && blockPos.getZ() != boundingBox.minZ() && blockPos.getZ() != boundingBox.maxZ() ? null : blockInput),
		HOLLOW((boundingBox, blockPos, blockInput, _) ->
				blockPos.getX() != boundingBox.minX() && blockPos.getX() != boundingBox.maxX() && blockPos.getY() != boundingBox.minY() && blockPos.getY() != boundingBox.maxY() && blockPos.getZ() != boundingBox.minZ() && blockPos.getZ() != boundingBox.maxZ() ? HOLLOW_CORE : blockInput),
		DESTROY((_, blockPos, blockInput, serverLevel) -> {
			serverLevel.destroyBlock(blockPos, true);
			return blockInput;
		}),
		DESTROY_NO_DROP((_, blockPos, blockInput, serverLevel) -> {
			serverLevel.destroyBlock(blockPos, false);
			return blockInput;
		});

		public final FillCommand.Filter filter;

		Mode(FillCommand.Filter filter) {
			this.filter = filter;
		}
	}

	public static void replace(ServerLevel level, BlockBox box, Block newBlock, Mode mode, Block replacingBlock) {
		placeBlocks(level, box, getBlockInput(newBlock), mode, (blockInWorld -> blockInWorld.getState().getBlock() == replacingBlock), null, null, null);
	}

	public static void replace(ServerLevel level, BlockBox box, Block newBlock, Mode mode, Block replacingBlock, GameScheduler scheduler, Function<BlockPos, Integer> tickDelay) {
		placeBlocks(level, box, getBlockInput(newBlock), mode, (blockInWorld -> blockInWorld.getState().getBlock() == replacingBlock), scheduler, tickDelay, null);
	}

	public static void replace(ServerLevel level, BlockBox box, Block newBlock, Mode mode, Block replacingBlock, GameScheduler scheduler, Function<BlockPos, Integer> tickDelay, Consumer<BlockPos> doneCallback) {
		placeBlocks(level, box, getBlockInput(newBlock), mode, (blockInWorld -> blockInWorld.getState().getBlock() == replacingBlock), scheduler, tickDelay, doneCallback);
	}

	public static void replace(ServerLevel level, BlockBox box, BlockInput newBlock, Mode mode, Block replacingBlock) {
		placeBlocks(level, box, newBlock, mode, (blockInWorld -> blockInWorld.getState().getBlock() == replacingBlock), null, null, null);
	}

	public static BlockInput getBlockInput(Block block) {
		return new BlockInput(block.defaultBlockState(), Collections.emptySet(), null);
	}

	public static void fill(ServerLevel level, BlockBox box, BlockInput newBlock, Mode mode) {
		placeBlocks(level, box, newBlock, mode, null, null, null, null);
	}

	/**
	 * Logic mostly copied from the fill command.
	 */
	public static void placeBlocks(ServerLevel level, BlockBox box, BlockInput newBlock, Mode mode, @Nullable Predicate<BlockInWorld> replacingPredicate, @Nullable GameScheduler scheduler, @Nullable Function<BlockPos, Integer> tickDelay, @Nullable Consumer<BlockPos> doneCallback) {
		record UpdatedBlock(BlockPos pos, BlockState oldState) {
		}

		List<UpdatedBlock> updatedBlocks = new ArrayList<>();

		BoundingBox boundingBox = new BoundingBox(box.min().getX(), box.min().getY(), box.min().getZ(), box.max().getX(), box.max().getY(), box.max().getZ());
		for (BlockPos pos : box) {
			if (replacingPredicate == null || replacingPredicate.test(new BlockInWorld(level, pos, true))) {

				if (scheduler != null) {
					var delay = tickDelay != null ? tickDelay.apply(pos) : 0;
					var blockPosCopy = pos.immutable();
					scheduler.runAfterTicks(delay, () -> {
						// The target block might have changed since we scheduled - check it again!
						if (delay > 0 && replacingPredicate != null && !replacingPredicate.test(new BlockInWorld(level, blockPosCopy, true))) {
							return;
						}
						BlockInput blockinput = mode.filter.filter(boundingBox, blockPosCopy, newBlock, level);
						if (blockinput != null) {
							BlockState oldState = level.getBlockState(blockPosCopy);
							if (level.getBlockEntity(blockPosCopy) instanceof Clearable clearable) {
								clearable.clearContent();
							}
							if (blockinput.place(level, blockPosCopy, Block.UPDATE_CLIENTS)) {
								level.updateNeighboursOnBlockSet(blockPosCopy, oldState);
							}
						}
						if (doneCallback != null) {
							doneCallback.accept(blockPosCopy);
						}
					});
				} else {
					BlockInput blockinput = mode.filter.filter(boundingBox, pos, newBlock, level);
					if (blockinput != null) {
						BlockState oldState = level.getBlockState(pos);
						if (level.getBlockEntity(pos) instanceof Clearable clearable) {
							clearable.clearContent();
						}
						if (blockinput.place(level, pos, Block.UPDATE_CLIENTS)) {
							updatedBlocks.add(new UpdatedBlock(pos.immutable(), oldState));
						}
					}
				}
			}
		}

		if (scheduler == null) {
			for (UpdatedBlock updatedBlock : updatedBlocks) {
				level.updateNeighboursOnBlockSet(updatedBlock.pos, updatedBlock.oldState);
			}
		}
	}
}
