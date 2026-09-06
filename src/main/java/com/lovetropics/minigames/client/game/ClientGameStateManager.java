package com.lovetropics.minigames.client.game;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.handler.ClientGameStateHandlers;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateMap;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class ClientGameStateManager {
	private static @Nullable GameClientStateMap map;

	public static <T extends GameClientState> void set(T state) {
		GameClientStateMap map = ClientGameStateManager.map;
		if (map == null) {
			ClientGameStateManager.map = map = new GameClientStateMap();
		}

		ClientGameStateHandlers.acceptState(state);
		map.add(state);
	}

	public static <T extends GameClientState> void remove(GameClientStateType<T> type) {
		GameClientStateMap map = ClientGameStateManager.map;
		if (map == null) {
			return;
		}

		T state = map.remove(type);
		if (state != null) {
			ClientGameStateHandlers.disableState(state);

			if (map.isEmpty()) {
				ClientGameStateManager.map = null;
			}
		}
	}

	public static <T extends GameClientState> @Nullable T getOrNull(Supplier<GameClientStateType<T>> type) {
		GameClientStateMap map = ClientGameStateManager.map;
		if (map != null) {
			return map.getOrNull(type.get());
		}
		return null;
	}

	public static <T extends GameClientState> T getOrDefault(Supplier<GameClientStateType<T>> type, T defaultValue) {
		return Objects.requireNonNullElse(getOrNull(type), defaultValue);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
		ClientGameStateManager.clearState();
	}

	private static void clearState() {
		GameClientStateMap map = ClientGameStateManager.map;
		if (map != null) {
			for (GameClientState state : map) {
				ClientGameStateHandlers.disableState(state);
			}
		}

		ClientGameStateManager.map = null;
	}
}
