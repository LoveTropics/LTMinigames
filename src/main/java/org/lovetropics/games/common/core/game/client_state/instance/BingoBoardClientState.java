package org.lovetropics.games.common.core.game.client_state.instance;

import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/// @param tiles       the tiles of the board, empty for slots with no tile yet
/// @param lockedSlots the empty slots that will get a tile once unlocked
/// @param points      the points of the player the board is sent to
/// @param unlockHint  how to unlock more tiles, if there is anything players can do about it
public record BingoBoardClientState(int rows, int columns, List<Optional<Tile>> tiles, List<LockedSlot> lockedSlots, int points, Optional<Component> unlockHint) implements GameClientState {
	public static final MapCodec<BingoBoardClientState> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			Codec.INT.fieldOf("rows").forGetter(BingoBoardClientState::rows),
			Codec.INT.fieldOf("columns").forGetter(BingoBoardClientState::columns),
			Tile.CODEC.optionalFieldOf("tile").codec().listOf().fieldOf("tiles").forGetter(BingoBoardClientState::tiles),
			LockedSlot.CODEC.listOf().optionalFieldOf("locked_slots", List.of()).forGetter(BingoBoardClientState::lockedSlots),
			Codec.INT.optionalFieldOf("points", 0).forGetter(BingoBoardClientState::points),
			ComponentSerialization.CODEC.optionalFieldOf("unlock_hint").forGetter(BingoBoardClientState::unlockHint)
	).apply(in, BingoBoardClientState::new));

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.BINGO_BOARD.get();
	}

	public Optional<LockedSlot> getLockedSlot(int index) {
		return lockedSlots.stream().filter(slot -> slot.index() == index).findFirst();
	}

	/// @param completions how many players have completed the tile
	/// @param reward      how many points the tile is worth to the player if they complete it now
	public record Tile(ItemStack icon, Component title, boolean completed, int completions, int reward) {
		public static final Codec<Tile> CODEC = RecordCodecBuilder.create(in -> in.group(
				ItemStack.CODEC.fieldOf("icon").forGetter(Tile::icon),
				ComponentSerialization.CODEC.fieldOf("title").forGetter(Tile::title),
				Codec.BOOL.fieldOf("completed").forGetter(Tile::completed),
				Codec.INT.optionalFieldOf("completions", 0).forGetter(Tile::completions),
				Codec.INT.optionalFieldOf("reward", 0).forGetter(Tile::reward)
		).apply(in, Tile::new));
	}

	/// @param clue a teaser of the tile that will be unlocked into this slot, if it has one
	public record LockedSlot(int index, Optional<Component> clue) {
		public static final Codec<LockedSlot> CODEC = RecordCodecBuilder.create(in -> in.group(
				Codec.INT.fieldOf("index").forGetter(LockedSlot::index),
				ComponentSerialization.CODEC.optionalFieldOf("clue").forGetter(LockedSlot::clue)
		).apply(in, LockedSlot::new));
	}
}
