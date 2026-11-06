package org.lovetropics.games.common.content.bingo;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import org.lovetropics.games.common.core.advancement.CriteriaTracker;
import org.lovetropics.games.common.core.game.IGameLookup;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.BingoBoardClientState;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.state.Ghosts;
import org.lovetropics.games.common.core.game.state.IGameState;
import org.lovetropics.games.common.core.game.state.Overlords;
import org.lovetropics.games.common.core.game.state.statistics.GameStatistics;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.integration.GameInstanceIntegrations;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/// The bingo board of a game, shared by all of its phases.
///
/// The board belongs to the phase that declares it, which keeps the players' points. Players may be playing in sub-phases
/// of that phase (e.g. each in their own world), so tiles can be completed from any phase of the game instance.
public final class BingoBoard implements IGameState {
	public static final GameStateKey<BingoBoard> KEY = GameStateKey.create("Bingo Board");
	private static final int SHOWN_PLACES = 6;

	private final IGamePhase game;
	private final int rows, columns;
	private final List<Float> positionRewardMultiplier;
	private final GameActionList onTileCompleted, onBingo;
	private final int unlockOnClear;

	private final List<Optional<BingoTile>> tiles;
	/// Slots with a tile waiting to be unlocked into them
	private final Int2ObjectMap<BingoTileDefinition> lockedSlots = new Int2ObjectOpenHashMap<>();
	/// Slots with nothing in them, not even a locked tile
	private final IntList emptySlots = new IntArrayList();
	private final List<BingoTile> placedTiles = new ArrayList<>();
	private boolean started;

	/// @param lockedTiles   locked tiles in their slot on the board, which can then be unlocked
	/// @param unlockOnClear how many tiles to unlock once a player has completed every tile on the board
	public BingoBoard(IGamePhase game, int rows, int columns, List<Float> positionRewardMultiplier, GameActionList onTileCompleted, GameActionList onBingo, Map<TilePosition, BingoTileDefinition> lockedTiles, int unlockOnClear) {
		this.game = game;
		this.rows = rows;
		this.columns = columns;
		this.positionRewardMultiplier = positionRewardMultiplier;
		this.onTileCompleted = onTileCompleted;
		this.onBingo = onBingo;
		this.unlockOnClear = unlockOnClear;
		tiles = NonNullList.withSize(rows * columns, Optional.empty());

		lockedTiles.forEach((position, definition) -> {
			int slot = position.slot(columns);
			lockedSlots.put(slot, definition);
			placeTile(slot, definition);
		});
		for (int slot = 0; slot < rows * columns; slot++) {
			if (!lockedSlots.containsKey(slot)) {
				emptySlots.add(slot);
			}
		}
	}

	/// Starts tracking the criteria of all tiles, current and future
	public void start() {
		if (!started) {
			started = true;
			CriteriaTracker.startAll(game.server(), tiles.stream().flatMap(Optional::stream).map(t -> t.tracker).toList());
		}
	}

	public void stop() {
		if (started) {
			started = false;
			for (BingoTile tile : placedTiles) {
				tile.tracker.stop(game.server());
			}
		}
	}

	/// Adds a tile into an empty slot, or in place of a locked tile if there is none left
	///
	/// @return the new tile index, or `-1` if the board is full
	public int addTile(BingoTileDefinition definition) {
		int index;
		if (!emptySlots.isEmpty()) {
			index = emptySlots.removeInt(game.random().nextInt(emptySlots.size()));
		} else if (!lockedSlots.isEmpty()) {
			index = randomLockedSlot();
			lockedSlots.remove(index);
		} else {
			return -1;
		}
		placeTile(index, definition);
		updateTiles();
		return index;
	}

	/// Unlocks random locked tiles
	///
	/// @return how many tiles were unlocked
	public int unlockTiles(int count) {
		List<Component> unlockedTitles = new ArrayList<>();
		while (unlockedTitles.size() < count && !lockedSlots.isEmpty()) {
			int index = randomLockedSlot();
			BingoTileDefinition definition = lockedSlots.remove(index);
			unlockedTitles.add(definition.title());
		}
		if (!unlockedTitles.isEmpty()) {
			updateTiles();
			Component titles = ComponentUtils.formatList(unlockedTitles, Component.literal(", ")).copy().withStyle(ChatFormatting.AQUA);
			game.allPlayers().sendMessage(Bingo.TILES_UNLOCKED.apply(titles).withStyle(ChatFormatting.GOLD));
			game.allPlayers().playSound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.5f);
		}
		return unlockedTitles.size();
	}

	private int randomLockedSlot() {
		IntList slots = new IntArrayList(lockedSlots.keySet());
		return slots.getInt(game.random().nextInt(slots.size()));
	}

	private void placeTile(int index, BingoTileDefinition definition) {
		int id = placedTiles.size();
		CriteriaTracker tracker = new CriteriaTracker(definition.criteria(), definition.requirements(), this::canComplete, player -> complete(id, player));
		BingoTile tile = new BingoTile(id, index, definition.icon().create(), definition.title(), definition.description(), definition.reward(), tracker, definition.tileType(), definition.kind(), definition.columnMultipliers());
		placedTiles.add(tile);
		tiles.set(index, Optional.of(tile)).ifPresent(replaced -> replaced.tracker.stop(game.server()));
		if (started) {
			tracker.start(game.server());
		}
	}

	private boolean canComplete(ServerPlayer player) {
		IGamePhase phase = IGameLookup.get().getGamePhaseFor(player);
		return phase != null && phase.instanceState().getOrNull(KEY) == this && Ghosts.isParticipantOrGhost(phase, player);
	}

	public void complete(int tileId, ServerPlayer player) {
		if (tileId < 0 || tileId >= placedTiles.size()) {
			return;
		}
		BingoTile completedTile = placedTiles.get(tileId);
		if (tiles.get(completedTile.slot).orElse(null) != completedTile || completedTile.completedBy.contains(player.getUUID())) {
			return;
		}
		// completing a column multiplier also changes what the tiles completed before are worth :)
		int pointsBefore = getTotalPoints(player.getUUID());
		completedTile.completedBy.add(player.getUUID());

		onTileCompleted.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
		MutableComponent tileCompletedComponent = Bingo.TILE_COMPLETED.apply(player.getDisplayName(), completedTile.title.copy().withStyle(ChatFormatting.AQUA));
		game.allPlayers().sendMessage(tileCompletedComponent);

		// Send the tile completion event to the techstack
		GameInstanceIntegrations integrations = game.instanceState().getOrNull(GameInstanceIntegrations.KEY);
		if (integrations != null) {
			String sb = "<:pogfish:1038094258713935912> " +
					player.getDisplayName().getString() +
					" (" + completedTile.completedBy.size() + "/" + game.participants().size() + ")" +
					" completed: " +
					completedTile.title.getString();
			integrations.sendInGameEvent(sb);
		}

		game.statistics().forPlayer(player).incrementInt(StatisticKey.POINTS, getTotalPoints(player.getUUID()) - pointsBefore);
		// Everyone sees how many players completed each tile, and what it is still worth to them
		updateTiles();

		if (hasBingo(player.getUUID())) {
			onBingo.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
		}

		if (hasCompletedAllTiles(player.getUUID(), true)) {
			game.invoker(Bingo.BOARD_COMPLETED).onBoardCompleted(player);
		} else if (unlockOnClear > 0 && hasCompletedAllTiles(player.getUUID(), false)) {
			unlockTiles(unlockOnClear);
		}
	}

	private int getTotalPoints(UUID player) {
		int points = 0;
		for (Optional<BingoTile> optionalTile : tiles) {
			BingoTile tile = optionalTile.orElse(null);
			if (tile != null && tile.completedBy.contains(player)) {
				points += getReward(tile, getPosition(tile, player), getColumnMultiplier(tile, player, true));
			}
		}
		return points;
	}

	private static int getPosition(BingoTile tile, UUID player) {
		int position = 0;
		for (UUID completedBy : tile.completedBy) {
			if (completedBy.equals(player)) {
				return position;
			}
			position++;
		}
		return position;
	}

	private float getColumnMultiplier(BingoTile tile, UUID player, boolean completedOnly) {
		float multiplier = 1.0f;
		int column = tile.slot % columns;
		for (int row = 0; row < rows; row++) {
			BingoTile other = tiles.get(row * columns + column).orElse(null);
			if (other == null || other == tile || other.columnMultipliers.isEmpty()) {
				continue;
			}
			if (!other.completedBy.contains(player) && (completedOnly || lockedSlots.containsKey(other.slot))) {
				continue;
			}
			multiplier *= other.getColumnMultiplier(getPosition(other, player));
		}
		return multiplier;
	}

	private List<Integer> getPlaceRewards(BingoTile tile, UUID player) {
		float columnMultiplier = getColumnMultiplier(tile, player, true);
		int places = Math.max(SHOWN_PLACES, tile.completedBy.size());
		List<Integer> rewards = new ArrayList<>(places);
		for (int position = 0; position < places; position++) {
			rewards.add(getReward(tile, position, columnMultiplier));
		}
		return rewards;
	}

	/// @param position how many players completed the tile before
	private int getReward(BingoTile tile, int position, float columnMultiplier) {
		float multiplier = positionRewardMultiplier.get(Math.min(position, positionRewardMultiplier.size() - 1));
		return (int) Math.floor(tile.reward * multiplier * columnMultiplier);
	}

	private BingoBoardClientState.Tile createClientTile(BingoTile tile, UUID player) {
		int position = getPosition(tile, player);
		return new BingoBoardClientState.Tile(
				tile.icon, tile.title, tile.description,
				tile.completedBy.contains(player), tile.completedBy.size(),
				getReward(tile, position, 1.0f),
				tile.tileType, tile.kind,
				getPlaceRewards(tile, player), List.copyOf(tile.completedBy),
				getReward(tile, position, getColumnMultiplier(tile, player, false)),
				tile.columnMultipliers
		);
	}

	/// @param includeLocked whether the locked tiles have to be completed too
	private boolean hasCompletedAllTiles(UUID player, boolean includeLocked) {
		for (int index = 0; index < tiles.size(); index++) {
			if ((includeLocked || !lockedSlots.containsKey(index)) && tiles.get(index).map(t -> !t.completedBy.contains(player)).orElse(false)) {
				return false;
			}
		}
		return true;
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
		for (ServerPlayer player : game.allPlayers()) {
			updatePlayer(player);
		}
	}

	/// Sends the board to the given player - no matter which phase of the game they are in.
	/// Only those playing or who are ghosts have a card of their own
	public void updatePlayer(ServerPlayer player) {
		List<Optional<BingoBoardClientState.Tile>> clientTiles = tiles.stream()
				.map(o -> o.filter(t -> !lockedSlots.containsKey(t.slot) || t.completedBy.contains(player.getUUID())))
				.map(o -> o.map(t -> createClientTile(t, player.getUUID())))
				.toList();
		List<BingoBoardClientState.LockedSlot> clientLockedSlots = lockedSlots.int2ObjectEntrySet().stream()
				.filter(entry -> clientTiles.get(entry.getIntKey()).isEmpty())
				.map(entry -> new BingoBoardClientState.LockedSlot(entry.getIntKey(), entry.getValue().clue()))
				.toList();
		int points = game.statistics().readForPlayer(player).getOr(StatisticKey.POINTS, 0);
		Optional<Component> unlockHint = !lockedSlots.isEmpty() && unlockOnClear > 0 ? Optional.of(Bingo.LOCKED_HINT_CLEAR.apply(unlockOnClear)) : Optional.empty();
		boolean overlord = game.getRoleFor(player) == PlayerRole.SPECTATOR && Overlords.get(game).contains(player);
		GameClientState.sendToPlayer(new BingoBoardClientState(rows, columns, clientTiles, clientLockedSlots, points, unlockHint, getPlayerProgress(), Ghosts.isParticipantOrGhost(game, player), overlord), player);
	}

	/// The progress of everyone playing, and of the players who left but still have points
	private List<BingoBoardClientState.PlayerProgress> getPlayerProgress() {
		GameStatistics statistics = game.statistics();
		Set<PlayerKey> players = new LinkedHashSet<>();
		for (PlayerKey player : statistics.getPlayers()) {
			if (statistics.readForPlayer(player).contains(StatisticKey.POINTS)) {
				players.add(player);
			}
		}
		for (ServerPlayer player : game.allPlayers().filter(p -> Ghosts.isParticipantOrGhost(game, p))) {
			players.add(PlayerKey.from(player));
		}

		List<BingoBoardClientState.PlayerProgress> progress = new ArrayList<>(players.size());
		for (PlayerKey player : players) {
			List<Integer> completedTiles = new ArrayList<>();
			for (int index = 0; index < tiles.size(); index++) {
				if (tiles.get(index).map(t -> t.completedBy.contains(player.id())).orElse(false)) {
					completedTiles.add(index);
				}
			}
			int points = statistics.readForPlayer(player).getOr(StatisticKey.POINTS, 0);
			progress.add(new BingoBoardClientState.PlayerProgress(player.id(), player.name(), points, completedTiles));
		}
		return progress;
	}

	public void removeFromPlayer(ServerPlayer player) {
		GameClientState.removeFromPlayer(GameClientStateTypes.BINGO_BOARD.get(), player);
	}

	private static final class BingoTile {
		private final int id;
		private final int slot;
		private final ItemStack icon;
		private final Component title;
		private final Optional<Component> description;
		private final int reward;
		private final CriteriaTracker tracker;
		private final BingoTileType tileType;
		private final BingoTileKind kind;
		private final List<Float> columnMultipliers;
		/// In the order the players completed the tile
		private final Set<UUID> completedBy = new LinkedHashSet<>();

		private BingoTile(int id, int slot, ItemStack icon, Component title, Optional<Component> description, int reward, CriteriaTracker tracker, BingoTileType tileType, BingoTileKind kind, List<Float> columnMultipliers) {
			this.id = id;
			this.slot = slot;
			this.icon = icon;
			this.title = title;
			this.description = description;
			this.reward = reward;
			this.tracker = tracker;
			this.tileType = tileType;
			this.kind = kind;
			this.columnMultipliers = columnMultipliers;
		}

		/// @param position how many players completed the tile before
		private float getColumnMultiplier(int position) {
			return columnMultipliers.get(Math.min(position, columnMultipliers.size() - 1));
		}
	}
}
