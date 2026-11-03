package org.lovetropics.games.common.core.network.spying;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.content.tropic_rush.behavior.SpyingBehavior;
import org.lovetropics.games.common.core.game.IGameLookup;
import org.lovetropics.games.common.core.game.IGamePhase;

import java.util.UUID;

public record ServerboundSpyPacket(UUID target, boolean watch) implements CustomPacketPayload {
	public static final Type<ServerboundSpyPacket> TYPE = new Type<>(LoveTropics.id("spy"));
	public static final StreamCodec<ByteBuf, ServerboundSpyPacket> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, ServerboundSpyPacket::target,
			ByteBufCodecs.BOOL, ServerboundSpyPacket::watch,
			ServerboundSpyPacket::new
	);

	public static void handle(ServerboundSpyPacket message, IPayloadContext context) {
		ServerPlayer player = (ServerPlayer) context.player();
		IGamePhase game = IGameLookup.get().getGamePhaseFor(player);
		SpyingBehavior spying = game != null ? game.instanceState().getOrNull(SpyingBehavior.KEY) : null;
		if (spying != null){
			spying.spy(player, message.target, message.watch);
		}
	}

	@Override
	public Type<ServerboundSpyPacket> type() {
		return TYPE;
	}
}
