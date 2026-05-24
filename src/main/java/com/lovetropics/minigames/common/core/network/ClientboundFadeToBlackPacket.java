package com.lovetropics.minigames.common.core.network;

import com.lovetropics.minigames.LoveTropics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundFadeToBlackPacket(boolean fadeIn, int duration) implements CustomPacketPayload {
	public static final Type<ClientboundFadeToBlackPacket> TYPE = new Type<>(LoveTropics.id("fade_to_black"));

	public static final StreamCodec<ByteBuf, ClientboundFadeToBlackPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, ClientboundFadeToBlackPacket::fadeIn,
			ByteBufCodecs.VAR_INT, ClientboundFadeToBlackPacket::duration,
			ClientboundFadeToBlackPacket::new
	);

	@Override
	public Type<ClientboundFadeToBlackPacket> type() {
		return TYPE;
	}
}
