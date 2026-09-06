package com.lovetropics.minigames.common.core.game.client_state;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.PlayerIsolation;
import com.lovetropics.minigames.common.core.network.SetGameClientStateMessage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = LoveTropics.ID)
public final class GameClientStateSender {
	private static final GameClientStateSender INSTANCE = new GameClientStateSender();

	private final Map<UUID, PlayerEntry> players = new Object2ObjectOpenHashMap<>();

	public static GameClientStateSender get() {
		return INSTANCE;
	}

	public PlayerEntry byPlayer(ServerPlayer player) {
		return players.computeIfAbsent(player.getUUID(), id -> new PlayerEntry());
	}

	// TODO: It's very strange to expose client states on the server. Can we improve on this system?
	public static <T extends GameClientState> @Nullable T getOrNull(ServerPlayer player, GameClientStateType<T> type) {
		return get().byPlayer(player).getOrNull(type);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			INSTANCE.byPlayer(player).tick(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof final ServerPlayer player && !PlayerIsolation.INSTANCE.isReloading(player)) {
			INSTANCE.players.remove(player.getUUID());
		}
	}

	public static final class PlayerEntry {
		private final Map<GameClientStateType<?>, GameClientState> values = new Object2ObjectOpenHashMap<>();
		private final Set<GameClientStateType<?>> changedValues = new ObjectOpenHashSet<>();

		public <T extends GameClientState> void enqueueSet(T state) {
			values.put(state.getType(), state);
			changedValues.add(state.getType());
		}

		public <T extends GameClientState> void enqueueRemove(GameClientStateType<T> type) {
			values.remove(type);
			changedValues.add(type);
		}

		@SuppressWarnings("unchecked")
		public <T extends GameClientState> @Nullable T getOrNull(GameClientStateType<T> type) {
			return (T) values.get(type);
		}

		void tick(ServerPlayer player) {
			if (!changedValues.isEmpty()) {
				for (GameClientStateType<?> type : changedValues) {
					GameClientState value = values.get(type);
					if (value != null) {
						sendSet(value, player);
					} else {
						sendRemove(type, player);
					}
				}
				changedValues.clear();
			}
		}

		void sendSet(GameClientState state, ServerPlayer player) {
			PacketDistributor.sendToPlayer(player, SetGameClientStateMessage.set(state));
		}

		void sendRemove(GameClientStateType<?> type, ServerPlayer player) {
			PacketDistributor.sendToPlayer(player, SetGameClientStateMessage.remove(type));
		}
	}
}
