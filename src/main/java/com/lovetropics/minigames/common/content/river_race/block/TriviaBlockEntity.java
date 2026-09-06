package com.lovetropics.minigames.common.content.river_race.block;

import com.lovetropics.minigames.common.content.river_race.behaviour.TriviaBehaviour;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

public class TriviaBlockEntity extends BlockEntity implements HasTrivia {

	public record TriviaBlockState(boolean isAnswered, long unlocksAt) {
		public static final StreamCodec<ByteBuf, TriviaBlockState> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, TriviaBlockState::isAnswered,
				ByteBufCodecs.VAR_LONG, TriviaBlockState::unlocksAt,
				TriviaBlockState::new
		);

		public boolean lockedOut() {
			return unlocksAt > 0;
		}
	}

	public static final String TAG_QUESTION = "question";
	public static final String TAG_UNLOCKS_AT = "unlocksAt";
	private TriviaBehaviour.@Nullable TriviaQuestion question;
	private long unlocksAt;
	private final TriviaType triviaType;

	public TriviaBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
		super(type, pos, blockState);
		if (blockState.getBlock() instanceof TriviaBlock triviaBlock) {
			triviaType = triviaBlock.getType();
		} else {
			throw new IllegalArgumentException("Cannot create TriviaBlockEntity for unrecognised block type: " + blockState);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.storeNullable(TAG_QUESTION, TriviaBehaviour.TriviaQuestion.CODEC, question);
		if (unlocksAt > 0) {
			output.putLong(TAG_UNLOCKS_AT, unlocksAt);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		question = input.read(TAG_QUESTION, TriviaBehaviour.TriviaQuestion.CODEC).orElse(null);
		unlocksAt = input.getLongOr(TAG_UNLOCKS_AT, 0);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putLong(TAG_UNLOCKS_AT, unlocksAt);
		return tag;
	}

	@Override
	public void onDataPacket(Connection net, ValueInput input) {
		handleUpdateTag(input);
	}

	@Override
	public void handleUpdateTag(ValueInput input) {
		unlocksAt = input.getLongOr(TAG_UNLOCKS_AT, 0);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
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
	public TriviaBehaviour.@Nullable TriviaQuestion getQuestion() {
		return question;
	}

	@Override
	public TriviaType getTriviaType() {
		return triviaType;
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
	public TriviaBlockState getState() {
		return new TriviaBlockState(isAnswered(), unlocksAt);
	}
}
