package com.lovetropics.minigames.client.game.handler;

import com.lovetropics.minigames.client.game.handler.spectate.ClientSpectatingManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public final class ClientGameStateHandlers {
	private static final Map<Identifier, ClientGameStateHandler<?>> REGISTRY = new Object2ObjectOpenHashMap<>();

	static {
		register(GameClientStateTypes.SPECTATING, ClientSpectatingManager.INSTANCE);
		register(GameClientStateTypes.RESOURCE_PACK, GameResourcePackHandler.INSTANCE);
		register(GameClientStateTypes.CRAFTING_BEE_CRAFTS, GameCraftingBeeHandler.HANDLER);
		register(GameClientStateTypes.BINGO_BOARD, GameBingoHandler.HANDLER);
	}

	public static <T extends GameClientState> void register(DeferredHolder<GameClientStateType<?>, GameClientStateType<T>> type, ClientGameStateHandler<T> handler) {
		REGISTRY.put(type.getId(), handler);
	}

	public static <T extends GameClientState> void acceptState(T state) {
		ClientGameStateHandler<T> handler = ClientGameStateHandlers.get(state);
		if (handler != null) {
			handler.accept(state);
		}
	}

	public static <T extends GameClientState> void disableState(T state) {
		ClientGameStateHandler<T> handler = ClientGameStateHandlers.get(state);
		if (handler != null) {
			handler.disable(state);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends GameClientState> @Nullable ClientGameStateHandler<T> get(T state) {
		Identifier id = GameClientStateTypes.REGISTRY.getKey(state.getType());
		if (id != null) {
			return (ClientGameStateHandler<T>) REGISTRY.get(id);
		} else {
			return null;
		}
	}
}
