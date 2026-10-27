package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;

import java.util.function.Supplier;

public record SetTimeSpeedBehavior(int factor) implements IGameBehavior {
	public static final MapCodec<SetTimeSpeedBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("factor").forGetter(c -> c.factor)
	).apply(i, SetTimeSpeedBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameWorldEvents.CREATE_LEVEL, (level, _) ->
				level.dimensionType().defaultClock().ifPresent(clock ->
						level.clockManager().setRate(clock, factor)
				)
		);
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.SET_TIME_SPEED;
	}
}
