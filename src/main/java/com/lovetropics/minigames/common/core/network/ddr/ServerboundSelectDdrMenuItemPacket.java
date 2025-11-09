package com.lovetropics.minigames.common.core.network.ddr;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DDRMachineLevel;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DDRMachineLevelClient;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DDRMachineLevels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record ServerboundSelectDdrMenuItemPacket(int entityId, int itemId) implements CustomPacketPayload {
	public static final Type<ServerboundSelectDdrMenuItemPacket> TYPE = new Type<>(LoveTropics.location("select_ddr_menu_item_message"));
	public static final StreamCodec<ByteBuf, ServerboundSelectDdrMenuItemPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ServerboundSelectDdrMenuItemPacket::entityId,
			ByteBufCodecs.VAR_INT, ServerboundSelectDdrMenuItemPacket::itemId,
			ServerboundSelectDdrMenuItemPacket::new
	);

	public static void handle(final ServerboundSelectDdrMenuItemPacket packet, final IPayloadContext context) {
		ServerPlayer player = (ServerPlayer) context.player();
		if (player.level().getEntity(packet.entityId) instanceof DDRMachineEntity ddrMachineEntity) {
			if (player.canInteractWithEntity(ddrMachineEntity, player.entityInteractionRange() + 1.0)) {
				List<DDRMachineLevelClient> availableLevels = ddrMachineEntity.getAvailableLevels();
				if (packet.itemId() < 0 || packet.itemId() >= availableLevels.size()) {
					return;
				}
				ResourceLocation id = availableLevels.get(packet.itemId()).id();
				DDRMachineLevel level = DDRMachineLevels.REGISTRY.get(id);
				if (level != null) {
					ddrMachineEntity.startPlaying(level);
				}
			}
		}
	}

	@Override
	public Type<ServerboundSelectDdrMenuItemPacket> type() {
		return TYPE;
	}
}
