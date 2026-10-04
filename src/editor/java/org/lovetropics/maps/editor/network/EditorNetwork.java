package org.lovetropics.maps.editor.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.lovetropics.maps.editor.MapsEditorMod;

@EventBusSubscriber(modid = MapsEditorMod.ID)
public final class EditorNetwork {
	@SubscribeEvent
	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");
		registrar.playToClient(SetWorkspaceMessage.TYPE, SetWorkspaceMessage.STREAM_CODEC);
		registrar.playToClient(AddWorkspaceRegionMessage.TYPE, AddWorkspaceRegionMessage.STREAM_CODEC);
		registrar.playBidirectional(UpdateWorkspaceRegionMessage.TYPE, UpdateWorkspaceRegionMessage.STREAM_CODEC, UpdateWorkspaceRegionMessage::handleServerbound);
	}

	@SubscribeEvent
	public static void registerClientHandler(RegisterClientPayloadHandlersEvent event) {
		event.register(SetWorkspaceMessage.TYPE, SetWorkspaceMessage::handle);
		event.register(AddWorkspaceRegionMessage.TYPE, AddWorkspaceRegionMessage::handle);
		event.register(UpdateWorkspaceRegionMessage.TYPE, UpdateWorkspaceRegionMessage::handleClientbound);
	}
}
