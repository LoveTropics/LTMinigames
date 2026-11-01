package org.lovetropics.games.common.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.IGameLookup;
import org.lovetropics.games.common.core.game.IGamePhase;

import java.util.Set;
import java.util.UUID;

public record SpectatePlayerAndTeleportMessage(UUID player) implements CustomPacketPayload {
	public static final Type<SpectatePlayerAndTeleportMessage> TYPE = new Type<>(LoveTropics.id("spectate_player_and_teleport"));

	public static final StreamCodec<ByteBuf, SpectatePlayerAndTeleportMessage> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, SpectatePlayerAndTeleportMessage::player,
			SpectatePlayerAndTeleportMessage::new
	);

	public static void handle(SpectatePlayerAndTeleportMessage message, IPayloadContext context) {
		ServerPlayer sender = (ServerPlayer) context.player();
		if (!sender.isSpectator()) {
			return;
		}

		IGamePhase game = IGameLookup.get().getGamePhaseFor(sender);
		ServerPlayer target = game != null ? game.allPlayers().getPlayerBy(message.player) : null;
		if (target != null) {
			sender.teleportTo(target.level(), target.getX(), target.getY(), target.getZ(), Set.of(), target.getYRot(), target.getXRot(), true);
		}

		sender.setCamera(target);
	}

	@Override
	public Type<SpectatePlayerAndTeleportMessage> type() {
		return TYPE;
	}
}
