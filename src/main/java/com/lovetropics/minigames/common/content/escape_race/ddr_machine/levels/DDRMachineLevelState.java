package com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels;

import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.stream.Stream;

public class DDRMachineLevelState {

	private static final List<MutableComponent> POSITIVE_PHRASES = List.of(
			Component.translatable("ltminigames.minigame.escape_race.ddr.positive.nice_one"),
			Component.translatable("ltminigames.minigame.escape_race.ddr.positive.close_enough"),
			Component.translatable("ltminigames.minigame.escape_race.ddr.positive.sick"),
			Component.translatable("ltminigames.minigame.escape_race.ddr.positive.smashedit"),
			Component.translatable("ltminigames.minigame.escape_race.ddr.positive.poppingoff"),
			Component.translatable("ltminigames.minigame.escape_race.ddr.positive.incredible")
	);
	private static final Component PERFECT_SCORE_MESSAGE = Component.translatable("ltminigames.minigame.escape_race.ddr.positive.perfect");
	private static final Component STREAK_BROKEN_MESSAGE = Component.translatable("ltminigames.minigame.escape_race.ddr.negative.streak_broken").withStyle(ChatFormatting.RED);

	private static final int SCORE_PER_TICK = 10;
	private static final int TICK_RANGE_EITHER_SIDE = 5;
	private static final int PERFECT_SCORE = TICK_RANGE_EITHER_SIDE * SCORE_PER_TICK;

	private final Holder<DdrLevel> level;
	private final DdrLevelInputHandler inputHandler;
	private int currentLevelScore = 0;
	private int highestStreak = 0;
	private int currentLevelStreak = 0;

	private final RandomSource random = RandomSource.create();

	public DDRMachineLevelState(Holder<DdrLevel> level) {
		this.level = level;
		inputHandler = new DdrLevelInputHandler(TICK_RANGE_EITHER_SIDE, level);
	}

	private Component pickMessage(int score) {
		if (score == PERFECT_SCORE) {
			return PERFECT_SCORE_MESSAGE;
		}
		return Util.getRandom(POSITIVE_PHRASES, random);
	}

	public Holder<DdrLevel> getLevel() {
		return level;
	}

	public Stream<TimedDdrInput> pendingInputs() {
		return inputHandler.pendingInputs();
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

	public void checkIfHit(int currentTick, DdrInput newInput, ServerPlayer player) {
		DdrLevelInputHandler.Result result = inputHandler.handleInput(newInput, currentTick);
		if (result.missedCount() > 0) {
			if (currentLevelStreak > 0) {
				currentLevelStreak = 0;
				sendSound(player, SoundEvents.SHIELD_BREAK.value());
				player.sendSystemMessage(STREAK_BROKEN_MESSAGE, true);
			}
		}

		// TODO: Score based on whether all inputs matched?
		DdrLevelInputHandler.Hit hit = result.hit();
		if (hit != null) {
			addScore(player, computeScore(hit));
		}
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

	private static int computeScore(DdrLevelInputHandler.Hit hit) {
		return Math.max(PERFECT_SCORE - (hit.deviationTicks() * SCORE_PER_TICK), 0);
	}
}
