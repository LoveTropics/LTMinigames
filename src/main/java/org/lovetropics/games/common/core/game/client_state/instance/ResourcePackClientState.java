package org.lovetropics.games.common.core.game.client_state.instance;

import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ResourcePackClientState(String packName) implements GameClientState {
	public static final MapCodec<ResourcePackClientState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("pack_name").forGetter(c -> c.packName)
	).apply(i, ResourcePackClientState::new));

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.RESOURCE_PACK.get();
	}
}
