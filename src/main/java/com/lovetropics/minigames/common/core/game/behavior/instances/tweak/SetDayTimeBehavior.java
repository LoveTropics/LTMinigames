package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SetDayTimeBehavior(long time) implements IGameBehavior {
	public static final Codec<SetDayTimeBehavior> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("time").forGetter(c -> c.time)
	).apply(i, SetDayTimeBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		game.getWorld().setDayTime(this.time);
	}
}
