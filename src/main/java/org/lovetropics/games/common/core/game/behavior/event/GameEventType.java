package org.lovetropics.games.common.core.game.behavior.event;

import java.lang.reflect.Array;
import java.util.Collection;

public final class GameEventType<T> {
	private final Class<T> type;
	private final Combinator<T> combinator;

	private final T empty;

	@SuppressWarnings("unchecked")
	private GameEventType(Class<T> type, Combinator<T> combinator) {
		this.type = type;
		this.combinator = combinator;

		empty = combinator.apply((T[]) Array.newInstance(this.type, 0));
	}

	public static <T> GameEventType<T> create(Class<T> type, Combinator<T> combinator) {
		return new GameEventType<>(type, combinator);
	}

	public T combine(T[] listeners) {
		return combinator.apply(listeners);
	}

	@SuppressWarnings("unchecked")
	public <U> T combineUnchecked(Collection<U> listeners) {
		T[] array = (T[]) Array.newInstance(type, listeners.size());
		return combine(listeners.toArray(array));
	}

	public T empty() {
		return empty;
	}

	public Class<T> type() {
		return type;
	}

	public interface Combinator<T> {
		T apply(T[] listeners);
	}
}
