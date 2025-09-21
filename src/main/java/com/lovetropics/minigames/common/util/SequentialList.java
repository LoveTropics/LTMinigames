package com.lovetropics.minigames.common.util;

import java.util.List;

public class SequentialList<T> {
	private final List<T> list;
	private int index;

	public SequentialList(List<T> list, int index) {
		this.list = list;
		this.index = index;
	}

	public T current() {
		return list.get(index);
	}

	public T next() {
		if (index >= list.size() - 1) {
			index = 0;
		} else {
			index++;
		}

		return current();
	}

	public List<T> all() {
		return list;
	}
}
