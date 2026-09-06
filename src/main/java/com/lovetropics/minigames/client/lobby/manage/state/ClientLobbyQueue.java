package com.lovetropics.minigames.client.lobby.manage.state;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

public final class ClientLobbyQueue implements Iterable<ClientLobbyQueuedGame> {
	private final IntList queue = new IntArrayList();
	private final Int2ObjectMap<ClientLobbyQueuedGame> games = new Int2ObjectOpenHashMap<>();

	public void add(int id, ClientLobbyQueuedGame game) {
		queue.add(id);
		games.put(id, game);
	}

	public void update(int id, ClientLobbyQueuedGame game) {
		games.replace(id, game);
	}

	public void applyUpdates(IntList queue, Int2ObjectMap<ClientLobbyQueuedGame> updated) {
		games.keySet().removeIf((IntPredicate) id -> !queue.contains(id));
		games.putAll(updated);

		this.queue.clear();
		this.queue.addAll(queue);
	}

	public @Nullable ClientLobbyQueuedGame byId(int id) {
		return games.get(id);
	}

	private ClientLobbyQueuedGame byIdOrThrow(int key) {
		ClientLobbyQueuedGame game = games.get(key);
		return Objects.requireNonNull(game, "game in queue with missing definition");
	}

	public int indexById(int id) {
		return queue.indexOf(id);
	}

	public int size() {
		return queue.size();
	}

	public boolean isEmpty() {
		return queue.isEmpty();
	}

	@Override
	public Iterator<ClientLobbyQueuedGame> iterator() {
		return Iterators.transform(entries().iterator(), Entry::game);
	}

	public Iterable<Entry> entries() {
		return () -> {
			IntListIterator iterator = queue.iterator();
			return new AbstractIterator<>() {
				@Override
				protected Entry computeNext() {
					if (!iterator.hasNext()) {
						return endOfData();
					}
					int id = iterator.nextInt();
					ClientLobbyQueuedGame game = byIdOrThrow(id);
					return new Entry(id, game);
				}
			};
		};
	}

	public void encode(RegistryFriendlyByteBuf buffer) {
		buffer.writeVarInt(queue.size());

		queue.forEach((IntConsumer) id -> {
			ClientLobbyQueuedGame game = byIdOrThrow(id);
			buffer.writeVarInt(id);
			game.encode(buffer);
		});
	}

	public static ClientLobbyQueue decode(RegistryFriendlyByteBuf buffer) {
		ClientLobbyQueue queue = new ClientLobbyQueue();

		int queueSize = buffer.readVarInt();
		for (int i = 0; i < queueSize; i++) {
			int id = buffer.readVarInt();
			ClientLobbyQueuedGame game = ClientLobbyQueuedGame.decode(buffer);
			queue.add(id, game);
		}

		return queue;
	}

	public static final class Entry {
		private final int id;
		private final ClientLobbyQueuedGame game;

		Entry(int id, ClientLobbyQueuedGame game) {
			this.id = id;
			this.game = game;
		}

		public int id() {
			return id;
		}

		public ClientLobbyQueuedGame game() {
			return game;
		}
	}
}
