package com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels;

import com.lovetropics.minigames.common.content.escape_race.EscapeRaceTexts;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DdrServerSession {
	private static final int PERFECT_TOLERANCE = 1;
	public static final int TICK_RANGE_EITHER_SIDE = PERFECT_TOLERANCE + 5;
	private static final int SCORE_PER_TICK = 10;
	private static final int PERFECT_SCORE = (TICK_RANGE_EITHER_SIDE - PERFECT_TOLERANCE) * SCORE_PER_TICK;

	private final Holder<DdrLevel> level;
	private final DdrLevelInputQueue inputQueue;
	private final long startedAtTime;

	private int currentLevelScore = 0;
	private int highestStreak = 0;
	private int currentLevelStreak = 0;

	private final RandomSource random = RandomSource.create();

	public DdrServerSession(Holder<DdrLevel> level, long startedAtTime) {
		this.level = level;
		inputQueue = new DdrLevelInputQueue(TICK_RANGE_EITHER_SIDE, level);
		this.startedAtTime = startedAtTime;
	}

	private Component pickMessage(int score) {
		if (score == PERFECT_SCORE) {
			return EscapeRaceTexts.DDR_PERFECT_SCORE;
		}
		return Util.getRandom(EscapeRaceTexts.DDR_POSITIVE_PHRASES, random);
	}

	public Holder<DdrLevel> level() {
		return level;
	}

	public int getCurrentLevelScore() {
		return currentLevelScore;
	}

	public int getHighestStreak() {
		return highestStreak;
	}

	public int getCurrentLevelStreak() {
		return currentLevelStreak;
	}

	public void increaseStreak(){
		this.currentLevelStreak++;
		if(this.currentLevelStreak > this.highestStreak) {
			this.highestStreak = this.currentLevelStreak;
		}
	}

	private void sendSound(ServerPlayer player, SoundEvent sound) {
		Vec3 pos = player.position();
		player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.MASTER, pos.x, pos.y, pos.z, 1f, 1.0f, 0));
	}

	// No reason not to completely trust the client - doesn't allow them to do something a modified client couldn't do anyway by sending perfectly timed inputs
	public DdrLevelInputQueue.@Nullable Hit handleInputTick(ServerPlayer player, DdrInput newInput, long inputTick) {
		DdrLevelInputQueue.Result result = inputQueue.handleInput(newInput, inputTick);
		if (result.missedCount() > 0) {
			if (currentLevelStreak > 0) {
				currentLevelStreak = 0;
				sendSound(player, SoundEvents.SHIELD_BREAK.value());
				player.sendSystemMessage(EscapeRaceTexts.DDR_STREAK_BROKEN, true);
			}
		}

		// TODO: Score based on whether all inputs matched?
		DdrLevelInputQueue.Hit hit = result.hit();
		if (hit != null) {
			addScore(player, computeScore(hit));
		}

		return hit;
	}

	public boolean isFinished(ServerPlayer player) {
		return currentTick(player) > level.value().lengthInTicks();
	}

	public long currentTick(ServerPlayer player) {
		return Math.max(player.level().getGameTime() - startedAtTime, 0);
	}

	private void addScore(ServerPlayer player, int score) {
		currentLevelScore += score;
		increaseStreak();

		notifyScore(player, score);
	}

	private void notifyScore(ServerPlayer player, int score) {
		if (score == PERFECT_SCORE) {
			sendSound(player, SoundEvents.ARROW_HIT_PLAYER);
		} else {
			sendSound(player, SoundEvents.NOTE_BLOCK_BELL.value());
		}
		player.sendSystemMessage(pickMessage(score).copy().withStyle(ChatFormatting.GREEN).append(Component.literal(" +" + score).withStyle(ChatFormatting.GOLD)), true);
	}

	private static int computeScore(DdrLevelInputQueue.Hit hit) {
		int adjustedTicks = Math.max(hit.deviationTicks() - PERFECT_TOLERANCE, 0);
		return Math.max(PERFECT_SCORE - (adjustedTicks * SCORE_PER_TICK), 0);
	}
}
