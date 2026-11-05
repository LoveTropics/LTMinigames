package org.lovetropics.games.common.core.game.client_state.instance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;
import org.lovetropics.games.common.content.bingo.BingoTileKind;
import org.lovetropics.games.common.content.bingo.BingoTileType;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/// @param tiles       the tiles of the board, empty for slots with no tile yet
/// @param lockedSlots the empty slots that will get a tile once unlocked
/// @param points      the points of the player the board is sent to
/// @param unlockHint  how to unlock more tiles, if there is anything players can do about it
/// @param players     the progress of every player of the game, including the one the board is sent to
/// @param hasCard     whether the player the board is sent to has a card of their own, otherwise they can only look at the cards of the players
/// @param overlord    whether the player the board is sent to is an overlord
public record BingoBoardClientState(int rows, int columns, List<Optional<Tile>> tiles, List<LockedSlot> lockedSlots, int points, Optional<Component> unlockHint, List<PlayerProgress> players, boolean hasCard, boolean overlord) implements GameClientState {
	public static final MapCodec<BingoBoardClientState> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			Codec.INT.fieldOf("rows").forGetter(BingoBoardClientState::rows),
			Codec.INT.fieldOf("columns").forGetter(BingoBoardClientState::columns),
			Tile.CODEC.optionalFieldOf("tile").codec().listOf().fieldOf("tiles").forGetter(BingoBoardClientState::tiles),
			LockedSlot.CODEC.listOf().optionalFieldOf("locked_slots", List.of()).forGetter(BingoBoardClientState::lockedSlots),
			Codec.INT.optionalFieldOf("points", 0).forGetter(BingoBoardClientState::points),
			ComponentSerialization.CODEC.optionalFieldOf("unlock_hint").forGetter(BingoBoardClientState::unlockHint),
			PlayerProgress.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(BingoBoardClientState::players),
			Codec.BOOL.optionalFieldOf("has_card", true).forGetter(BingoBoardClientState::hasCard),
			Codec.BOOL.optionalFieldOf("overlord", false).forGetter(BingoBoardClientState::overlord)
	).apply(in, BingoBoardClientState::new));

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.BINGO_BOARD.get();
	}

	public Optional<LockedSlot> getLockedSlot(int index) {
		return lockedSlots.stream().filter(slot -> slot.index() == index).findFirst();
	}

	public int unlockedTileCount() {
		return (int) tiles.stream().filter(Optional::isPresent).count();
	}

	/// @param completions how many players have completed the tile
	/// @param reward      how many points the tile earned the player if they completed it, otherwise what it is worth to them if they complete it now
	/// @param tileType    information about how the tile should be displayed
	/// 				   TODO add an Appearance record to BingoTileType so we avoid sending gameplay information.
	/// @param kind        what players have to do to complete the tile
	public record Tile(
			ItemStack icon,
			Component title,
			Optional<Component> description,
			boolean completed,
			int completions,
			int reward,
			BingoTileType tileType,
			BingoTileKind kind
	) {
		public static final Codec<Tile> CODEC = RecordCodecBuilder.create(in -> in.group(
				ItemStack.CODEC.fieldOf("icon").forGetter(Tile::icon),
				ComponentSerialization.CODEC.fieldOf("title").forGetter(Tile::title),
				ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(Tile::description),
				Codec.BOOL.fieldOf("completed").forGetter(Tile::completed),
				Codec.INT.optionalFieldOf("completions", 0).forGetter(Tile::completions),
				Codec.INT.optionalFieldOf("reward", 0).forGetter(Tile::reward),
				BingoTileType.CODEC.optionalFieldOf("tile_type", BingoTileType.DEFAULT).forGetter(Tile::tileType),
				BingoTileKind.CODEC.optionalFieldOf("kind", BingoTileKind.OBTAIN).forGetter(Tile::kind)
		).apply(in, Tile::new));
	}

	/// @param clue a teaser of the tile that will be unlocked into this slot, if it has one
	public record LockedSlot(int index, Optional<Component> clue) {
		public static final Codec<LockedSlot> CODEC = RecordCodecBuilder.create(in -> in.group(
				Codec.INT.fieldOf("index").forGetter(LockedSlot::index),
				ComponentSerialization.CODEC.optionalFieldOf("clue").forGetter(LockedSlot::clue)
		).apply(in, LockedSlot::new));
	}

	/// @param completedTiles the indices of the tiles the player has completed
	public record PlayerProgress(UUID id, String name, int points, List<Integer> completedTiles) {
		public static final Codec<PlayerProgress> CODEC = RecordCodecBuilder.create(in -> in.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(PlayerProgress::id),
				Codec.STRING.fieldOf("name").forGetter(PlayerProgress::name),
				Codec.INT.fieldOf("points").forGetter(PlayerProgress::points),
				Codec.INT.listOf().fieldOf("completed_tiles").forGetter(PlayerProgress::completedTiles)
		).apply(in, PlayerProgress::new));

		public boolean hasCompleted(int index) {
			return completedTiles.contains(index);
		}
	}
}
