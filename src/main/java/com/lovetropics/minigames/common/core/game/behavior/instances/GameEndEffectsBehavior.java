package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.util.TemplatedText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;

import javax.annotation.Nullable;
import java.util.Map;

public final class GameEndEffectsBehavior implements IGameBehavior {
	private static final long NO_STOP_DELAY = -1L;

	public static final MapCodec<GameEndEffectsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.LONG.optionalFieldOf("stop_delay", NO_STOP_DELAY).forGetter(c -> c.stopDelay),
			MoreCodecs.long2Object(TemplatedText.CODEC).optionalFieldOf("scheduled_messages", new Long2ObjectOpenHashMap<>()).forGetter(c -> c.scheduledMessages)
	).apply(i, GameEndEffectsBehavior::new));

	private final long stopDelay;
	private final Long2ObjectMap<TemplatedText> scheduledMessages;

	private boolean ended;
	private long stopTime;

	@Nullable
	private Component winner;

	public GameEndEffectsBehavior(long stopDelay, Long2ObjectMap<TemplatedText> scheduledMessages) {
		this.stopDelay = stopDelay;
		this.scheduledMessages = scheduledMessages;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameLogicEvents.GAME_OVER, winner -> {
			this.winner = ComponentUtils.mergeStyles(winner.name().copy(), Style.EMPTY.withColor(ChatFormatting.AQUA));
			ended = true;
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			if (ended) {
				tickEnded(game);
			}
		});
	}

	private void tickEnded(IGamePhase game) {
		sendScheduledMessages(game, stopTime);

		if (stopDelay != NO_STOP_DELAY && stopTime == stopDelay) {
			game.requestStop(GameStopReason.finished());
		}

		stopTime++;
	}

	private void sendScheduledMessages(IGamePhase game, long stopTime) {
		TemplatedText message = scheduledMessages.remove(stopTime);
		if (message != null) {
			game.allPlayers().sendMessage(message.apply(Map.of("winner", winner)));
		}
	}
}
