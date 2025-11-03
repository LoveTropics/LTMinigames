package com.lovetropics.minigames.common.core.network.ddr;

import com.lovetropics.minigames.LoveTropics;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetClientCameraViewMessage(int cameraType) implements CustomPacketPayload {
	public static final Type<SetClientCameraViewMessage> TYPE = new Type<>(LoveTropics.location("set_client_camera_view_message"));
	public static final StreamCodec<ByteBuf, SetClientCameraViewMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SetClientCameraViewMessage::cameraType,
			SetClientCameraViewMessage::new
	);

	public static void handle(final SetClientCameraViewMessage message, final IPayloadContext context) {
		if(message.cameraType > 2 || message.cameraType < 0) {
			return;
		}
		Minecraft.getInstance().options.setCameraType(CameraType.values()[message.cameraType]);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
