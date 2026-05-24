package com.lovetropics.minigames.client.lobby.state.message;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.lobby.state.ClientCurrentGame;
import com.lovetropics.minigames.client.lobby.state.ClientLobbyManager;
import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record LobbyUpdateMessage(int id, Optional<Update> update) implements CustomPacketPayload {
	public static final Type<LobbyUpdateMessage> TYPE = new Type<>(LoveTropics.id("lobby_update"));

	public static final StreamCodec<RegistryFriendlyByteBuf, LobbyUpdateMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, LobbyUpdateMessage::id,
			Update.STREAM_CODEC.apply(ByteBufCodecs::optional), LobbyUpdateMessage::update,
			LobbyUpdateMessage::new
	);

	public static LobbyUpdateMessage update(GameLobby lobby) {
		int id = lobby.getMetadata().id().networkId();
		String name = lobby.getMetadata().name();
		ClientCurrentGame currentGame = lobby.getClientCurrentGame();
		return new LobbyUpdateMessage(id, Optional.of(new Update(name, Optional.ofNullable(currentGame))));
	}

	public static LobbyUpdateMessage remove(GameLobby lobby) {
		return new LobbyUpdateMessage(lobby.getMetadata().id().networkId(), Optional.empty());
	}

	public static void handle(LobbyUpdateMessage message, IPayloadContext context) {
		if (message.update.isPresent()) {
			ClientLobbyManager.addOrUpdate(message.id, message.update.get().name, message.update.get().currentGame.orElse(null));
		} else {
			ClientLobbyManager.remove(message.id);
		}
	}

	@Override
	public Type<LobbyUpdateMessage> type() {
		return TYPE;
	}

	private record Update(String name, Optional<ClientCurrentGame> currentGame) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Update> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(200), Update::name,
				ClientCurrentGame.STREAM_CODEC.apply(ByteBufCodecs::optional), Update::currentGame,
				Update::new
		);
	}
}
