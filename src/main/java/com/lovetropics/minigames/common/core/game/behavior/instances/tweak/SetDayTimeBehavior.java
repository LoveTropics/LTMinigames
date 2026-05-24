package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SetDayTimeBehavior(long time) implements IGameBehavior {
	public static final MapCodec<SetDayTimeBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.LONG.fieldOf("time").forGetter(c -> c.time)
	).apply(i, SetDayTimeBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		// Todo 26.1 Port
//		game.level().setDayTime(time);
	}
}
