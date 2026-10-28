package org.lovetropics.games.common.core.game.state;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.BeaconClientState;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeaconState implements IGameState {
	private static final GameStateKey<BeaconState> KEY = GameStateKey.create("Beacons");

	private final IGamePhase game;
	private final Map<ResourceKey<Level>, Set<BlockPos>> positionsByDimension = new Reference2ObjectOpenHashMap<>();

	private BeaconState(IGamePhase game) {
		this.game = game;
	}

	public static BeaconState getOrAdd(IGamePhase game, EventRegistrar events) {
		BeaconState state = game.state().getOrNull(KEY);
		if (state != null) {
			return state;
		}
		BeaconState newState = new BeaconState(game);
		events.listen(GamePlayerEvents.ADD_TO_LEVEL, (player, level) ->
				GameClientState.sendToPlayer(newState.createClientState(level.dimension()), player)
		);
		events.listen(GamePlayerEvents.REMOVE, player ->
				GameClientState.removeFromPlayer(GameClientStateTypes.BEACON.get(), player)
		);
		game.state().register(KEY, newState);
		return newState;
	}

	public void add(ResourceKey<Level> dimension, BlockPos pos) {
		if (positionsByDimension.computeIfAbsent(dimension, _ -> new HashSet<>()).add(pos)) {
			sendDimensionUpdate(dimension);
		}
	}

	public void remove(ResourceKey<Level> dimension, BlockPos pos) {
		Set<BlockPos> positions = positionsByDimension.get(dimension);
		if (positions != null && positions.remove(pos)) {
			sendDimensionUpdate(dimension);
		}
	}

	private void sendDimensionUpdate(ResourceKey<Level> dimension) {
		BeaconClientState state = createClientState(dimension);
		GameClientState.sendToPlayers(state, game.allPlayers().inDimension(dimension));
	}

	private BeaconClientState createClientState(ResourceKey<Level> dimension) {
		Set<BlockPos> positions = positionsByDimension.get(dimension);
		return new BeaconClientState(positions != null ? List.copyOf(positions) : List.of());
	}
}
