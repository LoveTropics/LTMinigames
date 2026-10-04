package org.lovetropics.maps.editor.network;

import com.lovetropics.lib.BlockBox;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lovetropics.maps.editor.MapsEditorMod;
import org.lovetropics.maps.editor.client.ClientMapWorkspace;

public record AddWorkspaceRegionMessage(int id, String key, BlockBox region) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<AddWorkspaceRegionMessage> TYPE = new CustomPacketPayload.Type<>(MapsEditorMod.id("add_workspace_region"));

	public static final StreamCodec<ByteBuf, AddWorkspaceRegionMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, AddWorkspaceRegionMessage::id,
			ByteBufCodecs.stringUtf8(64), AddWorkspaceRegionMessage::key,
			BlockBox.STREAM_CODEC, AddWorkspaceRegionMessage::region,
			AddWorkspaceRegionMessage::new
	);

	public static void handle(AddWorkspaceRegionMessage message, IPayloadContext context) {
		ClientMapWorkspace.INSTANCE.addRegion(message.id, message.key, message.region);
	}

	@Override
	public Type<AddWorkspaceRegionMessage> type() {
		return TYPE;
	}
}
