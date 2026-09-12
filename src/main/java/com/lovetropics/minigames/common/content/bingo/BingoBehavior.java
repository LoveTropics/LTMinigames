package com.lovetropics.minigames.common.content.bingo;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.BingoBoardClientState;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class BingoBehavior implements IGameBehavior {
	public static final MapCodec<BingoBehavior> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			Codec.INT.fieldOf("rows").forGetter(b -> b.rows),
			Codec.INT.fieldOf("columns").forGetter(b -> b.columns),
			Codec.FLOAT.listOf(1, Integer.MAX_VALUE).fieldOf("position_reward_multipler").forGetter(b -> b.positionRewardMultiplier),
			GameActionList.CODEC.optionalFieldOf("on_tile_completed", GameActionList.EMPTY).forGetter(b -> b.onTileCompleted),
			GameActionList.CODEC.optionalFieldOf("on_bingo", GameActionList.EMPTY).forGetter(b -> b.onBingo)
	).apply(in, BingoBehavior::new));

	private final int rows, columns;
	private final List<Float> positionRewardMultiplier;
	private final GameActionList onTileCompleted, onBingo;
	private final List<Optional<BingoTile>> tiles;
	private final IntList emptySlots = new IntArrayList();

	private IGamePhase game;

	public BingoBehavior(int rows, int columns, List<Float> positionRewardMultiplier, GameActionList onTileCompleted, GameActionList onBingo) {
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

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		this.game = game;
		onTileCompleted.register(game, events);
		onBingo.register(game, events);

		events.listen(GamePhaseEvents.START, _ -> updateTiles());

		events.listen(GamePlayerEvents.REMOVE, player -> GameClientState.removeFromPlayer(GameClientStateTypes.BINGO_BOARD.get(), player));

		events.listen(Bingo.REQUEST_NEW_TILE_EVENT, (icon, title, reward) -> addTile(new BingoTile(icon, title, reward)));
		events.listen(Bingo.COMPLETE_BINGO_TILE_EVENT, (tile, completingPlayer) -> {
			tiles.get(tile).ifPresent(t -> {
				if (t.completedBy.add(completingPlayer.getUUID())) {
					updatePlayer(completingPlayer);
					onTileCompleted.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(completingPlayer));
					game.allPlayers().sendMessage(Bingo.TILE_COMPLETED.apply(completingPlayer.getDisplayName(), t.title.copy().withStyle(ChatFormatting.AQUA)));

					int position = t.completedBy.size() - 1;
					float multiplier = position >= positionRewardMultiplier.size() ? positionRewardMultiplier.getLast() : positionRewardMultiplier.get(position);
					game.statistics().forPlayer(completingPlayer).incrementInt(StatisticKey.POINTS, (int) Math.floor(t.reward * multiplier));

					if (checkBingo(completingPlayer)) {
						onBingo.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(completingPlayer));
					}
				}
			});
		});
	}

	private boolean checkBingo(ServerPlayer player) {
		rowCheck:
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				if (tiles.get(i * rows + j).isEmpty() || !tiles.get(i * rows + j).get().completedBy.contains(player.getUUID())) {
					continue rowCheck;
				}
			}
			return true;
		}

		columnCheck:
		for (int i = 0; i < columns; i++) {
			for (int j = 0; j < rows; j++) {
				if (tiles.get(j * rows + i).isEmpty() || !tiles.get(j * rows + i).get().completedBy.contains(player.getUUID())) {
					continue columnCheck;
				}
			}
			return true;
		}

		int diagonalSize = Math.min(rows, columns);

		// Primary diagonal check
		boolean primaryDiagonal = true;
		for (int i = 0; i < diagonalSize; i++) {
			if (tiles.get(i * rows + i).isEmpty() || !tiles.get(i * rows + i).get().completedBy.contains(player.getUUID())) {
				primaryDiagonal = false;
				break;
			}
		}
		if (primaryDiagonal) return true;

		// Secondary diagonal check
		boolean secondaryDiagonal = true;
		for (int i = diagonalSize - 1; i >= 0; i--) {
			int row = rows - i - 1;
			if (tiles.get(row * rows + i).isEmpty() || !tiles.get(row * rows + i).get().completedBy.contains(player.getUUID())) {
				secondaryDiagonal = false;
				break;
			}
		}
		return secondaryDiagonal;
	}

	private int addTile(BingoTile tile) {
		if (emptySlots.isEmpty()) return -1;
		var index = emptySlots.removeInt((int) Math.floor(Math.random() * emptySlots.size()));
		tiles.set(index, Optional.of(tile));
		updateTiles();
		return index;
	}

	private void updateTiles() {
		for (ServerPlayer participant : game.participants()) {
			updatePlayer(participant);
		}
	}

	private void updatePlayer(ServerPlayer player) {
		GameClientState.sendToPlayer(new BingoBoardClientState(rows, columns, tiles.stream()
				.map(o -> o.map(t -> t.tile(player)))
				.toList()), player);
	}

	public static final class BingoTile {
		private final ItemStack icon;
		private final Component title;
		private final int reward;
		private final Set<UUID> completedBy = new HashSet<>();

		public BingoTile(ItemStack icon, Component title, int reward) {
			this.icon = icon;
			this.title = title;
			this.reward = reward;
		}

		public BingoBoardClientState.Tile tile(ServerPlayer player) {
			return new BingoBoardClientState.Tile(icon, title, completedBy.contains(player.getUUID()));
		}
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return Bingo.BINGO;
	}
}
