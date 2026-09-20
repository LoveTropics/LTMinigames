package org.lovetropics.games.common.core.network.workspace;

import com.lovetropics.lib.BlockBox;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.client.map.ClientMapWorkspace;
import org.lovetropics.games.common.core.map.workspace.MapWorkspaceManager;
import org.lovetropics.games.common.core.map.workspace.WorkspaceRegions;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record UpdateWorkspaceRegionMessage(int id, Optional<BlockBox> region) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<UpdateWorkspaceRegionMessage> TYPE = new CustomPacketPayload.Type<>(LoveTropics.id("update_workspace_region"));

	public static final StreamCodec<ByteBuf, UpdateWorkspaceRegionMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, UpdateWorkspaceRegionMessage::id,
			BlockBox.STREAM_CODEC.apply(ByteBufCodecs::optional), UpdateWorkspaceRegionMessage::region,
			UpdateWorkspaceRegionMessage::new
	);

	public void handleServerbound(IPayloadContext context) {
		ServerPlayer sender = (ServerPlayer) context.player();
		MapWorkspaceManager workspaceManager = MapWorkspaceManager.get(sender.level().getServer());
		WorkspaceRegions regions = workspaceManager.getRegions(sender.level().getServer(), sender.level().dimension());
		if (regions != null) {
			regions.set(sender.level(), id, region.orElse(null));
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
