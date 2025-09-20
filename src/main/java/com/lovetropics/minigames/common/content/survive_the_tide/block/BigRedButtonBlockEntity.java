package com.lovetropics.minigames.common.content.survive_the_tide.block;

import com.lovetropics.minigames.common.content.survive_the_tide.SurviveTheTide;
import com.lovetropics.minigames.common.core.game.IGameManager;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class BigRedButtonBlockEntity extends BlockEntity {
	private static final String TAG_PRESSED = "pressed";
	private static final String TAG_PRESENT = "present";
	private static final String TAG_REQUIREMENTS = "requirements";
	private static final String TAG_TRIGGER_POS = "trigger_pos";

	private boolean pressed;

	private Requirements requirements = Requirements.DEFAULT;
	private int playersPresentCount;
	private int playersRequiredCount;

	@Nullable
	private BlockPos triggerPos;

	public BigRedButtonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BigRedButtonBlockEntity entity) {
		if (state.getValue(BigRedButtonBlock.TRIGGERED)) {
			return;
		}

		IGamePhase game = IGameManager.get().getGamePhaseAt(level, pos);
		int smallestTeamSize = game != null ? getSmallestTeamSize(game) : 1;
		int requiredCount = entity.requirements.resolve(smallestTeamSize);
		int presentCount = game != null && entity.pressed ? entity.countPlayersPresent(game, pos) : 0;
		entity.updatePlayerCount(presentCount, requiredCount);

		if (entity.pressed && presentCount >= requiredCount) {
			entity.trigger();
			entity.pressed = false;
		}
	}

	private int countPlayersPresent(IGamePhase game, BlockPos pos) {
		int count = 0;
		for (ServerPlayer player : game.participants()) {
			if (pos.closerToCenterThan(player.position(), requirements.distance)) {
				count++;
			}
		}
		return count;
	}

	private static int getSmallestTeamSize(IGamePhase game) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		if (teams == null) {
			return 1;
		}
		int smallestTeamSize = game.participants().size();
		for (GameTeam team : teams) {
			int teamSize = teams.getParticipantsForTeam(game, team.key()).size();
			if (teamSize > 0 && teamSize < smallestTeamSize) {
				smallestTeamSize = teamSize;
			}
		}
		return smallestTeamSize;
	}

	private void updatePlayerCount(int presentCount, int requiredCount) {
		if (presentCount == playersPresentCount && requiredCount == playersRequiredCount) {
			return;
		}
		playersPresentCount = presentCount;
		playersRequiredCount = requiredCount;
		markUpdated();
	}

	public void press() {
		// No game is active, just allow instant trigger
		if (IGameManager.get().getGamePhaseAt(level, getBlockPos()) == null) {
			trigger();
			return;
		}
		pressed = true;
	}

	private void trigger() {
		BigRedButtonBlock.trigger(getBlockState(), level, getBlockPos());
		if (triggerPos != null) {
			BlockState triggerState = level.getBlockState(triggerPos);
			if (triggerState.is(SurviveTheTide.LOOT_DISPENSER) && triggerState.getValue(LootDispenserBlock.STATE) == LootDispenserBlock.State.INACTIVE) {
				level.setBlockAndUpdate(triggerPos, triggerState.setValue(LootDispenserBlock.STATE, LootDispenserBlock.State.ACTIVE));
			}
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean(TAG_PRESSED, pressed);
		output.store(TAG_REQUIREMENTS, Requirements.CODEC, requirements);
		output.storeNullable(TAG_TRIGGER_POS, BlockPos.CODEC, triggerPos);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		pressed = input.getBooleanOr(TAG_PRESSED, false);
		requirements = input.read(TAG_REQUIREMENTS, Requirements.CODEC).orElse(Requirements.DEFAULT);
		triggerPos = input.read(TAG_TRIGGER_POS, BlockPos.CODEC).orElse(null);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putInt(TAG_PRESENT, playersPresentCount);
		tag.putInt(TAG_REQUIREMENTS, playersRequiredCount);
		return tag;
	}

	@Override
	public void onDataPacket(Connection net, ValueInput input) {
		handleUpdateTag(input);
	}

	@Override
	public void handleUpdateTag(ValueInput input) {
		playersPresentCount = input.getIntOr(TAG_PRESENT, 0);
		playersRequiredCount = input.getIntOr(TAG_REQUIREMENTS, 0);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	private void markUpdated() {
		setChanged();
		level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
	}

	public int getPlayersPresentCount() {
		return playersPresentCount;
	}

	public int getPlayersRequiredCount() {
		return playersRequiredCount;
	}

	private record Requirements(float percent, int count, float distance) {
		public static final Requirements DEFAULT = new Requirements(0.0f, 1, 4.0f);

		public static final Codec<Requirements> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.FLOAT.optionalFieldOf("percent", DEFAULT.percent).forGetter(Requirements::percent),
				Codec.INT.optionalFieldOf("count", DEFAULT.count).forGetter(Requirements::count),
				Codec.FLOAT.optionalFieldOf("distance", DEFAULT.distance).forGetter(Requirements::distance)
		).apply(i, Requirements::new));

		public int resolve(int smallestTeamSize) {
			int preferredCount = Math.max(
					Mth.floor(percent * smallestTeamSize),
					count
			);
			return Math.min(preferredCount, smallestTeamSize);
		}
	}
}
