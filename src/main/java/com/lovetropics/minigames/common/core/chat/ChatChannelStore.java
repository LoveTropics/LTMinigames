package com.lovetropics.minigames.common.core.chat;

import com.lovetropics.minigames.Constants;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ChatChannelStore {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Constants.MODID);

	public static final Supplier<AttachmentType<ChatChannel>> ATTACHMENT = ATTACHMENT_TYPES.register(
			"chat_channel", () -> AttachmentType.builder(() -> ChatChannel.GLOBAL).build()
	);

	public static void set(ServerPlayer player, ChatChannel channel) {
		player.setData(ATTACHMENT, channel);
	}

	public static ChatChannel get(ServerPlayer player) {
		return player.getData(ATTACHMENT);
	}
}
