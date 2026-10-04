package org.lovetropics.games.common.content.biodiversity_blitz.client_state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lovetropics.games.common.content.biodiversity_blitz.BiodiversityBlitz;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;

public record CurrencyTargetState(int value) implements GameClientState {
	public static final MapCodec<CurrencyTargetState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("value").forGetter(c -> c.value)
	).apply(i, CurrencyTargetState::new));

	@Override
	public GameClientStateType<?> getType() {
		return BiodiversityBlitz.CURRENCY_TARGET.get();
	}
}
