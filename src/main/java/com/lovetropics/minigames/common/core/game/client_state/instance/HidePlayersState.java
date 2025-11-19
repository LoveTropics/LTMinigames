package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.util.Codecs;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record HidePlayersState(
		IntSet playerIds
) implements GameClientState {
	public static final HidePlayersState EMPTY = new HidePlayersState(IntSet.of());

	public static final MapCodec<HidePlayersState> CODEC = Codecs.no();
	public static final StreamCodec<RegistryFriendlyByteBuf, HidePlayersState> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.collection(IntOpenHashSet::new, ByteBufCodecs.VAR_INT), HidePlayersState::playerIds,
			HidePlayersState::new
	);

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.HIDE_PLAYERS.get();
	}
}
