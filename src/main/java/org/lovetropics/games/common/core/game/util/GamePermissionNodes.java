package org.lovetropics.games.common.core.game.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContext;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionType;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.IGameState;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class GamePermissionNodes implements IGameState {
	public static final GameStateKey.Defaulted<GamePermissionNodes> KEY = GameStateKey.create("Game Permissions", GamePermissionNodes::new);

	private final Map<PermissionNode<?>, Check<?>> registeredChecks = new ConcurrentHashMap<>();

	private GamePermissionNodes() {
	}

	/// Note: if this permission is checked from off-thread, will block on the server thread.
	/// If latency is critical, use [#registerAnyThread(String, PermissionType , Check)] and handle thread-safety.
	public <T> void registerMainThreadOnly(String nodeId, PermissionType<T> type, Check<T> check) {
		registerAnyThread(nodeId, type, (player, node, context) -> {
			MinecraftServer server = player.level().getServer();
			if (server.isSameThread()) {
				return check.check(player, node, context);
			} else {
				return CompletableFuture.supplyAsync(() -> check.check(player, node, context), server).join();
			}
		});
	}

	public <T> void registerAnyThread(String nodeId, PermissionType<T> type, Check<T> check) {
		PermissionAPI.getRegisteredNodes().stream()
				.filter(node -> node.getType() == type && node.getNodeName().equals(nodeId))
				.forEach(node -> register(node, check));
	}

	private <T> void register(PermissionNode<?> node, Check<T> check) {
		registeredChecks.compute(node, (_, oldCheck) -> {
			if (oldCheck == null) {
				return check;
			}
			//noinspection unchecked
			return Check.compose(check, (Check<T>) oldCheck);
		});
	}

	@SuppressWarnings("unchecked")
	public <T> @Nullable T check(ServerPlayer player, PermissionNode<T> node, PermissionDynamicContext<?>... context) {
		Check<T> check = (Check<T>) registeredChecks.get(node);
		return check != null ? check.check(player, node, context) : null;
	}

	@FunctionalInterface
	public interface Check<T> {
		static <T> Check<T> compose(Check<T> first, Check<T> second) {
			return (player, node, context) -> {
				T value = first.check(player, node, context);
				if (value != null) {
					return value;
				}
				return second.check(player, node, context);
			};
		}

		@Nullable T check(ServerPlayer player, PermissionNode<T> node, PermissionDynamicContext<?>... context);
	}
}
