package org.lovetropics.games.common.core.game.persistent.behavior;

import com.mojang.serialization.MapCodec;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.persistent.PersistentGame;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehavior;

public class NoOpBehavior implements PersistentGameBehavior {
	public static final MapCodec<NoOpBehavior> CODEC = MapCodec.unit(NoOpBehavior::new);

	@Override
	public void register(PersistentGame game, EventRegistrar events) {

	}

	@Override
	public MapCodec<NoOpBehavior> codec() {
		return CODEC;
	}
}
