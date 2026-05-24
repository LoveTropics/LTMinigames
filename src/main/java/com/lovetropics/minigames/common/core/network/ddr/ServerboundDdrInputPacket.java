package com.lovetropics.minigames.common.core.network.ddr;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundDdrInputPacket(DdrInput input, long inputTick) implements CustomPacketPayload {
	public static final Type<ServerboundDdrInputPacket> TYPE = new Type<>(LoveTropics.id("ddr_input"));
	public static final StreamCodec<ByteBuf, ServerboundDdrInputPacket> STREAM_CODEC = StreamCodec.composite(
			DdrInput.STREAM_CODEC, ServerboundDdrInputPacket::input,
			ByteBufCodecs.VAR_LONG, ServerboundDdrInputPacket::inputTick,
			ServerboundDdrInputPacket::new
	);

	public static void handle(ServerboundDdrInputPacket packet, IPayloadContext context) {
		if (context.player() instanceof ServerPlayer player && player.getControlledVehicle() instanceof DDRMachineEntity ddrMachineEntity) {
			ddrMachineEntity.handleClientInput(player, packet.input(), packet.inputTick());
		}
	}

	@Override
	public Type<ServerboundDdrInputPacket> type() {
		return TYPE;
	}
}
