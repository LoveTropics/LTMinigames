package com.lovetropics.minigames.common.core.network.ddr;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DDRMachineLevel;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DDRMachineLevels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectDDRMenuItemMessage(int entityId, int itemId) implements CustomPacketPayload {
	public static final Type<SelectDDRMenuItemMessage> TYPE = new Type<>(LoveTropics.location("select_ddr_menu_item_message"));
	public static final StreamCodec<ByteBuf, SelectDDRMenuItemMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SelectDDRMenuItemMessage::entityId,
			ByteBufCodecs.VAR_INT, SelectDDRMenuItemMessage::itemId,
			SelectDDRMenuItemMessage::new
	);

	public static void handle(final SelectDDRMenuItemMessage message, final IPayloadContext context) {
		ServerPlayer player = (ServerPlayer) context.player();
		if (player.level().getEntity(message.entityId) instanceof DDRMachineEntity ddrMachineEntity) {
			if(player.hasLineOfSight(ddrMachineEntity) && player.getLookAngle().dot(ddrMachineEntity.getLookAngle()) < 1){
				ResourceLocation id = ddrMachineEntity.getAvailableLevels().get(message.itemId()).id();
				DDRMachineLevel ddrMachineLevel = DDRMachineLevels.REGISTRY.get(id);
				ddrMachineEntity.startPlaying(ddrMachineLevel);
			}
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
