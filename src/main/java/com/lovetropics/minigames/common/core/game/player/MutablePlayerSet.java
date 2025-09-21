package com.lovetropics.minigames.common.core.game.player;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public final class MutablePlayerSet implements PlayerSet {
	private final MinecraftServer server;
	private final Set<UUID> players = new ObjectOpenHashSet<>();

	public MutablePlayerSet(MinecraftServer server) {
		this.server = server;
	}

	public void clear() {
		players.clear();
	}

	public boolean add(ServerPlayer player) {
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

	@Nullable
	@Override
	public ServerPlayer getPlayerBy(UUID id) {
		return players.contains(id) ? server.getPlayerList().getPlayer(id) : null;
	}

	@Override
	public int size() {
		return players.size();
	}

	@Override
	public Iterator<ServerPlayer> iterator() {
		return PlayerIterable.resolvingIterator(server, players.iterator());
	}
}
