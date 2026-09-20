package org.lovetropics.games.common.content.biodiversity_blitz.client_state;

import com.lovetropics.lib.BlockBox;
import org.lovetropics.games.common.content.biodiversity_blitz.BiodiversityBlitz;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record ClientBbMobSpawnState(List<BlockBox> spawns) implements GameClientState {
	public static final MapCodec<ClientBbMobSpawnState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockBox.CODEC.listOf().fieldOf("spawns").forGetter(ClientBbMobSpawnState::spawns)
	).apply(i, ClientBbMobSpawnState::new));

	@Override
	public GameClientStateType<?> getType() {
		return BiodiversityBlitz.MOB_SPAWN.get();
	}
}
