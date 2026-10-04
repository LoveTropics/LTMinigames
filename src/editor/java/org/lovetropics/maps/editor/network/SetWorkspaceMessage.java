package org.lovetropics.maps.editor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;
import org.lovetropics.maps.editor.MapsEditorMod;
import org.lovetropics.maps.editor.client.ClientMapWorkspace;
import org.lovetropics.maps.editor.workspace.ClientWorkspaceRegions;
import org.lovetropics.maps.editor.workspace.WorkspaceRegions;

public class SetWorkspaceMessage implements CustomPacketPayload {
	public static final Type<SetWorkspaceMessage> TYPE = new Type<>(MapsEditorMod.id("set_workspace"));

	public static final StreamCodec<FriendlyByteBuf, SetWorkspaceMessage> STREAM_CODEC = StreamCodec.ofMember(SetWorkspaceMessage::encode, SetWorkspaceMessage::decode);

	private static final SetWorkspaceMessage HIDDEN = new SetWorkspaceMessage((WorkspaceRegions) null);

	public static SetWorkspaceMessage hidden() {
		return HIDDEN;
	}

	private @Nullable WorkspaceRegions server;
	private @Nullable ClientWorkspaceRegions client;

	public SetWorkspaceMessage(@Nullable WorkspaceRegions server) {
		this.server = server;
	}

	public SetWorkspaceMessage(ClientWorkspaceRegions client) {
		this.client = client;
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBoolean(server != null);
		if (server != null) {
			server.write(buffer);
		}
	}

	public static SetWorkspaceMessage decode(FriendlyByteBuf buffer) {
		ClientWorkspaceRegions regions;
		if (buffer.readBoolean()) {
			regions = ClientWorkspaceRegions.read(buffer);
		} else {
			regions = ClientWorkspaceRegions.noop();
		}
		return new SetWorkspaceMessage(regions);
	}

	public static void handle(SetWorkspaceMessage message, IPayloadContext context) {
		ClientMapWorkspace.INSTANCE.setRegions(message.client);
	}

	@Override
	public Type<SetWorkspaceMessage> type() {
		return TYPE;
	}
}
