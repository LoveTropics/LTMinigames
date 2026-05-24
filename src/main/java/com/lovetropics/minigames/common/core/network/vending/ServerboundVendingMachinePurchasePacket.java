package com.lovetropics.minigames.common.core.network.vending;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundVendingMachinePurchasePacket(int entityId) implements CustomPacketPayload {
	public static final Type<ServerboundVendingMachinePurchasePacket> TYPE = new Type<>(LoveTropics.location("vending_machine_purchase"));
	public static final StreamCodec<ByteBuf, ServerboundVendingMachinePurchasePacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ServerboundVendingMachinePurchasePacket::entityId,
			ServerboundVendingMachinePurchasePacket::new
	);

	public static void handle(final ServerboundVendingMachinePurchasePacket message, final IPayloadContext context) {
		ServerPlayer player = (ServerPlayer) context.player();
		if (!(player.level().getEntity(message.entityId) instanceof VendingMachineEntity vendingMachine)) {
			return;
		}
		if (player.isWithinEntityInteractionRange(vendingMachine, ServerPlayer.ENTITY_INTERACTION_DISTANCE_VERIFICATION_BUFFER)) {
			int selected = vendingMachine.getSelected();
			if (selected != VendingMachineEntity.NO_SLOT) {
				vendingMachine.tryPurchase(player, selected);
			}
		}
	}

	@Override
	public Type<ServerboundVendingMachinePurchasePacket> type() {
		return TYPE;
	}
}
