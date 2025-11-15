package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.util.TemplatedText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.context.ContextMap;

import java.util.LinkedList;
import java.util.Map;

public final class CountdownAction<T> implements IGameBehavior {
	public static final MapCodec<CountdownAction<?>> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.LONG.fieldOf("countdown").forGetter(c -> c.countdown / 20),
			TemplatedText.CODEC.fieldOf("warning").forGetter(c -> c.warning),
			GameActionList.MAP_CODEC.forGetter(c -> c.actions)
	).apply(i, CountdownAction::new));

	private final long countdown;
	private final TemplatedText warning;
	private final GameActionList<T> actions;

	private final LinkedList<QueueEntry<T>> queue = new LinkedList<>();

	public CountdownAction(long countdown, TemplatedText warning, GameActionList<T> actions) {
		this.countdown = countdown * 20;
		this.warning = warning;
		this.actions = actions;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		actions.register(game, events);

		actions.target.listenAndCaptureSource(events, (context, objects) ->
				queue.add(new QueueEntry<>(game.ticks() + countdown, context, objects))
		);

		events.listen(GamePhaseEvents.TICK, () -> {
			if (!queue.isEmpty()) {
				queue.removeIf(entry -> tickQueuedAction(game, entry));
			}
		});
	}

	private boolean tickQueuedAction(IGamePhase game, QueueEntry<T> entry) {
		long remainingTicks = entry.time() - game.ticks();
		if (remainingTicks <= 0) {
			return actions.apply(game, entry.context, entry.sources);
		} else {
			for (ServerPlayer player : game.allPlayers()) {
				tickCountdown(player, remainingTicks);
			}
			return false;
		}
	}

	private void tickCountdown(ServerPlayer player, long remainingTicks) {
		if (remainingTicks % SharedConstants.TICKS_PER_SECOND == 0) {
			long remainingSeconds = remainingTicks / SharedConstants.TICKS_PER_SECOND;
			MutableComponent timeText = Component.literal(String.valueOf(remainingSeconds)).withStyle(ChatFormatting.GOLD);
			player.displayClientMessage(warning.apply(Map.of("time", timeText)), true);
			player.playNotifySound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.MASTER, 0.8F, 1.0F);
		}
	}

	private record QueueEntry<T>(long time, ContextMap context, Iterable<T> sources) {
	}
}
