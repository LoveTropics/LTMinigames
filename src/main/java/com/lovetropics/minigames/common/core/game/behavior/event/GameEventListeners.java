package com.lovetropics.minigames.common.core.game.behavior.event;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GameEventListeners implements EventRegistrar {
	private final Reference2ObjectMap<GameEventType<?>, List<Object>> listeners = new Reference2ObjectOpenHashMap<>();
	private final Reference2ObjectMap<GameEventType<?>, Object> invokers = new Reference2ObjectOpenHashMap<>();

	@Override
	public <T> void listen(GameEventType<T> type, T listener) {
		List<Object> listeners = this.listeners.computeIfAbsent(type, e -> new ArrayList<>());
		listeners.add(listener);
		rebuildInvoker(type);
	}

	@Override
	public <T> void unlisten(GameEventType<T> type, T listener) {
		List<Object> listeners = this.listeners.get(type);
		if (listeners != null && listeners.remove(listener)) {
			rebuildInvoker(type);
		}
	}

	private <T> void rebuildInvoker(GameEventType<T> type) {
		Object combined = type.combineUnchecked(listeners.get(type));
		invokers.put(type, combined);
	}

	@SuppressWarnings("unchecked")
	public <T> T invoker(GameEventType<T> type) {
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
		return invokers.keySet();
	}

	@SuppressWarnings("unchecked")
	private static <T> void acceptUnchecked(ForEachHandler handler, GameEventType<T> type, Object listener) {
		handler.accept(type, (T) listener);
	}

	@Override
	public void addAll(GameEventListeners listeners) {
		listeners.listeners.forEach((type, list) -> {
			this.listeners.computeIfAbsent(type, e -> new ArrayList<>()).addAll(list);
			rebuildInvoker(type);
		});
	}

	@Override
	public void removeAll(GameEventListeners listeners) {
		listeners.listeners.forEach((type, list) -> {
			List<Object> targetList = this.listeners.get(type);
			if (targetList != null) {
				targetList.removeAll(list);
				rebuildInvoker(type);
			}
		});
	}

	public boolean hasListeners(GameEventType<?> type) {
		return !listeners.getOrDefault(type, List.of()).isEmpty();
	}

	public interface ForEachHandler {
		<T> void accept(GameEventType<T> type, T listener);
	}
}
