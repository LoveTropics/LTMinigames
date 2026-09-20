package org.lovetropics.games.common.core.game.persistent.behavior;

import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.BeSpeedyState;
import org.lovetropics.games.common.core.game.persistent.PersistentGame;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehavior;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviorType;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviors;
import com.mojang.serialization.MapCodec;

import java.util.function.Supplier;

public class MakeSpeedyBehavior implements PersistentGameBehavior {
	public static final MapCodec<MakeSpeedyBehavior> CODEC = MapCodec.unit(MakeSpeedyBehavior::new);

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		events.listen(GamePlayerEvents.ADD, player -> {
			GameClientState.sendToPlayer(BeSpeedyState.INSTANCE, player);
		});

		events.listen(GamePlayerEvents.REMOVE, player -> {
			GameClientState.removeFromPlayer(GameClientStateTypes.BE_SPEEDY.get(), player);
		});
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.MAKE_SPEEDY;
	}
}
