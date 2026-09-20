package org.lovetropics.games.common.core.game.client_state.instance;

import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.List;
import java.util.UUID;

public record SpectatingClientState(List<UUID> players) implements GameClientState {
	public static final MapCodec<SpectatingClientState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			UUIDUtil.STRING_CODEC.listOf().fieldOf("players").forGetter(c -> c.players)
	).apply(i, SpectatingClientState::new));

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.SPECTATING.get();
	}
}
