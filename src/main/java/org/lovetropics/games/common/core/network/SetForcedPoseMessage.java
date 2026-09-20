package org.lovetropics.games.common.core.network;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.client.ClientPoseHandler;
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

	public static void handle(SetForcedPoseMessage message, IPayloadContext context) {
		ClientPoseHandler.updateForcedPose(message.pose().orElse(null));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
