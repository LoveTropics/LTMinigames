package com.lovetropics.minigames.common.content.river_race.block;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.river_race.RiverRace;
import com.lovetropics.minigames.common.content.river_race.behaviour.TriviaBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;

public class TriviaChestBlockEntity extends ChestBlockEntity implements HasTrivia {
	@Nullable
	private TriviaBehaviour.TriviaQuestion question;
	private long unlocksAt;

	public TriviaChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
		super(type, pos, blockState);
	}

	public TriviaChestBlockEntity(BlockPos pos, BlockState blockState) {
		this(RiverRace.TRIVIA_CHEST_BLOCK_ENTITY.get(), pos, blockState);
	}

	@Override
	public Component getName() {
		return Component.translatable(LoveTropics.ID + ".container.triviaChest");
	}

	@Override
	protected Component getDefaultName() {
		return getName();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.storeNullable(TriviaBlockEntity.TAG_QUESTION, TriviaBehaviour.TriviaQuestion.CODEC, question);
		if (unlocksAt > 0) {
			output.putLong(TriviaBlockEntity.TAG_UNLOCKS_AT, unlocksAt);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		question = input.read(TriviaBlockEntity.TAG_QUESTION, TriviaBehaviour.TriviaQuestion.CODEC).orElse(null);
		unlocksAt = input.getLongOr(TriviaBlockEntity.TAG_UNLOCKS_AT, 0);
	}

	private void markUpdated() {
		setChanged();
		level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
	}

	@Override
	public void setQuestion(TriviaBehaviour.TriviaQuestion question) {
		this.question = question;
		markUpdated();
	}

	@Override
	@Nullable
	public TriviaBehaviour.TriviaQuestion getQuestion() {
		return question;
	}

	@Override
	public TriviaType getTriviaType() {
		return TriviaType.REWARD;
	}

	@Override
	public long lockout(int lockoutSeconds) {
		unlocksAt = level.getGameTime() + (lockoutSeconds * 20L);
		markUpdated();
		return unlocksAt;
	}

	@Override
	public void unlock() {
		unlocksAt = 0;
		markUpdated();
	}

	@Override
	public boolean markAsCorrect() {
		if (isAnswered()) {
			return false;
		}
		level.setBlockAndUpdate(getBlockPos(), getBlockState().setValue(TriviaBlock.ANSWERED, true));
		markUpdated();
		return true;
	}

	@Override
	public boolean isAnswered() {
		return getBlockState().getValue(TriviaBlock.ANSWERED);
	}

	@Override
	public TriviaBlockEntity.TriviaBlockState getState() {
		return new TriviaBlockEntity.TriviaBlockState(isAnswered(), unlocksAt);
	}
}
