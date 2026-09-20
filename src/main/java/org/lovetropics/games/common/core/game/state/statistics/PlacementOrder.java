package org.lovetropics.games.common.core.game.state.statistics;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Comparator;

public enum PlacementOrder implements StringRepresentable {
	MAX("max"),
	MIN("min");

	public static final Codec<PlacementOrder> CODEC = StringRepresentable.fromEnum(PlacementOrder::values);

	private final String key;

	PlacementOrder(String key) {
		this.key = key;
	}

	public <T extends Comparable<T>> Comparator<T> asComparator() {
		return this == MAX ? Comparator.reverseOrder() : Comparator.naturalOrder();
	}

	@Override
	public String getSerializedName() {
		return key;
	}
}
