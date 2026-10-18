package org.lovetropics.games.client.game.bingo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.client_state.instance.BingoBoardClientState;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/// The players of the bingo board ordered by points, who slide smoothly into their new place when overtaking each other
public final class BingoStandings {
	private static final Comparator<BingoBoardClientState.PlayerProgress> ORDER = Comparator.comparingInt(BingoBoardClientState.PlayerProgress::points).reversed()
			.thenComparing(BingoBoardClientState.PlayerProgress::name, String.CASE_INSENSITIVE_ORDER);

	/// How much of the way to its new place a player moves every tick
	private static final float SPEED = 0.3f;

	private final boolean includeLocalPlayer;

	private List<BingoBoardClientState.PlayerProgress> players = List.of();
	private final Map<UUID, Position> positions = new HashMap<>();

	/// @param includeLocalPlayer whether this client's own player is part of the standings, or only the other players
	public BingoStandings(boolean includeLocalPlayer) {
		this.includeLocalPlayer = includeLocalPlayer;
	}

	public void tick(@Nullable BingoBoardClientState board) {
		if (board == null) {
			players = List.of();
			positions.clear();
			return;
		}

		LocalPlayer localPlayer = Minecraft.getInstance().player;
		players = board.players().stream()
				.filter(player -> includeLocalPlayer || localPlayer == null || !player.id().equals(localPlayer.getUUID()))
				.sorted(ORDER)
				.toList();
		Set<UUID> ids = new HashSet<>();
		for (int index = 0; index < players.size(); index++) {
			UUID id = players.get(index).id();
			ids.add(id);
			// Players who just appeared start right where they belong
			int target = index;
			positions.computeIfAbsent(id, _ -> new Position(target)).tick(target);
		}
		positions.keySet().retainAll(ids);
	}

	public List<BingoBoardClientState.PlayerProgress> players() {
		return players;
	}

	/// @return where one of [#players()] is drawn in the standings, `0` being first - in between two places while they are moving
	public float position(UUID id, float partialTick) {
		Position position = positions.get(id);
		return position != null ? Mth.lerp(partialTick, position.previous, position.current) : 0.0f;
	}

	private static final class Position {
		private float previous;
		private float current;

		private Position(float initial) {
			previous = initial;
			current = initial;
		}

		private void tick(int target) {
			previous = current;
			current += (target - current) * SPEED;
			if (Math.abs(target - current) < 0.01f) {
				current = target;
			}
		}
	}
}
