package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPoint;
import com.lovetropics.minigames.common.util.LinearSpline;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public record CountdownEffectsBehavior(ProgressChannel channel, ProgressionPoint target, int seconds, SoundEvent sound, float volume, int startColor, int endColor, LinearSpline pitch, boolean showTitle) implements IGameBehavior {
	public static final MapCodec<CountdownEffectsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ProgressChannel.CODEC.optionalFieldOf("channel", ProgressChannel.MAIN).forGetter(CountdownEffectsBehavior::channel),
			ProgressionPoint.CODEC.fieldOf("target").forGetter(CountdownEffectsBehavior::target),
			Codec.INT.fieldOf("seconds").forGetter(CountdownEffectsBehavior::seconds),
			BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("sound").forGetter(CountdownEffectsBehavior::sound),
			Codec.FLOAT.optionalFieldOf("volume", 1.0f).forGetter(CountdownEffectsBehavior::volume),
			Codec.INT.optionalFieldOf("start_color", 0x55ff55).forGetter(CountdownEffectsBehavior::startColor),
			Codec.INT.optionalFieldOf("end_color", 0xffaa00).forGetter(CountdownEffectsBehavior::endColor),
			LinearSpline.CODEC.optionalFieldOf("pitch", LinearSpline.constant(1.0f)).forGetter(CountdownEffectsBehavior::pitch),
			Codec.BOOL.optionalFieldOf("show_title", true).forGetter(CountdownEffectsBehavior::showTitle)
	).apply(i, CountdownEffectsBehavior::new));

	private static final int WAITING_FOR_COUNTDOWN = -1;
	private static final int COMPLETED_COUNTDOWN = -2;

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		final ProgressHolder progression = channel.getOrThrow(game);

		final AtomicInteger countdownTo = new AtomicInteger(WAITING_FOR_COUNTDOWN);
		events.listen(GamePhaseEvents.TICK, () -> {
			int targetTick = countdownTo.getPlain();
			if (targetTick == WAITING_FOR_COUNTDOWN) {
				targetTick = target.resolve(progression);
				if (shouldStart(progression, targetTick)) {
					countdownTo.setPlain(targetTick);
				} else {
					return;
				}
			} else if (targetTick == COMPLETED_COUNTDOWN) {
				return;
			}
			if (tickCounting(game, targetTick - progression.time())) {
				countdownTo.set(COMPLETED_COUNTDOWN);
			}
		});
	}

	private boolean shouldStart(final ProgressHolder progression, final int targetTick) {
		final int startTick = targetTick - seconds * SharedConstants.TICKS_PER_SECOND;
		return progression.time() >= startTick;
	}

	private boolean tickCounting(final IGamePhase game, final int ticksLeft) {
		if (ticksLeft % SharedConstants.TICKS_PER_SECOND == 0) {
			final int secondsLeft = ticksLeft / SharedConstants.TICKS_PER_SECOND;
			if (secondsLeft > 0) {
				showCountdown(game, secondsLeft);
			} else {
				return true;
			}
		}
		return false;
	}

	private void showCountdown(final IGamePhase game, final int secondsLeft) {
		final float delta = Mth.inverseLerp(secondsLeft, seconds, 1);
		final float pitch = this.pitch.get(delta);
		final PlayerSet players = game.allPlayers();
		players.playSound(sound, SoundSource.MASTER, 1.0f, pitch);

		if (showTitle) {
			final int color = ARGB.lerp(delta, startColor, endColor);
			final Component title = Component.literal(".." + secondsLeft).withStyle(Style.EMPTY.withColor(color));
			players.showTitle(title, 4, SharedConstants.TICKS_PER_SECOND, 4);
		}
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.COUNTDOWN_EFFECTS;
	}
}
