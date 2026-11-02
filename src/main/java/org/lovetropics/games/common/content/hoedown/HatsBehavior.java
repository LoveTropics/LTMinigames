package org.lovetropics.games.common.content.hoedown;

import com.mojang.serialization.MapCodec;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;

public record HatsBehavior(
) implements IGameBehavior {
	public static final MapCodec<HatsBehavior> CODEC = MapCodec.unit(HatsBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		HatsState hats = HatsState.get(game);
		events.listen(GamePlayerEvents.ADD, hats::applyHatsFromTransfer);
		events.listen(GamePlayerEvents.REMOVE, hats::storeHatsForTransfer);
	}

	@Override
	public MapCodec<HatsBehavior> codec() {
		return CODEC;
	}
}
