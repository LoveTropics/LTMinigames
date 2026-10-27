package org.lovetropics.games.common.core.game.state;

import com.lovetropics.lib.BlockBox;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.CollidersClientState;
import org.lovetropics.games.common.core.game.util.ColliderSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ColliderState implements IGameState {
	public static final GameStateKey<ColliderState> KEY = GameStateKey.create("Colliders");

	private final IGamePhase game;

	private final Set<BlockBox> colliders = new HashSet<>();
	private ColliderSet bakedColliders = ColliderSet.EMPTY;

	private ColliderState(IGamePhase game) {
		this.game = game;
	}

	public static ColliderState getOrAdd(IGamePhase game, EventRegistrar events) {
		ColliderState state = game.state().getOrNull(KEY);
		if (state != null) {
			return state;
		}
		ColliderState newState = new ColliderState(game);
		events.listen(GamePlayerEvents.ADD, player -> GameClientState.sendToPlayer(newState.createClientState(), player));
		events.listen(GamePlayerEvents.REMOVE, player -> GameClientState.removeFromPlayer(GameClientStateTypes.COLLIDERS.get(), player));
		game.state().register(KEY, newState);
		return newState;
	}

	public void addColliders(Collection<BlockBox> boxes) {
		if (colliders.addAll(boxes)) {
			rebakeAndSynchronize();
		}
	}

	public void removeColliders(Collection<BlockBox> boxes) {
		boolean changed = false;
		for (BlockBox box : boxes) {
			changed |= colliders.remove(box);
		}
		if (changed) {
			rebakeAndSynchronize();
		}
	}

	private void rebakeAndSynchronize() {
		bakedColliders = new ColliderSet(List.copyOf(colliders));
		GameClientState.sendToPlayers(createClientState(), game.allPlayers());
	}

	private CollidersClientState createClientState() {
		return new CollidersClientState(bakedColliders);
	}

	public ColliderSet colliders() {
		return bakedColliders;
	}
}
