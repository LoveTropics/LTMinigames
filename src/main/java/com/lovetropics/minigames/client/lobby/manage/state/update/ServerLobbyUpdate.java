package com.lovetropics.minigames.client.lobby.manage.state.update;

import com.lovetropics.minigames.client.lobby.manage.ServerManageLobbyMessage;
import com.lovetropics.minigames.client.lobby.state.ClientGameDefinition;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.lovetropics.minigames.common.core.game.impl.LobbyManagement;
import com.lovetropics.minigames.common.core.game.lobby.LobbyControls;
import com.lovetropics.minigames.common.core.game.lobby.LobbyVisibility;
import com.lovetropics.minigames.common.util.PartialUpdate;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.resources.Identifier;

public abstract class ServerLobbyUpdate extends PartialUpdate<LobbyManagement> {
	public static final class Set extends AbstractSet<LobbyManagement> {
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

		public Set enqueue(ClientGameDefinition definition) {
			add(new Enqueue(definition.id()));
			return this;
		}

		public Set removeQueuedGame(int id) {
			add(new RemoveQueuedGame(id));
			return this;
		}

		public Set reorderQueuedGame(int id, int newIndex) {
			add(new ReorderQueuedGame(id, newIndex));
			return this;
		}

		public Set selectControl(LobbyControls.Type control) {
			add(new SelectControl(control));
			return this;
		}

		public Set setVisibility(LobbyVisibility visibility) {
			add(new SetVisibility(visibility));
			return this;
		}

		public Set close() {
			add(new Close());
			return this;
		}

		public ServerManageLobbyMessage intoMessage(int id) {
			return ServerManageLobbyMessage.update(id, this);
		}
	}

	public enum Type implements AbstractType<LobbyManagement> {
		SET_NAME(SetName::decode),
		ENQUEUE(Enqueue::decode),
		REMOVE_QUEUED_GAME(RemoveQueuedGame::decode),
		REORDER_QUEUED_GAME(ReorderQueuedGame::decode),
		SELECT_CONTROL(SelectControl::decode),
		SET_VISIBILITY(SetVisibility::decode),
		CLOSE(Close::decode),
		;

		private final StreamDecoder<RegistryFriendlyByteBuf, ServerLobbyUpdate> decode;

		Type(StreamDecoder<RegistryFriendlyByteBuf, ServerLobbyUpdate> decode) {
			this.decode = decode;
		}

		@Override
		public ServerLobbyUpdate decode(RegistryFriendlyByteBuf buffer) {
			return decode.decode(buffer);
		}
	}

	protected ServerLobbyUpdate(Type type) {
		super(type);
	}

	public static final class SetName extends ServerLobbyUpdate {
		private final String name;

		SetName(String name) {
			super(Type.SET_NAME);
			this.name = name;
		}

		@Override
		public void applyTo(LobbyManagement lobby) {
			lobby.setName(name);
		}

		@Override
		protected void encode(RegistryFriendlyByteBuf buffer) {
			buffer.writeUtf(name, 200);
		}

		static SetName decode(FriendlyByteBuf buffer) {
			return new SetName(buffer.readUtf(200));
		}
	}

	public static final class Enqueue extends ServerLobbyUpdate {
		private final Identifier definition;

		Enqueue(Identifier definition) {
			super(Type.ENQUEUE);
			this.definition = definition;
		}

		@Override
		public void applyTo(LobbyManagement lobby) {
			GameConfig config = GameConfigs.REGISTRY.get(definition);
			if (config != null) {
				lobby.enqueueGame(config);
			}
		}

		@Override
		protected void encode(RegistryFriendlyByteBuf buffer) {
			buffer.writeIdentifier(definition);
		}

		static Enqueue decode(FriendlyByteBuf buffer) {
			return new Enqueue(buffer.readIdentifier());
		}
	}

	public static final class RemoveQueuedGame extends ServerLobbyUpdate {
		private final int id;

		RemoveQueuedGame(int id) {
			super(Type.REMOVE_QUEUED_GAME);
			this.id = id;
		}

		@Override
		public void applyTo(LobbyManagement lobby) {
			lobby.removeQueuedGame(id);
		}

		@Override
		protected void encode(RegistryFriendlyByteBuf buffer) {
			buffer.writeVarInt(id);
		}

		static RemoveQueuedGame decode(FriendlyByteBuf buffer) {
			return new RemoveQueuedGame(buffer.readVarInt());
		}
	}

	public static final class ReorderQueuedGame extends ServerLobbyUpdate {
		private final int id;
		private final int newIndex;

		ReorderQueuedGame(int id, int newIndex) {
			super(Type.REORDER_QUEUED_GAME);
			this.id = id;
			this.newIndex = newIndex;
		}

		@Override
		public void applyTo(LobbyManagement lobby) {
			lobby.reorderQueuedGame(id, newIndex);
		}

		@Override
		protected void encode(RegistryFriendlyByteBuf buffer) {
			buffer.writeVarInt(id);
			buffer.writeVarInt(newIndex);
		}

		static ReorderQueuedGame decode(FriendlyByteBuf buffer) {
			return new ReorderQueuedGame(buffer.readVarInt(), buffer.readVarInt());
		}
	}

	public static final class SelectControl extends ServerLobbyUpdate {
		private final LobbyControls.Type control;

		SelectControl(LobbyControls.Type control) {
			super(Type.SELECT_CONTROL);
			this.control = control;
		}

		@Override
		public void applyTo(LobbyManagement lobby) {
			lobby.selectControl(control);
		}

		@Override
		protected void encode(RegistryFriendlyByteBuf buffer) {
			buffer.writeByte(control.ordinal() & 0xFF);
		}

		static SelectControl decode(FriendlyByteBuf buffer) {
			LobbyControls.Type[] types = LobbyControls.Type.values();
			LobbyControls.Type control = types[buffer.readUnsignedByte() % types.length];
			return new SelectControl(control);
		}
	}

	public static final class SetVisibility extends ServerLobbyUpdate {
		private final LobbyVisibility visibility;

		public SetVisibility(LobbyVisibility visibility) {
			super(Type.SET_VISIBILITY);
			this.visibility = visibility;
		}

		@Override
		public void applyTo(LobbyManagement lobby) {
			lobby.setVisibility(visibility);
		}

		@Override
		protected void encode(RegistryFriendlyByteBuf buffer) {
			buffer.writeEnum(visibility);
		}

		static SetVisibility decode(FriendlyByteBuf buffer) {
			return new SetVisibility(buffer.readEnum(LobbyVisibility.class));
		}
	}

	public static final class Close extends ServerLobbyUpdate {
		public Close() {
			super(Type.CLOSE);
		}

		@Override
		public void applyTo(LobbyManagement lobby) {
			lobby.close();
		}

		@Override
		protected void encode(RegistryFriendlyByteBuf buffer) {
		}

		static Close decode(FriendlyByteBuf buffer) {
			return new Close();
		}
	}
}
