package com.lovetropics.minigames.common.core.network;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.handler.spectate.ClientSpectatingManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record SpectatorPlayerActivityMessage(UUID player, int color) implements CustomPacketPayload {
	public static final Type<SpectatorPlayerActivityMessage> TYPE = new Type<>(LoveTropics.id("spectator_player_activity"));

	public static final StreamCodec<ByteBuf, SpectatorPlayerActivityMessage> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, SpectatorPlayerActivityMessage::player,
			ByteBufCodecs.INT, SpectatorPlayerActivityMessage::color,
			SpectatorPlayerActivityMessage::new
	);

	public static void handle(SpectatorPlayerActivityMessage message, IPayloadContext context) {
		ClientSpectatingManager.INSTANCE.onPlayerActivity(message.player, message.color);
	}

	@Override
	public Type<SpectatorPlayerActivityMessage> type() {
		return TYPE;
	}
}
