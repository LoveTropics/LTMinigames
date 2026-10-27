package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;

public record SetDayTimeBehavior(long time) implements IGameBehavior {
	public static final MapCodec<SetDayTimeBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.LONG.fieldOf("time").forGetter(c -> c.time)
	).apply(i, SetDayTimeBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameWorldEvents.CREATE_LEVEL, level ->
				level.dimensionType().defaultClock().ifPresent(clock -> level.clockManager().setTotalTicks(clock, time))
		);
	}
}
