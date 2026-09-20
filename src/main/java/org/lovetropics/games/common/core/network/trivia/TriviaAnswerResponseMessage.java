package org.lovetropics.games.common.core.network.trivia;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.client.game.trivia.ClientTriviaHandler;
import org.lovetropics.games.common.content.river_race.block.TriviaBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TriviaAnswerResponseMessage(BlockPos triviaBlock, TriviaBlockEntity.TriviaBlockState triviaBlockState) implements CustomPacketPayload {

	public static final Type<TriviaAnswerResponseMessage> TYPE = new Type<>(LoveTropics.id("trivia_answer_response"));
	public static final StreamCodec<ByteBuf, TriviaAnswerResponseMessage> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, TriviaAnswerResponseMessage::triviaBlock,
			TriviaBlockEntity.TriviaBlockState.STREAM_CODEC, TriviaAnswerResponseMessage::triviaBlockState,
			TriviaAnswerResponseMessage::new
	);

	public static void handle(TriviaAnswerResponseMessage message, IPayloadContext context) {
		ClientTriviaHandler.handleResponse(message);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
