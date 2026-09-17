package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record BingoBoardClientState(int rows, int columns, List<Optional<Tile>> tiles) implements GameClientState {
	public static final MapCodec<BingoBoardClientState> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			Codec.INT.fieldOf("rows").forGetter(BingoBoardClientState::rows),
			Codec.INT.fieldOf("columns").forGetter(BingoBoardClientState::columns),
			Tile.CODEC.optionalFieldOf("tile").codec().listOf().fieldOf("tiles").forGetter(BingoBoardClientState::tiles)
	).apply(in, BingoBoardClientState::new));

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.BINGO_BOARD.get();
	}

	public record Tile(ItemStack icon, Component title, boolean completed) {
		public static final Codec<Tile> CODEC = RecordCodecBuilder.create(in -> in.group(
				ItemStack.CODEC.fieldOf("icon").forGetter(Tile::icon),
				ComponentSerialization.CODEC.fieldOf("title").forGetter(Tile::title),
				Codec.BOOL.fieldOf("completed").forGetter(Tile::completed)
		).apply(in, Tile::new));
	}
}
