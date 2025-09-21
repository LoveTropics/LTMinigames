package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.List;

public record BeaconClientState(List<BlockPos> positions) implements GameClientState {
	public static final MapCodec<BeaconClientState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockPos.CODEC.listOf().fieldOf("positions").forGetter(BeaconClientState::positions)
	).apply(i, BeaconClientState::new));

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.BEACON.get();
	}
}
