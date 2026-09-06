package com.lovetropics.minigames.common.content.survive_the_tide.block;

import com.lovetropics.minigames.common.content.survive_the_tide.SurviveTheTide;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;

public class LootDispenserBlock extends BaseEntityBlock {
	public static final MapCodec<LootDispenserBlock> CODEC = simpleCodec(LootDispenserBlock::new);

	public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
	public static final EnumProperty<State> STATE = EnumProperty.create("state", State.class);

	public LootDispenserBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STATE, State.INACTIVE));
	}

	@Override
	protected MapCodec<LootDispenserBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING, STATE);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LootDispenserBlockEntity(SurviveTheTide.LOOT_DISPENSER_ENTITY.get(), pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return null;
		}
		return createTickerHelper(type, SurviveTheTide.LOOT_DISPENSER_ENTITY.get(), LootDispenserBlockEntity::tick);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
		boolean hasSignal = level.hasNeighborSignal(pos);
		if (hasSignal && state.getValue(STATE) == State.INACTIVE) {
			level.setBlock(pos, state.setValue(STATE, State.ACTIVE), Block.UPDATE_ALL);
		}
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	public enum State implements StringRepresentable {
		INACTIVE("inactive"),
		ACTIVE("active"),
		CLOGGED("clogged"),
		;

		private final String name;

		State(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}
}
