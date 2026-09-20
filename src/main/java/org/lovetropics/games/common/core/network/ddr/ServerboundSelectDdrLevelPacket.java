package org.lovetropics.games.common.core.network.ddr;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.content.escape_race.ddr_machine.DDRMachineEntity;
import org.lovetropics.games.common.content.escape_race.ddr_machine.levels.DdrLevel;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundSelectDdrLevelPacket(
		Holder<DdrLevel> level
) implements CustomPacketPayload {
	public static final Type<ServerboundSelectDdrLevelPacket> TYPE = new Type<>(LoveTropics.id("select_ddr_menu_item_message"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSelectDdrLevelPacket> STREAM_CODEC = StreamCodec.composite(
			DdrLevel.STREAM_CODEC, ServerboundSelectDdrLevelPacket::level,
			ServerboundSelectDdrLevelPacket::new
	);

	public static void handle(ServerboundSelectDdrLevelPacket packet, IPayloadContext context) {
		if (context.player() instanceof ServerPlayer player && player.getControlledVehicle() instanceof DDRMachineEntity ddrMachineEntity) {
			if (player.isWithinEntityInteractionRange(ddrMachineEntity, ServerPlayer.ENTITY_INTERACTION_DISTANCE_VERIFICATION_BUFFER)) {
				ddrMachineEntity.startPlaying(player, packet.level());
			}
		}
	}

	@Override
	public Type<ServerboundSelectDdrLevelPacket> type() {
		return TYPE;
	}
}
