package org.lovetropics.games.common.content.escape_race.client;

import org.lovetropics.games.common.content.escape_race.EscapeRace;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record EscapeRaceClientBucksState(int amount) implements GameClientState {
	public static final MapCodec<EscapeRaceClientBucksState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("currency").forGetter(c -> c.amount)
	).apply(i, EscapeRaceClientBucksState::new));

	@Override
	public GameClientStateType<?> getType() {
		return EscapeRace.BREAK_BUCK_STATE.get();
	}
}
