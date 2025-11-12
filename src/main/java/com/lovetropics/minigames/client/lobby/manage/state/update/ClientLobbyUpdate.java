package com.lovetropics.minigames.client.lobby.manage.state.update;

import com.lovetropics.minigames.client.lobby.manage.ClientLobbyManagement;
import com.lovetropics.minigames.client.lobby.manage.ClientManageLobbyMessage;
import com.lovetropics.minigames.client.lobby.manage.state.ClientLobbyPlayer;
import com.lovetropics.minigames.client.lobby.manage.state.ClientLobbyQueue;
import com.lovetropics.minigames.client.lobby.manage.state.ClientLobbyQueuedGame;
import com.lovetropics.minigames.client.lobby.state.ClientCurrentGame;
import com.lovetropics.minigames.client.lobby.state.ClientGameDefinition;
import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import com.lovetropics.minigames.common.core.game.impl.LobbyGameQueue;
import com.lovetropics.minigames.common.core.game.lobby.LobbyControls;
import com.lovetropics.minigames.common.core.game.lobby.LobbyVisibility;
import com.lovetropics.minigames.common.core.game.lobby.QueuedGame;
import com.lovetropics.minigames.common.util.PartialUpdate;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

public abstract class ClientLobbyUpdate extends PartialUpdate<ClientLobbyManagement.Session> {
	public static final class Set extends AbstractSet<ClientLobbyManagement.Session> {
		public static final StreamCodec<RegistryFriendlyByteBuf, Set> STREAM_CODEC = createStreamCodec(Set::new);

		private Set() {
			super(Family.of(Type.values()));
		}

		public static Set create() {
			return new Set();
		}

		public Set setName(String name) {
			add(new SetName(name));
			return this;
		}

		public Set initialize(
				List<ClientGameDefinition> installedGames,
				LobbyGameQueue queue
		) {
			ClientLobbyQueue clientQueue = new ClientLobbyQueue();
			for (QueuedGame game : queue) {
				clientQueue.add(game.networkId(), ClientLobbyQueuedGame.from(game));
			}

			add(new Initialize(installedGames, clientQueue));
			return this;
		}

		public Set setPlayersFrom(GameLobby lobby) {
			List<ClientLobbyPlayer> players = lobby.getPlayers().stream()
					.map(player -> ClientLobbyPlayer.from(lobby, player))
					.collect(Collectors.toList());
			add(new SetPlayers(players));
			return this;
		}

		public Set setControlState(LobbyControls.State controlsState) {
			add(new SetControlsState(controlsState));
			return this;
		}

		public Set setCurrentGame(@Nullable ClientCurrentGame currentGame) {
			add(new SetCurrentGame(currentGame));
			return this;
		}

		public Set updateQueue(LobbyGameQueue queue, int... updatedIds) {
			IntList order = new IntArrayList(queue.size());
			Int2ObjectMap<ClientLobbyQueuedGame> updated = new Int2ObjectArrayMap<>();

			for (QueuedGame game : queue) {
				order.add(game.networkId());

				for (int id : updatedIds) {
					if (id == game.networkId()) {
						updated.put(id, ClientLobbyQueuedGame.from(game));
						break;
					}
				}
			}

			add(new UpdateQueue(order, updated));

			return this;
		}

		public Set setVisibility(LobbyVisibility visibility, boolean canFocusLive) {
			add(new SetVisibility(visibility, canFocusLive));
			return this;
		}

		public ClientManageLobbyMessage intoMessage(int id) {
			return new ClientManageLobbyMessage(id, this);
		}
	}

	public enum Type implements AbstractType<ClientLobbyManagement.Session> {
		INITIALIZE(Initialize::decode),
		SET_NAME(SetName::decode),
		SET_CURRENT_GAME(SetCurrentGame::decode),
		UPDATE_QUEUE(UpdateQueue::decode),
		SET_PLAYERS(SetPlayers::decode),
		SET_CONTROLS_STATE(SetControlsState::decode),
		SET_VISIBILITY(SetVisibility::decode);

		private final StreamDecoder<RegistryFriendlyByteBuf, ClientLobbyUpdate> decode;

		Type(StreamDecoder<RegistryFriendlyByteBuf, ClientLobbyUpdate> decode) {
			this.decode = decode;
		}

		@Override
		public ClientLobbyUpdate decode(RegistryFriendlyByteBuf buffer) {
			return decode.decode(buffer);
		}
	}

	protected ClientLobbyUpdate(Type type) {
		super(type);
	}

	public static final class Initialize extends ClientLobbyUpdate {
		private final List<ClientGameDefinition> installedGames;
		private final ClientLobbyQueue queue;

		Initialize(List<ClientGameDefinition> installedGames, ClientLobbyQueue queue) {
			super(Type.INITIALIZE);
			this.installedGames = installedGames;
			this.queue = queue;
		}

		@Override
		public void applyTo(ClientLobbyManagement.Session session) {
			session.handleInitialize(installedGames, queue);
		}

		@Override
		protected void encode(RegistryFriendlyByteBuf buffer) {
			buffer.writeVarInt(installedGames.size());
			for (ClientGameDefinition game : installedGames) {
				game.encode(buffer);
			}

			queue.encode(buffer);
		}

		static Initialize decode(RegistryFriendlyByteBuf buffer) {
			int installedSize = buffer.readVarInt();
			List<ClientGameDefinition> installedGames = new ArrayList<>(installedSize);
			for (int i = 0; i < installedSize; i++) {
				installedGames.add(ClientGameDefinition.decode(buffer));
			}

			ClientLobbyQueue queue = ClientLobbyQueue.decode(buffer);

			return new Initialize(installedGames, queue);
		}
	}

	public static final class SetName extends ClientLobbyUpdate {
		private final String name;

		SetName(String name) {
			super(Type.SET_NAME);
			this.name = name;
		}

		@Override
		public void applyTo(ClientLobbyManagement.Session session) {
			session.handleName(name);
		}

		@Override
		protected void encode(RegistryFriendlyByteBuf buffer) {
			buffer.writeUtf(name, 200);
		}

		static SetName decode(FriendlyByteBuf buffer) {
			return new SetName(buffer.readUtf(200));
		}
	}

	public static final class SetCurrentGame extends ClientLobbyUpdate {
		@Nullable
		private final ClientCurrentGame game;

		SetCurrentGame(@Nullable ClientCurrentGame game) {
			super(Type.SET_CURRENT_GAME);
			this.game = game;
		}

		@Override
		public void applyTo(ClientLobbyManagement.Session session) {
			session.handleCurrentGame(game);
		}

		@Override
		protected void encode(RegistryFriendlyByteBuf buffer) {
			buffer.writeBoolean(game != null);
			if (game != null) {
				ClientCurrentGame.STREAM_CODEC.encode(buffer, game);
			}
		}

		static SetCurrentGame decode(RegistryFriendlyByteBuf buffer) {
			ClientCurrentGame game = buffer.readBoolean() ? ClientCurrentGame.STREAM_CODEC.decode(buffer) : null;
			return new SetCurrentGame(game);
		}
	}

	public static final class UpdateQueue extends ClientLobbyUpdate {
		private final IntList queue;
		private final Int2ObjectMap<ClientLobbyQueuedGame> updated;

		UpdateQueue(IntList queue, Int2ObjectMap<ClientLobbyQueuedGame> updated) {
			super(Type.UPDATE_QUEUE);
			this.queue = queue;
			this.updated = updated;
		}

		@Override
		public void applyTo(ClientLobbyManagement.Session session) {
			session.handleQueueUpdate(queue, updated);
		}

		@Override
		protected void encode(RegistryFriendlyByteBuf buffer) {
			buffer.writeVarInt(queue.size());
			queue.forEach((IntConsumer) buffer::writeVarInt);

			buffer.writeVarInt(updated.size());
			updated.forEach((id, game) -> {
				buffer.writeVarInt(id);
				game.encode(buffer);
			});
		}

		static UpdateQueue decode(RegistryFriendlyByteBuf buffer) {
			int queueSize = buffer.readVarInt();
			IntList queue = new IntArrayList(queueSize);
			for (int i = 0; i < queueSize; i++) {
				queue.add(buffer.readVarInt());
			}

			int updatedSize = buffer.readVarInt();
			Int2ObjectMap<ClientLobbyQueuedGame> updated = new Int2ObjectArrayMap<>(updatedSize);
			for (int i = 0; i < updatedSize; i++) {
				int id = buffer.readVarInt();
				ClientLobbyQueuedGame game = ClientLobbyQueuedGame.decode(buffer);
				updated.put(id, game);
			}

			return new UpdateQueue(queue, updated);
		}
	}

	public static final class SetPlayers extends ClientLobbyUpdate {
		private final List<ClientLobbyPlayer> players;

		SetPlayers(List<ClientLobbyPlayer> players) {
			super(Type.SET_PLAYERS);
			this.players = players;
		}

		@Override
		public void applyTo(ClientLobbyManagement.Session session) {
			session.handlePlayers(players);
		}

		@Override
		protected void encode(RegistryFriendlyByteBuf buffer) {
			buffer.writeVarInt(players.size());
			for (ClientLobbyPlayer player : players) {
				player.encode(buffer);
			}
		}

		static SetPlayers decode(FriendlyByteBuf buffer) {
			int size = buffer.readVarInt();
			List<ClientLobbyPlayer> players = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				players.add(ClientLobbyPlayer.decode(buffer));
			}
			return new SetPlayers(players);
		}
	}

	public static final class SetControlsState extends ClientLobbyUpdate {
		private final LobbyControls.State state;

		SetControlsState(LobbyControls.State state) {
			super(Type.SET_CONTROLS_STATE);
			this.state = state;
		}

		@Override
		public void applyTo(ClientLobbyManagement.Session session) {
			session.handleControlsState(state);
		}

		@Override
		protected void encode(RegistryFriendlyByteBuf buffer) {
			state.encode(buffer);
		}

		static SetControlsState decode(FriendlyByteBuf buffer) {
			return new SetControlsState(LobbyControls.State.decode(buffer));
		}
	}

	public static final class SetVisibility extends ClientLobbyUpdate {
		private final LobbyVisibility visibility;
		private final boolean canFocusLive;

		public SetVisibility(LobbyVisibility visibility, boolean canFocusLive) {
			super(Type.SET_VISIBILITY);
			this.visibility = visibility;
			this.canFocusLive = canFocusLive;
		}

		@Override
		public void applyTo(ClientLobbyManagement.Session session) {
			session.handleVisibility(visibility, canFocusLive);
		}

		@Override
		protected void encode(RegistryFriendlyByteBuf buffer) {
			buffer.writeEnum(visibility);
			buffer.writeBoolean(canFocusLive);
		}

		static SetVisibility decode(FriendlyByteBuf buffer) {
			return new SetVisibility(buffer.readEnum(LobbyVisibility.class), buffer.readBoolean());
		}
	}
}
