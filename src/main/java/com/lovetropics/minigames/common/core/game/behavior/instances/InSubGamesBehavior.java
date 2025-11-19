package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.SubGameEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record InSubGamesBehavior(
		IGameBehavior behavior,
		boolean recursive
) implements IGameBehavior {
	public static final MapCodec<InSubGamesBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IGameBehavior.CODEC.fieldOf("behavior").forGetter(InSubGamesBehavior::behavior),
			Codec.BOOL.optionalFieldOf("recursive", false).forGetter(InSubGamesBehavior::recursive)
	).apply(i, InSubGamesBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(SubGameEvents.CREATE, (subGame, subEvents) -> {
			behavior.registerState(subGame, subGame.state(), subGame.instanceState());
			behavior.register(subGame, subEvents);
			if (recursive) {
				register(subGame, subEvents);
			}
		});
	}
}
