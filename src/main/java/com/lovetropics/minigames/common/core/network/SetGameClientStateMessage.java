package com.lovetropics.minigames.common.core.network;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;
import java.util.function.Function;

public record SetGameClientStateMessage(GameClientStateType<?> stateType, Optional<GameClientState> state) implements CustomPacketPayload {
	public static final Type<SetGameClientStateMessage> TYPE = new Type<>(LoveTropics.id("set_game_client_state"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SetGameClientStateMessage> STREAM_CODEC = new StreamCodec<>() {
		private final StreamCodec<RegistryFriendlyByteBuf, GameClientStateType<?>> typeCodec = ByteBufCodecs.registry(GameClientStateTypes.REGISTRY_KEY);

		@Override
		public SetGameClientStateMessage decode(RegistryFriendlyByteBuf input) {
			GameClientStateType<?> type = typeCodec.decode(input);
			return new SetGameClientStateMessage(type, valueCodec(type).decode(input).map(Function.identity()));
		}

		@Override
		public void encode(RegistryFriendlyByteBuf output, SetGameClientStateMessage message) {
			typeCodec.encode(output, message.stateType);
			encodeUnchecked(output, message.stateType, message.state);
		}

		@SuppressWarnings("unchecked")
		private static <T extends GameClientState> void encodeUnchecked(RegistryFriendlyByteBuf output, GameClientStateType<T> type, Optional<GameClientState> state) {
			valueCodec(type).encode(output, state.map(s -> (T) s));
		}

		private static <T extends GameClientState> StreamCodec<RegistryFriendlyByteBuf, Optional<T>> valueCodec(GameClientStateType<T> type) {
			return type.streamCodec().<RegistryFriendlyByteBuf>cast().apply(ByteBufCodecs::optional);
		}
	};

	public static <T extends GameClientState> SetGameClientStateMessage set(T state) {
		return new SetGameClientStateMessage(state.getType(), Optional.of(state));
	}

	public static <T extends GameClientState> SetGameClientStateMessage remove(GameClientStateType<T> type) {
		return new SetGameClientStateMessage(type, Optional.empty());
	}

	public static void handle(SetGameClientStateMessage message, IPayloadContext context) {
		if (message.state.isPresent()) {
			ClientGameStateManager.set(message.state.get());
		} else {
			ClientGameStateManager.remove(message.stateType);
		}
	}

	@Override
	public Type<SetGameClientStateMessage> type() {
		return TYPE;
	}
}
