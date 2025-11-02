package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record DDRMachineLevelClient(ResourceLocation id, ItemStack icon, Component displayName) {
	public static final StreamCodec<RegistryFriendlyByteBuf, DDRMachineLevelClient> STREAM_CODEC = StreamCodec.of((output, definition) -> definition.encode(output), DDRMachineLevelClient::decode);

	public static DDRMachineLevelClient decode(RegistryFriendlyByteBuf buffer) {
		ResourceLocation id = buffer.readResourceLocation();
		ItemStack icon = ItemStack.STREAM_CODEC.decode(buffer);
		Component displayName = ComponentSerialization.STREAM_CODEC.decode(buffer);
		return new DDRMachineLevelClient(id, icon, displayName);
	}

	public void encode(RegistryFriendlyByteBuf buffer) {
		buffer.writeResourceLocation(id);
		ItemStack.STREAM_CODEC.encode(buffer, icon);
		ComponentSerialization.STREAM_CODEC.encode(buffer, displayName);
	}
}
