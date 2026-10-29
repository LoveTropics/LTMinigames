package org.lovetropics.games.common.core.game.behavior.event;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GameEventListeners implements EventRegistrar {
	private final Reference2ObjectMap<GameEventType<?>, List<Object>> listeners = new Reference2ObjectOpenHashMap<>();

	private volatile @Nullable Reference2ObjectMap<GameEventType<?>, Object> invokers;

	@Override
	public <T> void listen(GameEventType<T> type, T listener) {
		List<Object> listeners = this.listeners.computeIfAbsent(type, _ -> new ArrayList<>());
		listeners.add(listener);
		invokers = null;
	}

	@Override
	public <T> void unlisten(GameEventType<T> type, T listener) {
		List<Object> listeners = this.listeners.get(type);
		if (listeners != null && listeners.remove(listener)) {
			invokers = null;
		}
	}

	private Reference2ObjectMap<GameEventType<?>, Object> buildInvokers() {
		Reference2ObjectMap<GameEventType<?>, Object> invokers = new Reference2ObjectOpenHashMap<>();
		for (Map.Entry<GameEventType<?>, List<Object>> entry : listeners.entrySet()) {
			GameEventType<?> type = entry.getKey();
			invokers.put(type, type.combineUnchecked(entry.getValue()));
		}
		return invokers;
	}

	// Can be safely called from any thread - safety of the underlying handlers is however not guaranteed
	@SuppressWarnings("unchecked")
	public <T> T invoker(GameEventType<T> type) {
		Reference2ObjectMap<GameEventType<?>, Object> invokers = this.invokers;
		if (invokers == null) {
			synchronized (this) {
				invokers = this.invokers;
				if (invokers == null) {
					invokers = buildInvokers();
					this.invokers = invokers;
				}
			}
		}
		return (T) invokers.getOrDefault(type, type.empty());
	}

	public void forEach(ForEachHandler handler) {
		for (Map.Entry<GameEventType<?>, List<Object>> entry : listeners.entrySet()) {
			for (Object listener : entry.getValue()) {
				acceptUnchecked(handler, entry.getKey(), listener);
			}
		}
	}

	public Set<GameEventType<?>> eventTypes() {
		return listeners.keySet();
	}

	@SuppressWarnings("unchecked")
	private static <T> void acceptUnchecked(ForEachHandler handler, GameEventType<T> type, Object listener) {
		handler.accept(type, (T) listener);
	}

	@Override
	public void addAll(GameEventListeners listeners) {
		listeners.listeners.forEach((type, list) ->
				this.listeners.computeIfAbsent(type, _ -> new ArrayList<>()).addAll(list)
		);
		invokers = null;
	}

	@Override
	public void removeAll(GameEventListeners listeners) {
		listeners.listeners.forEach((type, list) -> {
			List<Object> targetList = this.listeners.get(type);
			if (targetList != null) {
				targetList.removeAll(list);
			}
		});
		invokers = null;
	}

	public boolean hasListeners(GameEventType<?> type) {
		return !listeners.getOrDefault(type, List.of()).isEmpty();
	}

	public interface ForEachHandler {
		<T> void accept(GameEventType<T> type, T listener);
	}
}
