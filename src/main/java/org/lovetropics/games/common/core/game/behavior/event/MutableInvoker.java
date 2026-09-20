package org.lovetropics.games.common.core.game.behavior.event;

import java.lang.reflect.Array;

public class MutableInvoker<T> {
	private final GameEventType<T> type;
	private final T[] listeners;
	private final T invoker;

	@SuppressWarnings("unchecked")
	public MutableInvoker(GameEventType<T> type) {
		this.type = type;
		listeners = (T[]) Array.newInstance(type.type(), 1);
		listeners[0] = type.empty();
		invoker = type.combine(listeners);
	}

	public static <T> MutableInvoker<T> addTo(EventRegistrar events, GameEventType<T> type) {
		MutableInvoker<T> invoker = new MutableInvoker<>(type);
		events.listen(type, invoker.get());
		return invoker;
	}

	public void set(T invoker) {
		listeners[0] = invoker;
	}

	@SuppressWarnings("unchecked")
	public void setUnchecked(Object invoker) {
		set((T) invoker);
	}

	public void clear() {
		listeners[0] = type.empty();
	}

	public T get() {
		return invoker;
	}
}
