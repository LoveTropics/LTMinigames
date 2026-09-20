package org.lovetropics.games.common.core.game.behavior.instances.action;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.state.progress.ProgressChannel;
import org.lovetropics.games.common.core.game.state.progress.ProgressHolder;
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
