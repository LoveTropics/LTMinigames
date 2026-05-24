package com.lovetropics.minigames.common.core.network;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.ClientPoseHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record SetForcedPoseMessage(Optional<Pose> pose) implements CustomPacketPayload {

	public static final Type<SetForcedPoseMessage> TYPE = new Type<>(LoveTropics.id("set_forced_pose"));
	public static final StreamCodec<ByteBuf, SetForcedPoseMessage> STREAM_CODEC = StreamCodec.composite(
			Pose.STREAM_CODEC.apply(ByteBufCodecs::optional), SetForcedPoseMessage::pose,
			SetForcedPoseMessage::new
	);

	public static void handle(final SetForcedPoseMessage message, final IPayloadContext context) {
		ClientPoseHandler.updateForcedPose(message.pose().orElse(null));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
