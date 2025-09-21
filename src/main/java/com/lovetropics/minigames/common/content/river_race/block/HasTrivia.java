package com.lovetropics.minigames.common.content.river_race.block;

import com.lovetropics.minigames.common.content.river_race.behaviour.TriviaBehaviour;

import javax.annotation.Nullable;

public interface HasTrivia {

	void setQuestion(TriviaBehaviour.TriviaQuestion question);

	@Nullable
	TriviaBehaviour.TriviaQuestion getQuestion();

	TriviaType getTriviaType();

	long lockout(int lockoutSeconds);

	void unlock();

	boolean markAsCorrect();

	boolean isAnswered();

	TriviaBlockEntity.TriviaBlockState getState();
}
