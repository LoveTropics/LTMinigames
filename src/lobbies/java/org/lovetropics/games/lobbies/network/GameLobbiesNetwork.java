package org.lovetropics.games.lobbies.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.lobbies.LobbiesMod;
import org.lovetropics.games.lobbies.client.select_role.ClientRoleSelection;

@EventBusSubscriber(modid = LobbiesMod.ID)
public class GameLobbiesNetwork {
	private static final String VERSION = "1";

	@SubscribeEvent
	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(VERSION);

		registrar.playToClient(LobbyUpdateMessage.TYPE, LobbyUpdateMessage.STREAM_CODEC);
		registrar.playToClient(JoinedLobbyMessage.TYPE, JoinedLobbyMessage.STREAM_CODEC);
		registrar.playToClient(LeftLobbyMessage.TYPE, LeftLobbyMessage.STREAM_CODEC);
		registrar.playToClient(LobbyPlayersMessage.TYPE, LobbyPlayersMessage.STREAM_CODEC);

		registrar.playToClient(ClientManageLobbyMessage.TYPE, ClientManageLobbyMessage.STREAM_CODEC);
		registrar.playToServer(ServerManageLobbyMessage.TYPE, ServerManageLobbyMessage.STREAM_CODEC, ServerManageLobbyMessage::handle);

		registrar.playToClient(SelectRolePromptMessage.TYPE, SelectRolePromptMessage.STREAM_CODEC);
		registrar.playToServer(SelectRoleMessage.TYPE, SelectRoleMessage.STREAM_CODEC, SelectRoleMessage::handle);
		registrar.playToServer(ManageOrCreateLobbyPacket.TYPE, ManageOrCreateLobbyPacket.STREAM_CODEC, ManageOrCreateLobbyPacket::handle);
		registrar.playToServer(JoinLobbyPacket.TYPE, JoinLobbyPacket.STREAM_CODEC, JoinLobbyPacket::handle);
		registrar.playToServer(LeaveLobbyPacket.TYPE, LeaveLobbyPacket.STREAM_CODEC, LeaveLobbyPacket::handle);
	}

	@SubscribeEvent
	public static void registerClientHandler(RegisterClientPayloadHandlersEvent event) {
		event.register(LobbyUpdateMessage.TYPE, LobbyUpdateMessage::handle);
		event.register(JoinedLobbyMessage.TYPE, JoinedLobbyMessage::handle);
		event.register(LeftLobbyMessage.TYPE, LeftLobbyMessage::handle);
		event.register(LobbyPlayersMessage.TYPE, LobbyPlayersMessage::handle);
		event.register(ClientManageLobbyMessage.TYPE, ClientManageLobbyMessage::handle);
		event.register(SelectRolePromptMessage.TYPE, (message, _) -> ClientRoleSelection.openScreen(message.lobbyId()));
	}
}
