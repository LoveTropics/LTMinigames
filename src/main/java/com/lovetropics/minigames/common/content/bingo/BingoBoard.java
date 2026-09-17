package com.lovetropics.minigames.common.content.bingo;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.BingoBoardClientState;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/// The bingo board of a game, shared by all of its phases.
///
/// The board belongs to the phase that declares it, which keeps the players' points. Players may be playing in sub-phases
/// of that phase (e.g. each in their own world), so the trigger of every tile is registered in each of those too.
public final class BingoBoard implements IGameState {
	public static final GameStateKey<BingoBoard> KEY = GameStateKey.create("Bingo Board");

	private final IGamePhase game;
	private final int rows, columns;
	private final List<Float> positionRewardMultiplier;
	private final GameActionList onTileCompleted, onBingo;

	private final List<Optional<BingoTile>> tiles;
	private final IntList emptySlots = new IntArrayList();
	private final List<PhaseEvents> phases = new ArrayList<>();

	public BingoBoard(IGamePhase game, int rows, int columns, List<Float> positionRewardMultiplier, GameActionList onTileCompleted, GameActionList onBingo) {
		this.game = game;
		this.rows = rows;
		this.columns = columns;
		this.positionRewardMultiplier = positionRewardMultiplier;
		this.onTileCompleted = onTileCompleted;
		this.onBingo = onBingo;
		tiles = NonNullList.withSize(rows * columns, Optional.empty());

		for (int i = 0; i < rows * columns; i++) {
			emptySlots.add(i);
		}
	}

	/// Registers the triggers of all tiles, current and future, in the given phase
	public void addPhase(IGamePhase phase, EventRegistrar events) {
		PhaseEvents phaseEvents = new PhaseEvents(phase, events);
		phases.add(phaseEvents);
		for (int index = 0; index < tiles.size(); index++) {
			int tileIndex = index;
			tiles.get(index).ifPresent(tile -> registerTrigger(tileIndex, tile, phaseEvents));
		}
	}

	public void removePhase(IGamePhase phase) {
		phases.removeIf(phaseEvents -> phaseEvents.phase == phase);
	}

	/// @return the new tile index, or `-1` if the board is full
	public int addTile(ItemStack icon, Component title, int reward, Supplier<GameActionList> trigger) {
		if (emptySlots.isEmpty()) {
			return -1;
		}
		int index = emptySlots.removeInt(game.random().nextInt(emptySlots.size()));
		BingoTile tile = new BingoTile(icon, title, reward, trigger);
		tiles.set(index, Optional.of(tile));
		for (PhaseEvents phaseEvents : phases) {
			registerTrigger(index, tile, phaseEvents);
		}
		updateTiles();
		return index;
	}

	private static void registerTrigger(int tileIndex, BingoTile tile, PhaseEvents phaseEvents) {
		// This event capturing dance allows us to pass a parameter to the actions in the trigger so that those actions can report back when they complete the tile
		GameEventListeners captureListeners = new GameEventListeners();

		// Create new instances of the trigger and register its events
		tile.trigger.get().register(phaseEvents.phase, phaseEvents.events.redirect(t -> t == Bingo.CAPTURE_TILE_EVENT, captureListeners));
		captureListeners.invoker(Bingo.CAPTURE_TILE_EVENT).capture(tileIndex);
	}

	public void complete(int tileIndex, ServerPlayer player) {
		Optional<BingoTile> tile = tileIndex >= 0 && tileIndex < tiles.size() ? tiles.get(tileIndex) : Optional.empty();
		if (tile.isEmpty() || !tile.get().completedBy.add(player.getUUID())) {
			return;
		}
		BingoTile completedTile = tile.get();

		updatePlayer(player);
		onTileCompleted.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
		game.allPlayers(true).sendMessage(Bingo.TILE_COMPLETED.apply(player.getDisplayName(), completedTile.title.copy().withStyle(ChatFormatting.AQUA)));

		int position = completedTile.completedBy.size() - 1;
		float multiplier = positionRewardMultiplier.get(Math.min(position, positionRewardMultiplier.size() - 1));
		game.statistics().forPlayer(player).incrementInt(StatisticKey.POINTS, (int) Math.floor(completedTile.reward * multiplier));

		if (hasBingo(player.getUUID())) {
			onBingo.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
		}
	}

	private boolean hasBingo(UUID player) {
		for (int row = 0; row < rows; row++) {
			boolean complete = true;
			for (int column = 0; column < columns && complete; column++) {
				complete = isCompletedBy(row, column, player);
			}
			if (complete) {
				return true;
			}
		}

		for (int column = 0; column < columns; column++) {
			boolean complete = true;
			for (int row = 0; row < rows && complete; row++) {
				complete = isCompletedBy(row, column, player);
			}
			if (complete) {
				return true;
			}
		}

		int diagonalSize = Math.min(rows, columns);
		boolean primaryDiagonal = true;
		boolean secondaryDiagonal = true;
		for (int i = 0; i < diagonalSize; i++) {
			primaryDiagonal &= isCompletedBy(i, i, player);
			secondaryDiagonal &= isCompletedBy(rows - i - 1, i, player);
		}
		return primaryDiagonal || secondaryDiagonal;
	}

	private boolean isCompletedBy(int row, int column, UUID player) {
		return tiles.get(row * columns + column)
				.map(tile -> tile.completedBy.contains(player))
				.orElse(false);
	}

	public void updateTiles() {
		for (ServerPlayer player : game.allPlayers(true)) {
			updatePlayer(player);
		}
	}

	/// Sends the board to the given player, if they are playing - no matter which phase of the game they are in
	public void updatePlayer(ServerPlayer player) {
		if (game.getRoleFor(player) != PlayerRole.PARTICIPANT) {
			return;
		}
		GameClientState.sendToPlayer(new BingoBoardClientState(rows, columns, tiles.stream()
				.map(o -> o.map(t -> t.tile(player)))
				.toList()), player);
	}

	public void removeFromPlayer(ServerPlayer player) {
		GameClientState.removeFromPlayer(GameClientStateTypes.BINGO_BOARD.get(), player);
	}

	private record PhaseEvents(IGamePhase phase, EventRegistrar events) {
	}

	private static final class BingoTile {
		private final ItemStack icon;
		private final Component title;
		private final int reward;
		private final Supplier<GameActionList> trigger;
		private final Set<UUID> completedBy = new HashSet<>();

		private BingoTile(ItemStack icon, Component title, int reward, Supplier<GameActionList> trigger) {
			this.icon = icon;
			this.title = title;
			this.reward = reward;
			this.trigger = trigger;
		}

		private BingoBoardClientState.Tile tile(ServerPlayer player) {
			return new BingoBoardClientState.Tile(icon, title, completedBy.contains(player.getUUID()));
		}
	}
}
