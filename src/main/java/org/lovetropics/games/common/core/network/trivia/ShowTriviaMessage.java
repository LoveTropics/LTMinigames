package org.lovetropics.games.common.core.network.trivia;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.client.game.trivia.ClientTriviaHandler;
import org.lovetropics.games.common.content.river_race.behaviour.TriviaBehaviour;
import org.lovetropics.games.common.content.river_race.block.TriviaBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ShowTriviaMessage(BlockPos triviaBlock, TriviaBehaviour.TriviaQuestion question, TriviaBlockEntity.TriviaBlockState triviaBlockState) implements CustomPacketPayload {

	public static final Type<ShowTriviaMessage> TYPE = new Type<>(LoveTropics.id("show_trivia"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ShowTriviaMessage> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, ShowTriviaMessage::triviaBlock,
			TriviaBehaviour.TriviaQuestion.STREAM_CODEC, ShowTriviaMessage::question,
			TriviaBlockEntity.TriviaBlockState.STREAM_CODEC, ShowTriviaMessage::triviaBlockState,
			ShowTriviaMessage::new
	);

	public static void handle(ShowTriviaMessage message, IPayloadContext context) {
		ClientTriviaHandler.showScreen(message);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
