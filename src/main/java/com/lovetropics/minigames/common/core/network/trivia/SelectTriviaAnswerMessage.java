package com.lovetropics.minigames.common.core.network.trivia;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.river_race.TriviaEvents;
import com.lovetropics.minigames.common.content.river_race.behaviour.TriviaBehaviour;
import com.lovetropics.minigames.common.content.river_race.block.HasTrivia;
import com.lovetropics.minigames.common.core.game.IGameManager;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectTriviaAnswerMessage(BlockPos triviaBlock, int selectedAnswer) implements CustomPacketPayload {
    public static final Type<SelectTriviaAnswerMessage> TYPE = new Type<>(LoveTropics.location("select_trivia_answer"));
    public static final StreamCodec<ByteBuf, SelectTriviaAnswerMessage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SelectTriviaAnswerMessage::triviaBlock,
            ByteBufCodecs.VAR_INT, SelectTriviaAnswerMessage::selectedAnswer,
            SelectTriviaAnswerMessage::new
    );

    public static void handle(final SelectTriviaAnswerMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (!player.canInteractWithBlock(message.triviaBlock(), ServerPlayer.BLOCK_INTERACTION_DISTANCE_VERIFICATION_BUFFER)) {
            return;
        }
        IGamePhase game = IGameManager.get().getGamePhaseFor(player);
		if (game != null && player.level().getBlockEntity(message.triviaBlock) instanceof final HasTrivia triviaBlock) {
            TriviaBehaviour.TriviaQuestion question = triviaBlock.getQuestion();
            if (question == null) {
                return;
            }
            TriviaBehaviour.TriviaQuestion.TriviaQuestionAnswer selectedAnswer = question.getAnswer(message.selectedAnswer);
			if (selectedAnswer != null) {
				game.invoker(TriviaEvents.ANSWER_TRIVIA_BLOCK_QUESTION).onAnswerQuestion(player, message.triviaBlock(), triviaBlock, question, selectedAnswer);
//                triviaBlock.handleAnswerSelection(context.player(), message.selectedAnswer());
            }
		}
    }

    @Override
    public Type<SelectTriviaAnswerMessage> type() {
        return TYPE;
    }
}
