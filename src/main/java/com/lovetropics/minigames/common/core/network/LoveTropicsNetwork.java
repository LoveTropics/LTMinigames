package com.lovetropics.minigames.common.core.network;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.lobby.LeaveLobbyPacket;
import com.lovetropics.minigames.client.lobby.ManageOrCreateLobbyPacket;
import com.lovetropics.minigames.client.lobby.manage.ClientManageLobbyMessage;
import com.lovetropics.minigames.client.lobby.manage.ServerManageLobbyMessage;
import com.lovetropics.minigames.client.lobby.select_role.SelectRoleMessage;
import com.lovetropics.minigames.client.lobby.select_role.SelectRolePromptMessage;
import com.lovetropics.minigames.client.lobby.state.message.JoinedLobbyMessage;
import com.lovetropics.minigames.client.lobby.state.message.LeftLobbyMessage;
import com.lovetropics.minigames.client.lobby.state.message.LobbyPlayersMessage;
import com.lovetropics.minigames.client.lobby.state.message.LobbyUpdateMessage;
import com.lovetropics.minigames.client.particle_line.DrawParticleLineMessage;
import com.lovetropics.minigames.client.toast.ShowNotificationToastMessage;
import com.lovetropics.minigames.common.core.network.trivia.RequestTriviaStateUpdateMessage;
import com.lovetropics.minigames.common.core.network.trivia.SelectTriviaAnswerMessage;
import com.lovetropics.minigames.common.core.network.trivia.ShowTriviaMessage;
import com.lovetropics.minigames.common.core.network.trivia.TriviaAnswerResponseMessage;
import com.lovetropics.minigames.common.core.network.workspace.AddWorkspaceRegionMessage;
import com.lovetropics.minigames.common.core.network.workspace.SetWorkspaceMessage;
import com.lovetropics.minigames.common.core.network.workspace.UpdateWorkspaceRegionMessage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = LoveTropics.ID)
public final class LoveTropicsNetwork {
	@SubscribeEvent
	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(LoveTropics.getCompatVersion());
		registrar.playToClient(SetWorkspaceMessage.TYPE, SetWorkspaceMessage.STREAM_CODEC);
		registrar.playToClient(AddWorkspaceRegionMessage.TYPE, AddWorkspaceRegionMessage.STREAM_CODEC);
		registrar.playBidirectional(UpdateWorkspaceRegionMessage.TYPE, UpdateWorkspaceRegionMessage.STREAM_CODEC, UpdateWorkspaceRegionMessage::handleServerbound);

		registrar.playToServer(SpectatePlayerAndTeleportMessage.TYPE, SpectatePlayerAndTeleportMessage.STREAM_CODEC, SpectatePlayerAndTeleportMessage::handle);

		registrar.playToClient(LobbyUpdateMessage.TYPE, LobbyUpdateMessage.STREAM_CODEC);
		registrar.playToClient(JoinedLobbyMessage.TYPE, JoinedLobbyMessage.STREAM_CODEC);
		registrar.playToClient(LeftLobbyMessage.TYPE, LeftLobbyMessage.STREAM_CODEC);
		registrar.playToClient(LobbyPlayersMessage.TYPE, LobbyPlayersMessage.STREAM_CODEC);

		registrar.playToClient(PlayerDisguiseMessage.TYPE, PlayerDisguiseMessage.STREAM_CODEC);
		registrar.playToClient(ShowNotificationToastMessage.TYPE, ShowNotificationToastMessage.STREAM_CODEC);

		registrar.playToClient(ClientManageLobbyMessage.TYPE, ClientManageLobbyMessage.STREAM_CODEC);
		registrar.playToServer(ServerManageLobbyMessage.TYPE, ServerManageLobbyMessage.STREAM_CODEC, ServerManageLobbyMessage::handle);

		registrar.playToClient(SetGameClientStateMessage.TYPE, SetGameClientStateMessage.STREAM_CODEC);

		registrar.playToClient(SelectRolePromptMessage.TYPE, SelectRolePromptMessage.STREAM_CODEC);
		registrar.playToServer(SelectRoleMessage.TYPE, SelectRoleMessage.STREAM_CODEC, SelectRoleMessage::handle);
		registrar.playToServer(ManageOrCreateLobbyPacket.TYPE, ManageOrCreateLobbyPacket.STREAM_CODEC, ManageOrCreateLobbyPacket::handle);
		registrar.playToServer(JoinedLobbyMessage.TYPE, JoinedLobbyMessage.STREAM_CODEC, JoinedLobbyMessage::handle);
		registrar.playToServer(LeaveLobbyPacket.TYPE, LeaveLobbyPacket.STREAM_CODEC, LeaveLobbyPacket::handle);

		registrar.playToClient(DrawParticleLineMessage.TYPE, DrawParticleLineMessage.STREAM_CODEC);

		registrar.playToClient(SpectatorPlayerActivityMessage.TYPE, SpectatorPlayerActivityMessage.STREAM_CODEC);

		registrar.playToClient(FillFluidPacket.TYPE, FillFluidPacket.STREAM_CODEC);

		registrar.playToClient(ShowTriviaMessage.TYPE, ShowTriviaMessage.STREAM_CODEC);
		registrar.playToServer(SelectTriviaAnswerMessage.TYPE, SelectTriviaAnswerMessage.STREAM_CODEC, SelectTriviaAnswerMessage::handle);
		registrar.playToServer(RequestTriviaStateUpdateMessage.TYPE, RequestTriviaStateUpdateMessage.STREAM_CODEC, RequestTriviaStateUpdateMessage::handle);
		registrar.playToClient(TriviaAnswerResponseMessage.TYPE, TriviaAnswerResponseMessage.STREAM_CODEC);

		registrar.playToClient(SetForcedPoseMessage.TYPE, SetForcedPoseMessage.STREAM_CODEC);
	}

	@SubscribeEvent
	public static void registerClientHandler(RegisterClientPayloadHandlersEvent event) {
		event.register(SetWorkspaceMessage.TYPE, SetWorkspaceMessage::handle);
		event.register(AddWorkspaceRegionMessage.TYPE, AddWorkspaceRegionMessage::handle);
		event.register(UpdateWorkspaceRegionMessage.TYPE, UpdateWorkspaceRegionMessage::handleClientbound);

		event.register(LobbyUpdateMessage.TYPE, LobbyUpdateMessage::handle);
		event.register(JoinedLobbyMessage.TYPE, JoinedLobbyMessage::handle);
		event.register(LeftLobbyMessage.TYPE, LeftLobbyMessage::handle);
		event.register(LobbyPlayersMessage.TYPE, LobbyPlayersMessage::handle);

		event.register(PlayerDisguiseMessage.TYPE, PlayerDisguiseMessage::handle);
		event.register(ShowNotificationToastMessage.TYPE, ShowNotificationToastMessage::handle);

		event.register(ClientManageLobbyMessage.TYPE, ClientManageLobbyMessage::handle);

		event.register(SetGameClientStateMessage.TYPE, SetGameClientStateMessage::handle);

		event.register(SelectRolePromptMessage.TYPE, SelectRolePromptMessage::handle);

		event.register(DrawParticleLineMessage.TYPE, DrawParticleLineMessage::handle);

		event.register(SpectatorPlayerActivityMessage.TYPE, SpectatorPlayerActivityMessage::handle);

		event.register(FillFluidPacket.TYPE, FillFluidPacket::handle);

		event.register(ShowTriviaMessage.TYPE, ShowTriviaMessage::handle);
		event.register(TriviaAnswerResponseMessage.TYPE, TriviaAnswerResponseMessage::handle);

		event.register(SetForcedPoseMessage.TYPE, SetForcedPoseMessage::handle);
	}
}
