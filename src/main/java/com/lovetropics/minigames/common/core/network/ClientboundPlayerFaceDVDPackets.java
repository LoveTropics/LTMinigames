package com.lovetropics.minigames.common.core.network;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.gui.PlayerFaceDVDRender;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class ClientboundPlayerFaceDVDPackets {

	public record Add(UUID uuid, int lengthInTicks) implements CustomPacketPayload {

		public static final Type<Add> TYPE = new Type<>(LoveTropics.id("add_player_face_dvd"));

		public static final StreamCodec<ByteBuf, Add> STREAM_CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, Add::uuid,
				ByteBufCodecs.INT, Add::lengthInTicks,
				Add::new
		);

		public static void handle(final Add add, final IPayloadContext context) {
			PlayerFaceDVDRender.add(add.uuid(), add.lengthInTicks());
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record Clear() implements CustomPacketPayload {

		public static final Type<Clear> TYPE = new Type<>(LoveTropics.id("clear_player_face_dvd"));

		public static final StreamCodec<ByteBuf, Clear> STREAM_CODEC = StreamCodec.unit(new Clear());

		public static void handle(final Clear message, final IPayloadContext context) {
			PlayerFaceDVDRender.clear();
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

}
