package com.lovetropics.minigames.common.core.game;

import com.google.common.collect.Lists;
import com.lovetropics.minigames.common.core.game.player.PlayerIterable;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.util.TeamAllocator;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LobbyRegistrations implements PlayerSet {
	private final MinecraftServer server;

	private final Set<UUID> players = new ObjectOpenHashSet<>();
	private final Map<UUID, PlayerRole> forcedRoles = new Object2ObjectOpenHashMap<>();

	public LobbyRegistrations(MinecraftServer server) {
		this.server = server;
	}

	public void clear() {
		players.clear();
		forcedRoles.clear();
	}

	public TeamAllocator<PlayerRole, PlayerKey> createAllocator() {
		TeamAllocator<PlayerRole, PlayerKey> allocator = new TeamAllocator<>(Lists.newArrayList(PlayerRole.ROLES));
		allocator.setOverflowTeam(PlayerRole.SPECTATOR);

		for (ServerPlayer player : this) {
			PlayerRole role = forcedRoles.get(player.getUUID());
			allocator.addPlayer(PlayerKey.from(player), role);
		}

		return allocator;
	}

	public boolean add(UUID id) {
		return players.add(id);
	}

	public boolean remove(UUID id) {
		if (players.remove(id)) {
			forcedRoles.remove(id);
			return true;
		} else {
			return false;
		}
	}

	public boolean forceRole(UUID id, @Nullable PlayerRole role) {
		if (players.contains(id)) {
			if (role != null) {
				return forcedRoles.put(id, role) != role;
			} else {
				return forcedRoles.remove(id) != null;
			}
		}
		return false;
	}

	@Override
	public boolean contains(UUID id) {
		return players.contains(id);
	}

	@Override
	public int size() {
		return players.size();
	}

	public @Nullable PlayerRole getForcedRoleFor(UUID id) {
		return forcedRoles.get(id);
	}

	@Override
	public @Nullable ServerPlayer getPlayerBy(UUID id) {
		return contains(id) ? server.getPlayerList().getPlayer(id) : null;
	}

	@Override
	public Iterator<ServerPlayer> iterator() {
		return PlayerIterable.resolvingIterator(server, players.iterator());
	}
}
