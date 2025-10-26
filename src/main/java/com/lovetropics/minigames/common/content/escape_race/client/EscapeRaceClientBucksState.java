package com.lovetropics.minigames.common.content.escape_race.client;

import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitz;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.ClientBbSelfState;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
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
