package com.lovetropics.minigames.common.core.game.player;

import com.google.common.collect.Iterators;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

public interface PlayerSet extends PlayerIterable {
	PlayerSet EMPTY = new PlayerSet() {
		@Override
		public boolean contains(UUID id) {
			return false;
		}

		@Nullable
		@Override
		public ServerPlayer getPlayerBy(UUID id) {
			return null;
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public boolean isEmpty() {
			return true;
		}

		@Override
		public Iterator<ServerPlayer> iterator() {
			return Collections.emptyIterator();
		}
	};

	static PlayerSet ofServer(MinecraftServer server) {
		return of(server.getPlayerList());
	}

	static PlayerSet of(PlayerList players) {
		return new PlayerSet() {
			@Override
			public boolean contains(UUID id) {
				return players.getPlayer(id) != null;
			}

			@Nullable
			@Override
			public ServerPlayer getPlayerBy(UUID id) {
				return players.getPlayer(id);
			}

			@Override
			public int size() {
				return players.getPlayerCount();
			}

			@Override
			public Iterator<ServerPlayer> iterator() {
				return players.getPlayers().iterator();
			}
		};
	}

	static PlayerSet wrap(MinecraftServer server, Collection<UUID> players) {
		return new PlayerSet() {
			@Override
			public boolean contains(UUID id) {
				return players.contains(id);
			}

			@Nullable
			@Override
			public ServerPlayer getPlayerBy(UUID id) {
				return contains(id) ? server.getPlayerList().getPlayer(id) : null;
			}

			@Override
			public int size() {
				return players.size();
			}

			@Override
			public Iterator<ServerPlayer> iterator() {
				return PlayerIterable.resolvingIterator(server, players.iterator());
			}
		};
	}

	static PlayerSet of(ServerPlayer player) {
		return new PlayerSet() {
			@Override
			public boolean contains(UUID id) {
				return id.equals(player.getUUID());
			}

			@Override
			@Nullable
			public ServerPlayer getPlayerBy(UUID id) {
				return contains(id) ? player : null;
			}

			@Override
			public int size() {
				return 1;
			}

			@Override
			public Iterator<ServerPlayer> iterator() {
				return Collections.singletonList(player).iterator();
			}
		};
	}

	static PlayerSet intersection(PlayerSet left, PlayerSet right) {
		return new PlayerSet() {
			@Override
			public boolean contains(UUID id) {
				return left.contains(id) && right.contains(id);
			}

			@Nullable
			@Override
			public ServerPlayer getPlayerBy(UUID id) {
				ServerPlayer leftPlayer = left.getPlayerBy(id);
				ServerPlayer rightPlayer = right.getPlayerBy(id);
				if (leftPlayer != null && rightPlayer != null) {
					return leftPlayer;
				}
				return null;
			}

			@Override
			public int size() {
				return Iterators.size(iterator());
			}

			@Override
			public Iterator<ServerPlayer> iterator() {
				return Iterators.filter(left.iterator(), right::contains);
			}
		};
	}

	static PlayerSet difference(PlayerSet first, PlayerSet second) {
		return new PlayerSet() {
			@Override
			public boolean contains(UUID id) {
				return first.contains(id) && !second.contains(id);
			}

			@Override
			@Nullable
			public ServerPlayer getPlayerBy(UUID id) {
				return !second.contains(id) ? first.getPlayerBy(id) : null;
			}

			@Override
			public int size() {
				return Iterators.size(iterator());
			}

			@Override
			public Iterator<ServerPlayer> iterator() {
				return Iterators.filter(first.iterator(), player -> !second.contains(player));
			}
		};
	}

	default boolean contains(Entity entity) {
		return contains(entity.getUUID());
	}

	boolean contains(UUID id);

	@Nullable
	ServerPlayer getPlayerBy(UUID id);

	@Nullable
	default ServerPlayer getPlayerBy(PlayerKey key) {
		return getPlayerBy(key.id());
	}

	int size();

	default boolean isEmpty() {
		return size() == 0;
	}

	@Override
	Iterator<ServerPlayer> iterator();
}
