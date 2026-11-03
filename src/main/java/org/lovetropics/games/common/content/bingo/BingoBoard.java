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
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameEventListeners;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.BingoBoardClientState;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.Ghosts;
import org.lovetropics.games.common.core.game.state.IGameState;
import org.lovetropics.games.common.core.game.state.statistics.GameStatistics;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.integration.GameInstanceIntegrations;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/// The bingo board of a game, shared by all of its phases.
///
/// The board belongs to the phase that declares it, which keeps the players' points. Players may be playing in sub-phases
/// of that phase (e.g. each in their own world), so the trigger of every tile is registered in each of those too.
///
/// Tiles can be added directly, or unlocked from a pool of locked tiles. Which tile of the pool ends up in which slot is
/// decided upfront, so that players can be given a clue about a locked tile before it gets unlocked. They are unlocked
/// in a random order, and the tiles of the pool that don't fit on the board are never used.
public final class BingoBoard implements IGameState {
	public static final GameStateKey<BingoBoard> KEY = GameStateKey.create("Bingo Board");

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
	private final List<PhaseEvents> phases = new ArrayList<>();

	/// @param tilePool      locked tiles that can be unlocked onto the board
	/// @param unlockOnClear how many tiles to unlock once a player has completed every tile on the board
	public BingoBoard(IGamePhase game, int rows, int columns, List<Float> positionRewardMultiplier, GameActionList onTileCompleted, GameActionList onBingo, List<BingoTileDefinition> tilePool, int unlockOnClear) {
		this.game = game;
		this.rows = rows;
		this.columns = columns;
		this.positionRewardMultiplier = positionRewardMultiplier;
		this.onTileCompleted = onTileCompleted;
		this.onBingo = onBingo;
		this.unlockOnClear = unlockOnClear;
		tiles = NonNullList.withSize(rows * columns, Optional.empty());

		List<BingoTileDefinition> pool = new ArrayList<>(tilePool);
		//Util.shuffle(pool, game.random());
		IntList slots = new IntArrayList();
		for (int i = 0; i < rows * columns; i++) {
			slots.add(i);
		}
		//Util.shuffle(slots, game.random());

		for (int i = 0; i < slots.size(); i++) {
			if (i < pool.size()) {
				lockedSlots.put(slots.getInt(i), pool.get(i));
				placeTile(slots.getInt(i), pool.get(i));
			} else {
				emptySlots.add(slots.getInt(i));
			}
		}
	}

	/// Registers the triggers of all tiles, current and future, in the given phase
	public void addPhase(IGamePhase phase, EventRegistrar events) {
		PhaseEvents phaseEvents = new PhaseEvents(phase, events);
		phases.add(phaseEvents);
		for (Optional<BingoTile> tile : tiles) {
			tile.ifPresent(t -> registerTrigger(t, phaseEvents));
		}
	}

	public void removePhase(IGamePhase phase) {
		phases.removeIf(phaseEvents -> phaseEvents.phase == phase);
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
		BingoTile tile = new BingoTile(placedTiles.size(), index, definition.icon().create(), definition.title(), definition.reward(), definition.trigger(), definition.tileType());
		placedTiles.add(tile);
		tiles.set(index, Optional.of(tile));
		for (PhaseEvents phaseEvents : phases) {
			registerTrigger(tile, phaseEvents);
		}
	}

	private static void registerTrigger(BingoTile tile, PhaseEvents phaseEvents) {
		// This event capturing dance allows us to pass a parameter to the actions in the trigger so that those actions can report back when they complete the tile
		GameEventListeners captureListeners = new GameEventListeners();

		// Create new instances of the trigger and register its events
		tile.trigger.get().register(phaseEvents.phase, phaseEvents.events.redirect(t -> t == Bingo.CAPTURE_TILE_EVENT, captureListeners));
		captureListeners.invoker(Bingo.CAPTURE_TILE_EVENT).capture(tile.id);
	}

	public void complete(int tileId, ServerPlayer player) {
		if (tileId < 0 || tileId >= placedTiles.size()) {
			return;
		}
		BingoTile completedTile = placedTiles.get(tileId);
		if (tiles.get(completedTile.slot).orElse(null) != completedTile || !completedTile.completedBy.add(player.getUUID())) {
			return;
		}

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

		game.statistics().forPlayer(player).incrementInt(StatisticKey.POINTS, getReward(completedTile, completedTile.completedBy.size() - 1));
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

	/// @param position how many players completed the tile before
	private int getReward(BingoTile tile, int position) {
		float multiplier = positionRewardMultiplier.get(Math.min(position, positionRewardMultiplier.size() - 1));
		return (int) Math.floor(tile.reward * multiplier);
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
				.map(o -> o.map(t -> t.tile(player, getReward(t, t.completedBy.size()))))
				.toList();
		List<BingoBoardClientState.LockedSlot> clientLockedSlots = lockedSlots.int2ObjectEntrySet().stream()
				.filter(entry -> clientTiles.get(entry.getIntKey()).isEmpty())
				.map(entry -> new BingoBoardClientState.LockedSlot(entry.getIntKey(), entry.getValue().clue()))
				.toList();
		int points = game.statistics().forPlayer(player).getOr(StatisticKey.POINTS, 0);
		Optional<Component> unlockHint = !lockedSlots.isEmpty() && unlockOnClear > 0 ? Optional.of(Bingo.LOCKED_HINT_CLEAR.apply(unlockOnClear)) : Optional.empty();
		GameClientState.sendToPlayer(new BingoBoardClientState(rows, columns, clientTiles, clientLockedSlots, points, unlockHint, getPlayerProgress(), Ghosts.isParticipantOrGhost(game, player)), player);
	}

	/// The progress of everyone playing, and of the players who left but still have points
	private List<BingoBoardClientState.PlayerProgress> getPlayerProgress() {
		GameStatistics statistics = game.statistics();
		Set<PlayerKey> players = new LinkedHashSet<>();
		for (PlayerKey player : statistics.getPlayers()) {
			if (statistics.forPlayer(player).get(StatisticKey.POINTS) != null) {
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
			int points = statistics.forPlayer(player).getOr(StatisticKey.POINTS, 0);
			progress.add(new BingoBoardClientState.PlayerProgress(player.id(), player.name(), points, completedTiles));
		}
		return progress;
	}

	public void removeFromPlayer(ServerPlayer player) {
		GameClientState.removeFromPlayer(GameClientStateTypes.BINGO_BOARD.get(), player);
	}

	private record PhaseEvents(IGamePhase phase, EventRegistrar events) {
	}

	private static final class BingoTile {
		private final int id;
		private final int slot;
		private final ItemStack icon;
		private final Component title;
		private final int reward;
		private final Supplier<GameActionList> trigger;
		private final BingoTileType tileType;
		/// In the order the players completed the tile
		private final Set<UUID> completedBy = new LinkedHashSet<>();

		private BingoTile(int id, int slot, ItemStack icon, Component title, int reward, Supplier<GameActionList> trigger, BingoTileType tileType) {
			this.id = id;
			this.slot = slot;
			this.icon = icon;
			this.title = title;
			this.reward = reward;
			this.trigger = trigger;
			this.tileType = tileType;
		}

		private BingoBoardClientState.Tile tile(ServerPlayer player, int nextReward) {
			return new BingoBoardClientState.Tile(icon, title, completedBy.contains(player.getUUID()), completedBy.size(), nextReward, tileType);
		}
	}
}
