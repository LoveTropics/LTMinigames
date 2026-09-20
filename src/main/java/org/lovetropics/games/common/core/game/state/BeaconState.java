package org.lovetropics.games.common.core.game.state;

import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.instance.BeaconClientState;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Set;

public class BeaconState implements IGameState {
	public static final GameStateKey.Defaulted<BeaconState> KEY = GameStateKey.create("Beacons", BeaconState::new);

	private final Set<BlockPos> positions = new ObjectOpenHashSet<>();

	public void add(BlockPos pos) {
		positions.add(pos);
	}

	public boolean remove(BlockPos pos) {
		return positions.remove(pos);
	}

	public void sendTo(PlayerSet players) {
		BeaconClientState state = new BeaconClientState(List.copyOf(positions));
		GameClientState.sendToPlayers(state, players);
	}
}
