package com.lovetropics.minigames.common.content.river_race.block;

import com.lovetropics.minigames.common.content.river_race.RiverRace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public class TriviaBlock extends Block implements EntityBlock {

	public static class RewardTriviaBlock extends TriviaBlock {
		public RewardTriviaBlock(Properties properties) {
			super(properties, TriviaType.REWARD);
		}
	}

	public static class GateTriviaBlock extends TriviaBlock {
		public GateTriviaBlock(Properties properties) {
			super(properties, TriviaType.GATE);
		}
	}

	public static class CollectableTriviaBlock extends TriviaBlock {
		public CollectableTriviaBlock(Properties properties) {
			super(properties, TriviaType.COLLECTABLE);
		}
	}

	public static class VictoryTriviaBlock extends TriviaBlock {
		public VictoryTriviaBlock(Properties properties) {
			super(properties, TriviaType.VICTORY);
		}
	}

	public static final BooleanProperty ANSWERED = BooleanProperty.create("answered");

	private final TriviaType type;

	public TriviaBlock(Properties properties, TriviaType type) {
		super(properties);
		this.type = type;
		registerDefaultState(getStateDefinition().any().setValue(ANSWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ANSWERED);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new TriviaBlockEntity(RiverRace.TRIVIA_BLOCK_ENTITY.get(), blockPos, blockState);
	}

	public TriviaType getType() {
		return type;
	}
}
