package com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels;

import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

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


	private final Holder<DdrLevel> level;
	private final Long2ObjectMap<DDRMachineLevelTickState> tickStates = new Long2ObjectOpenHashMap<>();
	private int currentLevelScore = 0;
	private int highestStreak = 0;
	private int currentLevelStreak = 0;

	public DDRMachineLevelState(Holder<DdrLevel> level) {
		this.level = level;
		for (Long2ObjectMap.Entry<DdrInput> entry : level.value().ticks().long2ObjectEntrySet()) {
			tickStates.put(entry.getLongKey(), new DDRMachineLevelTickState(entry.getValue()));
		}
	}

	public Holder<DdrLevel> getLevel() {
		return level;
	}

	public Long2ObjectMap<DDRMachineLevelTickState> getTickStates() {
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
		player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.MASTER, pos.x, pos.y, pos.z, 1f, 1.0f, 0));
	}

	public void checkIfHit(int currentTick, DdrInput newInput, ServerPlayer player) {
		boolean hasChanged = !newInput.isEmpty();
		List<Map.Entry<Integer, DDRMachineLevelTickState>> withinRange = tickStates.long2ObjectEntrySet().stream()
				.filter((entry) -> entry.getLongKey() >= currentTick - TICK_RANGE_EITHER_SIDE && entry.getLongKey() <= currentTick + TICK_RANGE_EITHER_SIDE && !entry.getValue().wasHit())
				// TODO
				.map(e -> Map.entry((int) e.getLongKey(), e.getValue()))
				.toList();
		for (Map.Entry<Integer, DDRMachineLevelTickState> entry : withinRange) {
			int processingTick = entry.getKey();
			if(processingTick == currentTick){
				DDRMachineLevelTickState value = entry.getValue();
				if(hasChanged && ((newInput.back() && value.tick.back()) || (newInput.forward() && value.tick.forward()) ||
						(newInput.left() && value.tick.left()) || (newInput.right() && value.tick.right()))) {
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
				if(hasChanged &&(newInput.back() && value.tick.back()) || (newInput.forward() && value.tick.forward()) ||
						(newInput.left() && value.tick.left()) || (newInput.right() && value.tick.right())) {
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
		private final DdrInput tick;
		private boolean hit;

		public DDRMachineLevelTickState(DdrInput tick) {
			this.tick = tick;
		}

		public DdrInput getTick() {
			return tick;
		}

		public boolean wasHit() {
			return hit;
		}

		public void setHit(boolean hit) {
			this.hit = hit;
		}
	}
}
