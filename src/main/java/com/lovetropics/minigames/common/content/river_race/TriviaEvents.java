package com.lovetropics.minigames.common.content.river_race;

import com.lovetropics.minigames.common.content.river_race.behaviour.TriviaBehaviour;
import com.lovetropics.minigames.common.content.river_race.block.HasTrivia;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class TriviaEvents {

	public static final GameEventType<AnswerQuestion> ANSWER_TRIVIA_BLOCK_QUESTION = GameEventType.create(AnswerQuestion.class,
			listeners -> (player, pos, triviaBlockEntity, question, answer) -> {
				for (AnswerQuestion listener : listeners) {
					boolean isCorrect = listener.onAnswerQuestion(player, pos, triviaBlockEntity, question, answer);
					if (isCorrect) {
						return true;
					}
				}
				return false;
			});

	public interface AnswerQuestion {
		boolean onAnswerQuestion(ServerPlayer player, BlockPos pos,
								 HasTrivia triviaBlockEntity,
								 TriviaBehaviour.TriviaQuestion question, TriviaBehaviour.TriviaQuestion.TriviaQuestionAnswer answer);
	}
}
