package com.lovetropics.minigames.client.toast;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class NotificationToasts {
	static void display(Component message, NotificationStyle style) {
		Minecraft client = Minecraft.getInstance();
		client.getToasts().addToast(new NotificationToast(message, style));
	}
}
