package org.lovetropics.games.common.content.survive_the_tide.block;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BaseBigRedButtonBlock extends ButtonBlock {
	public static final int PRESSED_DEPTH = 2;
	public static final int UNPRESSED_DEPTH = 3;
	public static final int HALF_SIZE = 5;
	private static final VoxelShape CEILING_AABB_X = Block.box(8.0 - HALF_SIZE, 16.0 - UNPRESSED_DEPTH, 8.0 - HALF_SIZE, 8.0 + HALF_SIZE, 16.0, 8.0 + HALF_SIZE);
	private static final VoxelShape CEILING_AABB_Z = Block.box(8.0 - HALF_SIZE, 16.0 - UNPRESSED_DEPTH, 8.0 - HALF_SIZE, 8.0 + HALF_SIZE, 16.0, 8.0 + HALF_SIZE);
	private static final VoxelShape FLOOR_AABB_X = Block.box(8.0 - HALF_SIZE, 0.0, 8.0 - HALF_SIZE, 8.0 + HALF_SIZE, UNPRESSED_DEPTH, 8.0 + HALF_SIZE);
	private static final VoxelShape FLOOR_AABB_Z = Block.box(8.0 - HALF_SIZE, 0.0, 8.0 - HALF_SIZE, 8.0 + HALF_SIZE, UNPRESSED_DEPTH, 8.0 + HALF_SIZE);
	private static final VoxelShape NORTH_AABB = Block.box(8.0 - HALF_SIZE, 8.0 - HALF_SIZE, 16.0 - UNPRESSED_DEPTH, 8.0 + HALF_SIZE, 8.0 + HALF_SIZE, 16.0);
	private static final VoxelShape SOUTH_AABB = Block.box(8.0 - HALF_SIZE, 8.0 - HALF_SIZE, 0.0, 8.0 + HALF_SIZE, 8.0 + HALF_SIZE, UNPRESSED_DEPTH);
	private static final VoxelShape WEST_AABB = Block.box(16.0 - UNPRESSED_DEPTH, 8.0 - HALF_SIZE, 8.0 - HALF_SIZE, 16.0, 8.0 + HALF_SIZE, 8.0 + HALF_SIZE);
	private static final VoxelShape EAST_AABB = Block.box(0.0, 8.0 - HALF_SIZE, 8.0 - HALF_SIZE, UNPRESSED_DEPTH, 8.0 + HALF_SIZE, 8.0 + HALF_SIZE);
	private static final VoxelShape PRESSED_CEILING_AABB_X = Block.box(8.0 - HALF_SIZE, 16.0 - PRESSED_DEPTH, 8.0 - HALF_SIZE, 8.0 + HALF_SIZE, 16.0, 8.0 + HALF_SIZE);
	private static final VoxelShape PRESSED_CEILING_AABB_Z = Block.box(8.0 - HALF_SIZE, 16.0 - PRESSED_DEPTH, 8.0 - HALF_SIZE, 8.0 + HALF_SIZE, 16.0, 8.0 + HALF_SIZE);
	private static final VoxelShape PRESSED_FLOOR_AABB_X = Block.box(8.0 - HALF_SIZE, 0.0, 8.0 - HALF_SIZE, 8.0 + HALF_SIZE, PRESSED_DEPTH, 8.0 + HALF_SIZE);
	private static final VoxelShape PRESSED_FLOOR_AABB_Z = Block.box(8.0 - HALF_SIZE, 0.0, 8.0 - HALF_SIZE, 8.0 + HALF_SIZE, PRESSED_DEPTH, 8.0 + HALF_SIZE);
	private static final VoxelShape PRESSED_NORTH_AABB = Block.box(8.0 - HALF_SIZE, 8.0 - HALF_SIZE, 16.0 - PRESSED_DEPTH, 8.0 + HALF_SIZE, 8.0 + HALF_SIZE, 16.0);
	private static final VoxelShape PRESSED_SOUTH_AABB = Block.box(8.0 - HALF_SIZE, 8.0 - HALF_SIZE, 0.0, 8.0 + HALF_SIZE, 8.0 + HALF_SIZE, PRESSED_DEPTH);
	private static final VoxelShape PRESSED_WEST_AABB = Block.box(16.0 - PRESSED_DEPTH, 8.0 - HALF_SIZE, 8.0 - HALF_SIZE, 16.0, 8.0 + HALF_SIZE, 8.0 + HALF_SIZE);
	private static final VoxelShape PRESSED_EAST_AABB = Block.box(0.0, 8.0 - HALF_SIZE, 8.0 - HALF_SIZE, PRESSED_DEPTH, 8.0 + HALF_SIZE, 8.0 + HALF_SIZE);

	public BaseBigRedButtonBlock(Properties properties) {
		super(BlockSetType.STONE, SharedConstants.TICKS_PER_SECOND * 2, properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, Boolean.FALSE).setValue(FACE, AttachFace.WALL));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction direction = state.getValue(FACING);
		boolean pressed = state.getValue(POWERED);
		return switch (state.getValue(FACE)) {
			case FLOOR -> switch (direction.getAxis()) {
				case X -> pressed ? PRESSED_FLOOR_AABB_X : FLOOR_AABB_X;
				default -> pressed ? PRESSED_FLOOR_AABB_Z : FLOOR_AABB_Z;
			};
			case WALL -> switch (direction) {
				case EAST -> pressed ? PRESSED_EAST_AABB : EAST_AABB;
				case WEST -> pressed ? PRESSED_WEST_AABB : WEST_AABB;
				case SOUTH -> pressed ? PRESSED_SOUTH_AABB : SOUTH_AABB;
				case NORTH, UP, DOWN -> pressed ? PRESSED_NORTH_AABB : NORTH_AABB;
			};
			default -> switch (direction.getAxis()) {
				case X -> pressed ? PRESSED_CEILING_AABB_X : CEILING_AABB_X;
				default -> pressed ? PRESSED_CEILING_AABB_Z : CEILING_AABB_Z;
			};
		};
	}
}
