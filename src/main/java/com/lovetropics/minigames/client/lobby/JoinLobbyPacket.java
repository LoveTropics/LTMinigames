package com.lovetropics.minigames.client.lobby;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.command.game.JoinGameCommand;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record JoinLobbyPacket() implements CustomPacketPayload {
    public static final Type<JoinLobbyPacket> TYPE = new Type<>(LoveTropics.location("join_lobby"));

	public static final JoinLobbyPacket INSTANCE = new JoinLobbyPacket();

    public static final StreamCodec<ByteBuf, JoinLobbyPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void handle(JoinLobbyPacket message, IPayloadContext context) {
		if (context.player() instanceof ServerPlayer player) {
			JoinGameCommand.joinAsRole(null, null, player, player.createCommandSourceStack());
		}
	}

    @Override
    public Type<JoinLobbyPacket> type() {
        return TYPE;
    }
}
