package org.lovetropics.games.common.content.river_race.block;

import net.minecraft.util.StringRepresentable;

public enum TriviaType implements StringRepresentable {
	REWARD("reward", TriviaDifficulty.EASY),
	GATE("gate", TriviaDifficulty.MEDIUM),
	COLLECTABLE("collectable", TriviaDifficulty.HARD),
	VICTORY("victory", TriviaDifficulty.HARD);

	private final String id;
	private final TriviaDifficulty difficulty;

	TriviaType(String id, TriviaDifficulty difficulty) {
		this.id = id;
		this.difficulty = difficulty;
	}

	public TriviaDifficulty difficulty() {
		return difficulty;
	}

	@Override
	public String getSerializedName() {
		return id;
	}
}
