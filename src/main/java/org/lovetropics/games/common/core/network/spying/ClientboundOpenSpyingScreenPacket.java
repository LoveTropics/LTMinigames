package org.lovetropics.games.common.core.network.spying;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;

import java.util.List;

public record ClientboundOpenSpyingScreenPacket(List<ShopClientState.Target> targets) implements CustomPacketPayload {
	public static final Type<ClientboundOpenSpyingScreenPacket> TYPE = new Type<>(LoveTropics.id("open_spying_screen"));
	public static final StreamCodec<ByteBuf, ClientboundOpenSpyingScreenPacket> STREAM_CODEC = ByteBufCodecs.fromCodec(ShopClientState.Target.CODEC.listOf())
			.map(ClientboundOpenSpyingScreenPacket::new, ClientboundOpenSpyingScreenPacket::targets);

	@Override
	public Type<ClientboundOpenSpyingScreenPacket> type() {
		return TYPE;
	}
}
