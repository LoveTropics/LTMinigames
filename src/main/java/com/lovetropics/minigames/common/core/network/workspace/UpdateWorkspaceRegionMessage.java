package com.lovetropics.minigames.common.core.network.workspace;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.map.ClientMapWorkspace;
import com.lovetropics.minigames.common.core.map.workspace.MapWorkspace;
import com.lovetropics.minigames.common.core.map.workspace.MapWorkspaceManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record UpdateWorkspaceRegionMessage(int id, Optional<BlockBox> region) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<UpdateWorkspaceRegionMessage> TYPE = new CustomPacketPayload.Type<>(LoveTropics.location("update_workspace_region"));

	public static final StreamCodec<ByteBuf, UpdateWorkspaceRegionMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, UpdateWorkspaceRegionMessage::id,
			BlockBox.STREAM_CODEC.apply(ByteBufCodecs::optional), UpdateWorkspaceRegionMessage::region,
			UpdateWorkspaceRegionMessage::new
	);

	public void handleServerbound(IPayloadContext context) {
		ServerPlayer sender = (ServerPlayer) context.player();
		MapWorkspaceManager workspaceManager = MapWorkspaceManager.get(sender.getServer());
		MapWorkspace workspace = workspaceManager.getWorkspace(sender.level().dimension());
		if (workspace != null) {
			workspace.regions().set(sender.getServer(), id, region.orElse(null));
		}
	}

	public void handleClientbound(IPayloadContext context) {
		ClientMapWorkspace.INSTANCE.updateRegion(id, region.orElse(null));
	}

	@Override
	public Type<UpdateWorkspaceRegionMessage> type() {
		return TYPE;
	}
}
