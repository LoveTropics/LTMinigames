package com.lovetropics.minigames.common.core.network.ddr;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundDdrInputHitPacket(int entityId, long inputTick) implements CustomPacketPayload {
	public static final Type<ClientboundDdrInputHitPacket> TYPE = new Type<>(LoveTropics.id("ddr_input_hit"));
	public static final StreamCodec<ByteBuf, ClientboundDdrInputHitPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ClientboundDdrInputHitPacket::entityId,
			ByteBufCodecs.VAR_LONG, ClientboundDdrInputHitPacket::inputTick,
			ClientboundDdrInputHitPacket::new
	);

	public static void handle(final ClientboundDdrInputHitPacket packet, final IPayloadContext context) {
		if (context.player().level().getEntity(packet.entityId()) instanceof DDRMachineEntity ddrMachine) {
			ddrMachine.handleRemoteClientInputHit(packet.inputTick());
		}
	}

	@Override
	public Type<ClientboundDdrInputHitPacket> type() {
		return TYPE;
	}
}
