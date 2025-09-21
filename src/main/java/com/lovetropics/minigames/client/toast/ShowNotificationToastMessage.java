package com.lovetropics.minigames.client.toast;

import com.lovetropics.minigames.LoveTropics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ShowNotificationToastMessage(Component message, NotificationStyle style) implements CustomPacketPayload {
	public static final Type<ShowNotificationToastMessage> TYPE = new Type<>(LoveTropics.location("show_notification_toast"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ShowNotificationToastMessage> STREAM_CODEC = StreamCodec.composite(
			ComponentSerialization.STREAM_CODEC, ShowNotificationToastMessage::message,
			NotificationStyle.STREAM_CODEC, ShowNotificationToastMessage::style,
			ShowNotificationToastMessage::new
	);

	public static void handle(ShowNotificationToastMessage message, IPayloadContext context) {
		NotificationToasts.display(message.message, message.style);
	}

	@Override
	public Type<ShowNotificationToastMessage> type() {
		return TYPE;
	}
}
