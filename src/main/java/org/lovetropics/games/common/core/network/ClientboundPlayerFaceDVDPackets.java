package org.lovetropics.games.common.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.client.gui.PlayerFaceDVDRender;

import java.util.UUID;

public class ClientboundPlayerFaceDVDPackets {

	public record Add(UUID uuid, int lengthInTicks, int count) implements CustomPacketPayload {

		public static final Type<Add> TYPE = new Type<>(LoveTropics.id("add_player_face_dvd"));

		public static final StreamCodec<ByteBuf, Add> STREAM_CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, Add::uuid,
				ByteBufCodecs.VAR_INT, Add::lengthInTicks,
				ByteBufCodecs.VAR_INT, Add::count,
				Add::new
		);

		public static void handle(Add add, IPayloadContext context) {
			for (int i = 0; i < add.count; i++) {
				PlayerFaceDVDRender.add(add.uuid(), add.lengthInTicks());
			}
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record Clear() implements CustomPacketPayload {

		public static final Type<Clear> TYPE = new Type<>(LoveTropics.id("clear_player_face_dvd"));

		public static final StreamCodec<ByteBuf, Clear> STREAM_CODEC = StreamCodec.unit(new Clear());

		public static void handle(Clear message, IPayloadContext context) {
			PlayerFaceDVDRender.clear();
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

}
