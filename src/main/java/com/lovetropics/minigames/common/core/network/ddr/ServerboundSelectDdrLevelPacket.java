package com.lovetropics.minigames.common.core.network.ddr;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevel;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundSelectDdrLevelPacket(
		int entityId,
		Holder<DdrLevel> level
) implements CustomPacketPayload {
	public static final Type<ServerboundSelectDdrLevelPacket> TYPE = new Type<>(LoveTropics.location("select_ddr_menu_item_message"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSelectDdrLevelPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ServerboundSelectDdrLevelPacket::entityId,
			DdrLevel.STREAM_CODEC, ServerboundSelectDdrLevelPacket::level,
			ServerboundSelectDdrLevelPacket::new
	);

	public static void handle(final ServerboundSelectDdrLevelPacket packet, final IPayloadContext context) {
		ServerPlayer player = (ServerPlayer) context.player();
		if (player.level().getEntity(packet.entityId) instanceof DDRMachineEntity ddrMachineEntity) {
			if (player.canInteractWithEntity(ddrMachineEntity, ServerPlayer.ENTITY_INTERACTION_DISTANCE_VERIFICATION_BUFFER)) {
				ddrMachineEntity.startPlaying(packet.level());
			}
		}
	}

	@Override
	public Type<ServerboundSelectDdrLevelPacket> type() {
		return TYPE;
	}
}
