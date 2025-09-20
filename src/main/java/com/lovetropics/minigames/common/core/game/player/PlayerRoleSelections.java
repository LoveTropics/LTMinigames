package com.lovetropics.minigames.common.core.game.player;

import com.lovetropics.minigames.client.lobby.select_role.SelectRolePromptMessage;
import com.lovetropics.minigames.common.core.game.lobby.GameLobbyId;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class PlayerRoleSelections {
	private final GameLobbyId lobbyId;

	private final Map<UUID, PlayerRole> roles = new Object2ObjectOpenHashMap<>();
	private final Map<UUID, CompletableFuture<PlayerRole>> pendingResponses = new Object2ObjectOpenHashMap<>();
	private final Map<UUID, Consumer<PlayerRole>> responseHandlers = new Object2ObjectOpenHashMap<>();

	public PlayerRoleSelections(GameLobbyId lobbyId) {
		this.lobbyId = lobbyId;
	}

	public void clear() {
		roles.clear();
		pendingResponses.clear();
	}

	public void clearAndPromptAll(PlayerSet players) {
		clear();
		for (ServerPlayer player : players) {
			prompt(player);
		}
	}

	public CompletableFuture<PlayerRole> prompt(ServerPlayer player) {
		CompletableFuture<PlayerRole> future = pendingResponses.get(player.getUUID());
		if (future == null) {
			final CompletableFuture<PlayerRole> rootFuture = new CompletableFuture<>();
			responseHandlers.put(player.getUUID(), rootFuture::complete);
			future = rootFuture.thenApplyAsync(role -> {
				roles.put(player.getUUID(), role);
				return role;
			}, player.getServer());
			pendingResponses.put(player.getUUID(), future);
		}
		sendPromptTo(player);
		return future;
	}

	private void sendPromptTo(ServerPlayer player) {
		PacketDistributor.sendToPlayer(player, new SelectRolePromptMessage(lobbyId.networkId()));
		player.playNotifySound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.MASTER, 1.0F, 1.0F);
	}

	public void remove(ServerPlayer player) {
		roles.remove(player.getUUID());
	}

	public void acceptResponse(ServerPlayer player, PlayerRole role) {
		Consumer<PlayerRole> handler = responseHandlers.remove(player.getUUID());
		if (handler != null) {
			handler.accept(role);
			pendingResponses.remove(player.getUUID());
		}
	}

	public void setRole(ServerPlayer player, PlayerRole role) {
		roles.put(player.getUUID(), role);
	}

	@Nonnull
	public PlayerRole getSelectedRoleFor(UUID playerId) {
		return roles.getOrDefault(playerId, PlayerRole.SPECTATOR);
	}

	public boolean hasPending() {
		return pendingResponses.isEmpty();
	}

	@Override
	public String toString() {
		return roles.toString();
	}
}
