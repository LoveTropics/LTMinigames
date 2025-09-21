package com.lovetropics.minigames.common.core.game.state.statistics;

import java.util.function.Function;

public final class StatisticDisplays {
	private static final String[] ORDINAL_SUFFIXES = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};

	public static <T> Function<T, String> simple() {
		return Object::toString;
	}

	public static Function<Integer, String> placement() {
		return placement -> "placed " + ordinal(placement);
	}

	// from: https://stackoverflow.com/a/6810409/4871468
	private static String ordinal(int value) {
		return switch (value % 100) {
			case 11, 12, 13 -> value + "th";
			default -> value + ORDINAL_SUFFIXES[value % 10];
		};
	}

	public static <T> Function<T, String> unit(String unit) {
		String suffix = " " + unit;
		return value -> value + suffix;
	}

	public static Function<Integer, String> minutesSeconds() {
		return totalSeconds -> {
			int minutes = totalSeconds / 60;
			int seconds = totalSeconds % 60;
			return String.format("%02d:%02d", minutes, seconds);
		};
	}

	public static Function<Integer, String> seconds() {
		return unit("seconds");
	}

	public static Function<PlayerKey, String> playerName() {
		return PlayerKey::name;
	}
}
