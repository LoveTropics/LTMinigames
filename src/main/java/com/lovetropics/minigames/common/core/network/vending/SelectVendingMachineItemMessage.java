package com.lovetropics.minigames.common.core.network.vending;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectVendingMachineItemMessage(int entityId, int itemId) implements CustomPacketPayload {
	public static final Type<SelectVendingMachineItemMessage> TYPE = new Type<>(LoveTropics.id("select_vending_machine_item_message"));
	public static final StreamCodec<ByteBuf, SelectVendingMachineItemMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SelectVendingMachineItemMessage::entityId,
			ByteBufCodecs.VAR_INT, SelectVendingMachineItemMessage::itemId,
			SelectVendingMachineItemMessage::new
	);

	public static void handle(final SelectVendingMachineItemMessage message, final IPayloadContext context) {
		ServerPlayer player = (ServerPlayer) context.player();
		if (!(player.level().getEntity(message.entityId) instanceof VendingMachineEntity vendingMachine)) {
			return;
		}
		if (player.isWithinEntityInteractionRange(vendingMachine, ServerPlayer.ENTITY_INTERACTION_DISTANCE_VERIFICATION_BUFFER)) {
			vendingMachine.trySelect(message.itemId);
		}
	}

	@Override
	public Type<SelectVendingMachineItemMessage> type() {
		return TYPE;
	}
}
