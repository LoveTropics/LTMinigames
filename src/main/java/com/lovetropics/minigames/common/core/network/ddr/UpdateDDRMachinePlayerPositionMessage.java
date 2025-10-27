package com.lovetropics.minigames.common.core.network.ddr;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateDDRMachinePlayerPositionMessage(boolean left, boolean right, boolean forward, boolean backwards) implements CustomPacketPayload {
	public static final Type<UpdateDDRMachinePlayerPositionMessage> TYPE = new Type<>(LoveTropics.location("update_ddr_machine_player_position_message"));
	public static final StreamCodec<ByteBuf, UpdateDDRMachinePlayerPositionMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, UpdateDDRMachinePlayerPositionMessage::left,
			ByteBufCodecs.BOOL, UpdateDDRMachinePlayerPositionMessage::right,
			ByteBufCodecs.BOOL, UpdateDDRMachinePlayerPositionMessage::forward,
			ByteBufCodecs.BOOL, UpdateDDRMachinePlayerPositionMessage::backwards,
			UpdateDDRMachinePlayerPositionMessage::new
	);

	public static void handle(final UpdateDDRMachinePlayerPositionMessage message, final IPayloadContext context) {
		ServerPlayer player = (ServerPlayer) context.player();
		if (player.getVehicle() instanceof DDRMachineEntity ddrMachineEntity) {
			ddrMachineEntity.updatePlayerPosition(message.left(),  message.right(), message.forward(), message.backwards());
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
