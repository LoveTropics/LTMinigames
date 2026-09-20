package org.lovetropics.games.common.core.network;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.client.gui.ClientFadeToBlack;
import org.lovetropics.games.client.toast.ShowNotificationToastMessage;
import org.lovetropics.games.common.core.network.ddr.ClientboundDdrInputHitPacket;
import org.lovetropics.games.common.core.network.ddr.ServerboundDdrInputPacket;
import org.lovetropics.games.common.core.network.ddr.ServerboundSelectDdrLevelPacket;
import org.lovetropics.games.common.core.network.trivia.RequestTriviaStateUpdateMessage;
import org.lovetropics.games.common.core.network.trivia.SelectTriviaAnswerMessage;
import org.lovetropics.games.common.core.network.trivia.ShowTriviaMessage;
import org.lovetropics.games.common.core.network.trivia.TriviaAnswerResponseMessage;
import org.lovetropics.games.common.core.network.vending.ClientboundVendingMachineDropPacket;
import org.lovetropics.games.common.core.network.vending.SelectVendingMachineItemMessage;
import org.lovetropics.games.common.core.network.vending.ServerboundVendingMachinePurchasePacket;
import org.lovetropics.games.common.core.network.workspace.AddWorkspaceRegionMessage;
import org.lovetropics.games.common.core.network.workspace.SetWorkspaceMessage;
import org.lovetropics.games.common.core.network.workspace.UpdateWorkspaceRegionMessage;
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

		registrar.playToClient(ShowNotificationToastMessage.TYPE, ShowNotificationToastMessage.STREAM_CODEC);

		registrar.playToClient(SetGameClientStateMessage.TYPE, SetGameClientStateMessage.STREAM_CODEC);

		registrar.playToClient(SpectatorPlayerActivityMessage.TYPE, SpectatorPlayerActivityMessage.STREAM_CODEC);

		registrar.playToClient(FillFluidPacket.TYPE, FillFluidPacket.STREAM_CODEC);

		registrar.playToClient(ShowTriviaMessage.TYPE, ShowTriviaMessage.STREAM_CODEC);
		registrar.playToServer(SelectTriviaAnswerMessage.TYPE, SelectTriviaAnswerMessage.STREAM_CODEC, SelectTriviaAnswerMessage::handle);
		registrar.playToServer(RequestTriviaStateUpdateMessage.TYPE, RequestTriviaStateUpdateMessage.STREAM_CODEC, RequestTriviaStateUpdateMessage::handle);
		registrar.playToClient(TriviaAnswerResponseMessage.TYPE, TriviaAnswerResponseMessage.STREAM_CODEC);

		registrar.playToClient(SetForcedPoseMessage.TYPE, SetForcedPoseMessage.STREAM_CODEC);

		registrar.playToServer(SelectVendingMachineItemMessage.TYPE, SelectVendingMachineItemMessage.STREAM_CODEC, SelectVendingMachineItemMessage::handle);
		registrar.playToServer(ServerboundVendingMachinePurchasePacket.TYPE, ServerboundVendingMachinePurchasePacket.STREAM_CODEC, ServerboundVendingMachinePurchasePacket::handle);
		registrar.playToClient(ClientboundVendingMachineDropPacket.TYPE, ClientboundVendingMachineDropPacket.STREAM_CODEC);

		registrar.playToServer(ServerboundDdrInputPacket.TYPE, ServerboundDdrInputPacket.STREAM_CODEC, ServerboundDdrInputPacket::handle);
		registrar.playToServer(ServerboundSelectDdrLevelPacket.TYPE, ServerboundSelectDdrLevelPacket.STREAM_CODEC, ServerboundSelectDdrLevelPacket::handle);
		registrar.playToClient(ClientboundDdrInputHitPacket.TYPE, ClientboundDdrInputHitPacket.STREAM_CODEC);

		registrar.playToClient(ClientboundFadeToBlackPacket.TYPE, ClientboundFadeToBlackPacket.STREAM_CODEC);

		registrar.playToClient(ClientboundPlayerFaceDVDPackets.Add.TYPE, ClientboundPlayerFaceDVDPackets.Add.STREAM_CODEC);
		registrar.playToClient(ClientboundPlayerFaceDVDPackets.Clear.TYPE, ClientboundPlayerFaceDVDPackets.Clear.STREAM_CODEC);
	}

	@SubscribeEvent
	public static void registerClientHandler(RegisterClientPayloadHandlersEvent event) {
		event.register(SetWorkspaceMessage.TYPE, SetWorkspaceMessage::handle);
		event.register(AddWorkspaceRegionMessage.TYPE, AddWorkspaceRegionMessage::handle);
		event.register(UpdateWorkspaceRegionMessage.TYPE, UpdateWorkspaceRegionMessage::handleClientbound);

		event.register(ShowNotificationToastMessage.TYPE, ShowNotificationToastMessage::handle);

		event.register(SetGameClientStateMessage.TYPE, SetGameClientStateMessage::handle);

		event.register(SpectatorPlayerActivityMessage.TYPE, SpectatorPlayerActivityMessage::handle);

		event.register(FillFluidPacket.TYPE, FillFluidPacket::handle);

		event.register(ShowTriviaMessage.TYPE, ShowTriviaMessage::handle);
		event.register(TriviaAnswerResponseMessage.TYPE, TriviaAnswerResponseMessage::handle);

		event.register(SetForcedPoseMessage.TYPE, SetForcedPoseMessage::handle);

		event.register(ClientboundDdrInputHitPacket.TYPE, ClientboundDdrInputHitPacket::handle);
		event.register(ClientboundVendingMachineDropPacket.TYPE, ClientboundVendingMachineDropPacket::handle);

		event.register(ClientboundFadeToBlackPacket.TYPE, ClientFadeToBlack::handle);

		event.register(ClientboundPlayerFaceDVDPackets.Add.TYPE, ClientboundPlayerFaceDVDPackets.Add::handle);
		event.register(ClientboundPlayerFaceDVDPackets.Clear.TYPE, ClientboundPlayerFaceDVDPackets.Clear::handle);
	}
}
