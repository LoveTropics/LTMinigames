package com.lovetropics.minigames.common.core.network;

import com.lovetropics.minigames.LoveTropics;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Set;
import java.util.UUID;

public record SpectatePlayerAndTeleportMessage(UUID player) implements CustomPacketPayload {
	public static final Type<SpectatePlayerAndTeleportMessage> TYPE = new Type<>(LoveTropics.location("spectate_player_and_teleport"));

	public static final StreamCodec<ByteBuf, SpectatePlayerAndTeleportMessage> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, SpectatePlayerAndTeleportMessage::player,
			SpectatePlayerAndTeleportMessage::new
	);

	public static void handle(SpectatePlayerAndTeleportMessage message, IPayloadContext context) {
		ServerPlayer sender = (ServerPlayer) context.player();
		if (!sender.isSpectator()) {
			return;
		}

		Player target = sender.level().getPlayerByUUID(message.player);
		if (target != null) {
			sender.teleportTo(sender.level(), target.getX(), target.getY(), target.getZ(), Set.of(), target.getYRot(), target.getXRot(), true);
		}

		sender.setCamera(target);
	}

	@Override
	public Type<SpectatePlayerAndTeleportMessage> type() {
		return TYPE;
	}
}
