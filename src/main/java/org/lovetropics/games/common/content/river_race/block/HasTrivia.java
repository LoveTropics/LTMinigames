package org.lovetropics.games.common.content.river_race.block;

import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.content.river_race.behaviour.TriviaBehaviour;

public interface HasTrivia {

	void setQuestion(TriviaBehaviour.TriviaQuestion question);

	TriviaBehaviour.@Nullable TriviaQuestion getQuestion();

	TriviaType getTriviaType();

	long lockout(int lockoutSeconds);

	void unlock();

	boolean markAsCorrect();

	boolean isAnswered();

	TriviaBlockEntity.TriviaBlockState getState();
}
