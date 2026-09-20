package org.lovetropics.games.common.core.game.player;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public final class MutablePlayerSet implements PlayerSet {
	private @Nullable MinecraftServer server;
	private final Set<UUID> players = new ObjectOpenHashSet<>();

	public void clear() {
		players.clear();
	}

	public boolean add(ServerPlayer player) {
		if (server == null) {
			server = player.level().getServer();
		}
		return players.add(player.getUUID());
	}

	public boolean remove(UUID id) {
		return players.remove(id);
	}

	public boolean remove(Entity entity) {
		return remove(entity.getUUID());
	}

	@Override
	public boolean contains(UUID id) {
		return players.contains(id);
	}

	@Override
	public @Nullable ServerPlayer getPlayerBy(UUID id) {
		if (server == null) {
			return null;
		}
		return players.contains(id) ? server.getPlayerList().getPlayer(id) : null;
	}

	@Override
	public int size() {
		return players.size();
	}

	@Override
	public Iterator<ServerPlayer> iterator() {
		if (server == null) {
			return Collections.emptyIterator();
		}
		return PlayerIterable.resolvingIterator(server, players.iterator());
	}
}
