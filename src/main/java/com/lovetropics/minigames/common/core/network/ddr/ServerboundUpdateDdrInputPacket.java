package com.lovetropics.minigames.common.core.network.ddr;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundUpdateDdrInputPacket(DdrInput input) implements CustomPacketPayload {
	public static final Type<ServerboundUpdateDdrInputPacket> TYPE = new Type<>(LoveTropics.location("ddr_input"));
	public static final StreamCodec<ByteBuf, ServerboundUpdateDdrInputPacket> STREAM_CODEC = StreamCodec.composite(
			DdrInput.STREAM_CODEC, ServerboundUpdateDdrInputPacket::input,
			ServerboundUpdateDdrInputPacket::new
	);

	public static void handle(final ServerboundUpdateDdrInputPacket message, final IPayloadContext context) {
		if (context.player() instanceof ServerPlayer player && player.getVehicle() instanceof DDRMachineEntity ddrMachineEntity) {
			ddrMachineEntity.updatePlayerInput(message.input());
		}
	}

	@Override
	public Type<ServerboundUpdateDdrInputPacket> type() {
		return TYPE;
	}
}
