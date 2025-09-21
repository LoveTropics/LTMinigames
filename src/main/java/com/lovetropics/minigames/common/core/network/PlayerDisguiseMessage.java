package com.lovetropics.minigames.common.core.network;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.ClientPlayerDisguises;
import com.lovetropics.minigames.common.core.diguise.DisguiseType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record PlayerDisguiseMessage(int entityId, Optional<DisguiseType> disguise) implements CustomPacketPayload {
	public static final Type<PlayerDisguiseMessage> TYPE = new Type<>(LoveTropics.location("player_disguise"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PlayerDisguiseMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PlayerDisguiseMessage::entityId,
			DisguiseType.STREAM_CODEC.apply(ByteBufCodecs::optional), PlayerDisguiseMessage::disguise,
			PlayerDisguiseMessage::new
	);

	public static void handle(PlayerDisguiseMessage message, IPayloadContext context) {
		ClientPlayerDisguises.updateClientDisguise(message.entityId, message.disguise.orElse(null));
	}

	@Override
	public Type<PlayerDisguiseMessage> type() {
		return TYPE;
	}
}
