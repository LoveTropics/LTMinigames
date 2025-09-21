package com.lovetropics.minigames.common.content.survive_the_tide.behavior;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record SpectatorEffectsBehavior(float radius, float power, float threshold) implements IGameBehavior {
	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePhaseEvents.TICK, () -> {
			EffectGrid grid = new EffectGrid(radius, Mth.floor(power * threshold));
			game.spectators().forEach(grid::addPlayer);

			grid.candidateCells().forEach(cell -> {
				List<ServerPlayer> players = cell.players;
			});
		});
	}

	private static class EffectGrid {
		private final float cellSize;
		private final int candidateThreshold;

		private final Long2ObjectMap<Cell> cells = new Long2ObjectOpenHashMap<>();
		private final LongSet candidateCells = new LongArraySet();

		private EffectGrid(float cellSize, int candidateThreshold) {
			this.cellSize = cellSize;
			this.candidateThreshold = candidateThreshold;
		}

		public void addPlayer(ServerPlayer player) {
			int cellX = Mth.floor(player.getX() / cellSize);
			int cellY = Mth.floor(player.getY() / cellSize);
			int cellZ = Mth.floor(player.getZ() / cellSize);
			for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
				for (int offsetY = -1; offsetY <= 1; offsetY++) {
					for (int offsetX = -1; offsetX <= 1; offsetX++) {
						addPlayerToCell(player, cellX + offsetX, cellY + offsetY, cellZ + offsetZ);
					}
				}
			}
		}

		private void addPlayerToCell(ServerPlayer player, int cellX, int cellY, int cellZ) {
			long cellPos = SectionPos.asLong(cellX, cellY, cellZ);
			int count = cells.computeIfAbsent(cellPos, k -> new Cell()).addPlayer(player);
			if (count >= candidateThreshold) {
				candidateCells.add(cellPos);
			}
		}

		public Stream<Cell> candidateCells() {
			return candidateCells.longStream()
					.mapToObj(cells::get)
					.filter(Objects::nonNull);
		}

		private static class Cell {
			private final List<ServerPlayer> players = new ArrayList<>();

			public int addPlayer(ServerPlayer player) {
				players.add(player);
				return players.size();
			}
		}
	}
}
