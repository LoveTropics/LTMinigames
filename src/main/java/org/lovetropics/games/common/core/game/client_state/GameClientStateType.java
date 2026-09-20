package org.lovetropics.games.common.core.game.client_state;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record GameClientStateType<T extends GameClientState>(
		MapCodec<T> codec,
		StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec
) {
	public GameClientStateType(MapCodec<T> codec) {
		this(codec, ByteBufCodecs.fromCodecWithRegistriesTrusted(codec.codec()));
	}
}
