package com.lovetropics.minigames.common.core.network.trivia;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.trivia.ClientTriviaHandler;
import com.lovetropics.minigames.common.content.river_race.block.TriviaBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TriviaAnswerResponseMessage(BlockPos triviaBlock, TriviaBlockEntity.TriviaBlockState triviaBlockState) implements CustomPacketPayload {

	public static final Type<TriviaAnswerResponseMessage> TYPE = new Type<>(LoveTropics.location("trivia_answer_response"));
	public static final StreamCodec<ByteBuf, TriviaAnswerResponseMessage> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, TriviaAnswerResponseMessage::triviaBlock,
			TriviaBlockEntity.TriviaBlockState.STREAM_CODEC, TriviaAnswerResponseMessage::triviaBlockState,
			TriviaAnswerResponseMessage::new
	);

	public static void handle(final TriviaAnswerResponseMessage message, final IPayloadContext context) {
		ClientTriviaHandler.handleResponse(message);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
