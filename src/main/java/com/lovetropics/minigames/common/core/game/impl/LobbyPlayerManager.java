package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.LobbyRegistrations;
import com.lovetropics.minigames.common.core.game.lobby.IGameLobbyPlayers;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerRoleSelections;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.lovetropics.minigames.common.core.game.util.TeamAllocator;
import com.lovetropics.minigames.common.role.StreamHosts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class LobbyPlayerManager implements IGameLobbyPlayers {
	private final GameLobby lobby;
	private final LobbyRegistrations registrations;
	private final PlayerRoleSelections roleSelections;

	LobbyPlayerManager(GameLobby lobby) {
		this.lobby = lobby;
		registrations = new LobbyRegistrations(lobby.getServer());
		roleSelections = new PlayerRoleSelections(lobby.getMetadata().id());
	}

	@Override
	public TeamAllocator<PlayerRole, ServerPlayer> createRoleAllocator() {
		TeamAllocator<PlayerRole, ServerPlayer> allocator = registrations.createAllocator();
		for (ServerPlayer player : registrations) {
			if (allocator.hasPreference(player)) {
				continue;
			}

			PlayerRole role = roleSelections.getSelectedRoleFor(player.getUUID());
			if (StreamHosts.isHost(player) || role != PlayerRole.PARTICIPANT) {
				allocator.addPlayer(player, role);
			}
		}

		return allocator;
	}

	@Override
	public CompletableFuture<GameResult<Unit>> joinAndPrompt(ServerPlayer player) {
		if (isAlreadyInLobby(player)) {
			return CompletableFuture.completedFuture(GameResult.error(GameTexts.Commands.ALREADY_IN_LOBBY));
		}
		CompletableFuture<PlayerRole> future = roleSelections.prompt(player);
		return future.thenApplyAsync(role -> doJoin(player), lobby.getServer());
	}

	@Override
	public GameResult<Unit> join(ServerPlayer player, PlayerRole role) {
		if (isAlreadyInLobby(player)) {
			return GameResult.error(GameTexts.Commands.ALREADY_IN_LOBBY);
		}
		roleSelections.setRole(player, role);
		registrations.forceRole(player.getUUID(), role);
		return doJoin(player);
	}

	private GameResult<Unit> doJoin(ServerPlayer player) {
		if (registrations.add(player.getUUID())) {
			LoveTropics.LOGGER.debug("Player '{}' joining minigame lobby (host={})", player.getScoreboardName(), StreamHosts.isHost(player));
			lobby.onPlayerRegister(player);
			return GameResult.ok();
		} else {
			return GameResult.error(GameTexts.Commands.ALREADY_IN_LOBBY);
		}
	}

	private boolean isAlreadyInLobby(ServerPlayer player) {
		return lobby.manager.getLobbyFor(player) != null || registrations.contains(player.getUUID());
	}

	@Override
	public boolean remove(ServerPlayer player, boolean loggingOut) {
		if (registrations.remove(player.getUUID())) {
			lobby.onPlayerLeave(player, loggingOut);
			roleSelections.remove(player);
			return true;
		}
		return false;
	}

	public ServerPlayer logOut(ServerPlayer player) {
		if (registrations.remove(player.getUUID())) {
			player = lobby.onPlayerLeave(player, true);
			roleSelections.remove(player);
		}
		return player;
	}

	@Override
	public boolean forceRole(ServerPlayer player, @Nullable PlayerRole role) {
		return registrations.forceRole(player.getUUID(), role);
	}

	@Override
	public PlayerRoleSelections getRoleSelections() {
		return roleSelections;
	}

	@Override
	public boolean contains(UUID id) {
		return registrations.contains(id);
	}

	@Nullable
	@Override
	public ServerPlayer getPlayerBy(UUID id) {
		return registrations.getPlayerBy(id);
	}

	@Override
	public int size() {
		return registrations.size();
	}

	@Override
	public Iterator<ServerPlayer> iterator() {
		return registrations.iterator();
	}
}
