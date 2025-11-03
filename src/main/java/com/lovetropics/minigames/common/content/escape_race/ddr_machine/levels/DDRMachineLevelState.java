package com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DDRMachineLevelState {

	private static final List<MutableComponent> POSITIVE_PHRASES = List.of(
			Component.translatable("ltminigames.minigame.escape_race.ddr.positive.nice_one"),
			Component.translatable("ltminigames.minigame.escape_race.ddr.positive.close_enough"),
			Component.translatable("ltminigames.minigame.escape_race.ddr.positive.sick"),
			Component.translatable("ltminigames.minigame.escape_race.ddr.positive.smashedit"),
			Component.translatable("ltminigames.minigame.escape_race.ddr.positive.poppingoff"),
			Component.translatable("ltminigames.minigame.escape_race.ddr.positive.incredible")
	);

	private static final int PERFECT_SCORE = 50;
	private static final int TICK_RANGE_EITHER_SIDE = 5;

	public static MutableComponent pickRandom(){
		return POSITIVE_PHRASES.get((int)(Math.random()*POSITIVE_PHRASES.size())).copy();
	}


	private final DDRMachineLevel level;
	private final Map<Integer, DDRMachineLevelTickState> tickStates;
	private int currentLevelScore = 0;
	private int highestStreak = 0;
	private int currentLevelStreak = 0;

	public DDRMachineLevelState(DDRMachineLevel level) {
		this.level = level;
		this.tickStates = new HashMap<>();
		for (Map.Entry<String, DDRMachineLevelTick> entry : level.ticks().entrySet()) {
			tickStates.put(Integer.parseInt(entry.getKey()), new DDRMachineLevelTickState(entry.getValue()));
		}
	}

	public DDRMachineLevel getLevel() {
		return level;
	}

	public Map<Integer, DDRMachineLevelTickState> getTickStates() {
		return tickStates;
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
		player.connection.send(new ClientboundSoundPacket(Holder.direct(sound), SoundSource.MASTER, pos.x, pos.y, pos.z, 1f, 1.0f, 0));
	}

	public void checkIfHit(int currentTick, boolean hasChanged, PlayerMoveTickState playerState, ServerPlayer player) {
		List<Map.Entry<Integer, DDRMachineLevelTickState>> withinRange = tickStates.entrySet().stream()
				.filter((entry) -> entry.getKey() >= currentTick - TICK_RANGE_EITHER_SIDE && entry.getKey() <= currentTick + TICK_RANGE_EITHER_SIDE && !entry.getValue().wasHit()).toList();
		for (Map.Entry<Integer, DDRMachineLevelTickState> entry : withinRange) {
			int processingTick = entry.getKey();
			if(processingTick == currentTick){
				DDRMachineLevelTickState value = entry.getValue();
				if(hasChanged && ((playerState.back && value.tick.back()) || (playerState.forward && value.tick.forward()) ||
						(playerState.left && value.tick.left()) || (playerState.right && value.tick.right()))) {
					// Exactly on!
					currentLevelScore += PERFECT_SCORE;
					this.increaseStreak();
					entry.getValue().setHit(true);
					sendSound(player, SoundEvents.ARROW_HIT_PLAYER);
					player.sendSystemMessage(Component.translatable("ltminigames.minigame.escape_race.ddr.positive.perfect").withStyle(ChatFormatting.GREEN).append(Component.literal(" +" + PERFECT_SCORE).withStyle(ChatFormatting.GOLD)), true);
				}
			} else {
				DDRMachineLevelTickState value = entry.getValue();
				int diffInTicks = Mth.clamp(Mth.abs(currentTick - processingTick), 0, 5);
				if(hasChanged &&(playerState.back && value.tick.back()) || (playerState.forward && value.tick.forward()) ||
						(playerState.left && value.tick.left()) || (playerState.right && value.tick.right())) {
					int score = Mth.clamp(PERFECT_SCORE - (diffInTicks * 10), 0, PERFECT_SCORE);
					currentLevelScore += score;
					this.increaseStreak();
					entry.getValue().setHit(true);
					sendSound(player, SoundEvents.NOTE_BLOCK_BELL.value());
					player.sendSystemMessage(pickRandom().withStyle(ChatFormatting.GREEN).append(Component.literal(" +" + score).withStyle(ChatFormatting.GOLD)), true);
				} else if(currentTick - processingTick >= 5) {
					if(currentLevelStreak > 0) {
						currentLevelStreak = 0;
						sendSound(player, SoundEvents.SHIELD_BREAK.value());
						player.sendSystemMessage(Component.translatable("ltminigames.minigame.escape_race.ddr.negative.streak_broken").withStyle(ChatFormatting.RED), true);
					}
				}
			}
		}
	}

	public static class DDRMachineLevelTickState {
		private final DDRMachineLevelTick tick;
		private boolean hit;

		public DDRMachineLevelTickState(DDRMachineLevelTick tick) {
			this.tick = tick;
		}

		public DDRMachineLevelTick getTick() {
			return tick;
		}

		public boolean wasHit() {
			return hit;
		}

		public void setHit(boolean hit) {
			this.hit = hit;
		}
	}

	public record PlayerMoveTickState(boolean forward, boolean back, boolean left, boolean right) {}
}
