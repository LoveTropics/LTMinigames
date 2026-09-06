package com.lovetropics.minigames.common.content.river_race.block;

import com.lovetropics.minigames.common.content.river_race.RiverRace;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

public class TriviaChestBlock extends AbstractChestBlock<ChestBlockEntity> {
	public static final MapCodec<TriviaChestBlock> CODEC = simpleCodec(TriviaChestBlock::new);
	private static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	private static final EnumProperty<ChestType> TYPE = BlockStateProperties.CHEST_TYPE;
	public static final BooleanProperty ANSWERED = TriviaBlock.ANSWERED;
	private static final VoxelShape AABB = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

	public TriviaChestBlock(Properties properties) {
		super(properties, RiverRace.TRIVIA_CHEST_BLOCK_ENTITY::get);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false).setValue(ANSWERED, false).setValue(TYPE, ChestType.SINGLE));
	}

	@Override
	public MapCodec<? extends TriviaChestBlock> codec() {
		return CODEC;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (state.getValue(WATERLOGGED)) {
			scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return AABB;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState()
				.setValue(FACING, context.getHorizontalDirection().getOpposite())
				.setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		Containers.updateNeighboursAfterDestroy(state, level, pos);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level instanceof ServerLevel serverLevel) {
			MenuProvider menuProvider = getMenuProvider(state, level, pos);
			if (menuProvider != null) {
				player.openMenu(menuProvider);
				player.awardStat(Stats.CUSTOM.get(Stats.OPEN_CHEST));
				PiglinAi.angerNearbyPiglins(serverLevel, player, true);
			}
			return InteractionResult.CONSUME;
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combine(BlockState state, Level level, BlockPos pos, boolean override) {
		return DoubleBlockCombiner.Combiner::acceptNone;
	}

	@Override
	protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof TriviaChestBlockEntity chest ? chest : null;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TriviaChestBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
		return level.isClientSide() ? createTickerHelper(blockEntityType, this.blockEntityType.get(), ChestBlockEntity::lidAnimateTick) : null;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED, TYPE, ANSWERED);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
			chest.recheckOpen();
		}
	}
}
