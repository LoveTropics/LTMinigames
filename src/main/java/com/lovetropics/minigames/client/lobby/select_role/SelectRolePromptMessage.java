package com.lovetropics.minigames.client.lobby.select_role;

import com.lovetropics.minigames.LoveTropics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectRolePromptMessage(int lobbyId) implements CustomPacketPayload {
	public static final Type<SelectRolePromptMessage> TYPE = new Type<>(LoveTropics.id("select_role_prompt"));

	public static final StreamCodec<ByteBuf, SelectRolePromptMessage> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(SelectRolePromptMessage::new, SelectRolePromptMessage::lobbyId);

	public static void handle(SelectRolePromptMessage message, IPayloadContext context) {
		ClientRoleSelection.openScreen(message.lobbyId);
	}

	@Override
	public Type<SelectRolePromptMessage> type() {
		return TYPE;
	}
}
