package com.lovetropics.minigames.common.content.survive_the_tide.block;

import com.lovetropics.minigames.common.content.survive_the_tide.SurviveTheTide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class BigRedButtonBlock extends BaseBigRedButtonBlock implements EntityBlock {
	public static final BooleanProperty TRIGGERED = BooleanProperty.create("triggered");

	public BigRedButtonBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, Boolean.FALSE).setValue(FACE, AttachFace.WALL).setValue(TRIGGERED, false));
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BigRedButtonBlockEntity entity) {
			if (!state.getValue(TRIGGERED)) {
				entity.press();
				return InteractionResult.SUCCESS;
			}
		}
		return super.useWithoutItem(state, level, pos, player, hit);
	}

	public static void trigger(BlockState state, Level level, BlockPos pos) {
		level.setBlock(pos, state.setValue(POWERED, true).setValue(TRIGGERED, true), Block.UPDATE_ALL);
		level.updateNeighborsAt(pos, state.getBlock());
		level.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), state.getBlock());

		level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS);
		level.gameEvent(null, GameEvent.BLOCK_ACTIVATE, pos);
	}

	@Override
	protected void checkPressed(BlockState state, Level level, BlockPos pos) {
		if (!state.getValue(TRIGGERED)) {
			super.checkPressed(state, level, pos);
		}
	}

	@Override
	public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
		return state.getValue(TRIGGERED) ? 15 : 0;
	}

	@Override
	public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
		return state.getValue(TRIGGERED) && getConnectedDirection(state) == side ? 15 : 0;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(TRIGGERED);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BigRedButtonBlockEntity(SurviveTheTide.BIG_RED_BUTTON_ENTITY.get(), pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return null;
		}
		return createTickerHelper(type, SurviveTheTide.BIG_RED_BUTTON_ENTITY.get(), BigRedButtonBlockEntity::tick);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> actualType, BlockEntityType<E> type, BlockEntityTicker<? super E> ticker) {
		return type == actualType ? (BlockEntityTicker<A>) ticker : null;
	}
}
