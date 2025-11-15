package com.lovetropics.minigames.common.core.network.vending;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundVendingMachineDropPacket(int entityId, ItemStack itemStack, int fromSlot) implements CustomPacketPayload {
	public static final Type<ClientboundVendingMachineDropPacket> TYPE = new Type<>(LoveTropics.location("vending_machine_drop"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundVendingMachineDropPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ClientboundVendingMachineDropPacket::entityId,
			ItemStack.STREAM_CODEC, ClientboundVendingMachineDropPacket::itemStack,
			ByteBufCodecs.VAR_INT, ClientboundVendingMachineDropPacket::fromSlot,
			ClientboundVendingMachineDropPacket::new
	);

	public static void handle(ClientboundVendingMachineDropPacket packet, IPayloadContext context) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level.getEntity(packet.entityId) instanceof VendingMachineEntity vendingMachine) {
			vendingMachine.startDropping(packet.itemStack, packet.fromSlot);
		}
	}

	@Override
	public Type<ClientboundVendingMachineDropPacket> type() {
		return TYPE;
	}
}
