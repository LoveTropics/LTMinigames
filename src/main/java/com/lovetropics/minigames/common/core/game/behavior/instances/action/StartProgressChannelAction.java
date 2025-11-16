package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.function.Supplier;

public record StartProgressChannelAction(
		ProgressChannel channel
) implements IGameBehavior {
	public static final MapCodec<StartProgressChannelAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ProgressChannel.CODEC.fieldOf("channel").forGetter(StartProgressChannelAction::channel)
	).apply(i, StartProgressChannelAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		ProgressHolder holder = channel.getOrThrow(game);
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			holder.start();
			return true;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.START_PROGRESS_CHANNEL;
	}
}
