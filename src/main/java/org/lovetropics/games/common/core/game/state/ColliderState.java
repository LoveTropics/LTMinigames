package org.lovetropics.games.common.core.game.state;

import com.lovetropics.lib.BlockBox;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.CollidersClientState;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.util.ColliderSet;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ColliderState implements IGameState {
	public static final GameStateKey<ColliderState> KEY = GameStateKey.create("Colliders");

	private final IGamePhase game;

	private final Set<Region> colliders = new HashSet<>();
	private final Map<ResourceKey<Level>, ColliderSet> bakedColliders = new Reference2ObjectOpenHashMap<>();

	private ColliderState(IGamePhase game) {
		this.game = game;
	}

	public static ColliderState getOrAdd(IGamePhase game, EventRegistrar events) {
		ColliderState state = game.state().getOrNull(KEY);
		if (state != null) {
			return state;
		}
		ColliderState newState = new ColliderState(game);
		events.listen(GamePlayerEvents.ADD_TO_LEVEL, (player, level) ->
				GameClientState.sendToPlayer(newState.createClientState(level.dimension()), player)
		);
		events.listen(GamePlayerEvents.REMOVE, player -> GameClientState.removeFromPlayer(GameClientStateTypes.COLLIDERS.get(), player));
		game.state().register(KEY, newState);
		return newState;
	}

	public void addColliders(List<Region> regions) {
		if (colliders.addAll(regions)) {
			rebakeAndSynchronize();
		}
	}

	public void removeColliders(List<Region> regions) {
		boolean changed = false;
		for (Region region : regions) {
			changed |= colliders.remove(region);
		}
		if (changed) {
			rebakeAndSynchronize();
		}
	}

	private void rebakeAndSynchronize() {
		Map<ServerLevel, List<Region>> regionsByLevel = colliders.stream()
				.collect(Collectors.groupingBy(Region::level));

		bakedColliders.clear();
		for (Map.Entry<ServerLevel, List<Region>> entry : regionsByLevel.entrySet()) {
			ServerLevel level = entry.getKey();
			List<BlockBox> boxes = entry.getValue().stream().map(Region::box).toList();
			ColliderSet colliderSet = new ColliderSet(boxes);
			bakedColliders.put(level.dimension(), colliderSet);

			GameClientState.sendToPlayers(new CollidersClientState(colliderSet), game.allPlayers().inLevel(level));
		}
	}

	private CollidersClientState createClientState(ResourceKey<Level> dimension) {
		return new CollidersClientState(getColliders(dimension));
	}

	public ColliderSet getColliders(ResourceKey<Level> dimension) {
		return bakedColliders.getOrDefault(dimension, ColliderSet.EMPTY);
	}
}
