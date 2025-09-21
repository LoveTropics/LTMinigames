package com.lovetropics.minigames.common.core.network.trivia;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.river_race.block.HasTrivia;
import com.lovetropics.minigames.common.core.game.IGameManager;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestTriviaStateUpdateMessage(BlockPos triviaBlock) implements CustomPacketPayload {
	public static final Type<RequestTriviaStateUpdateMessage> TYPE = new Type<>(LoveTropics.location("request_trivia_update"));
	public static final StreamCodec<ByteBuf, RequestTriviaStateUpdateMessage> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, RequestTriviaStateUpdateMessage::triviaBlock,
			RequestTriviaStateUpdateMessage::new
	);

	public static void handle(final RequestTriviaStateUpdateMessage message, final IPayloadContext context) {
		if (context.player().level().getBlockEntity(message.triviaBlock()) instanceof HasTrivia triviaBlockEntity) {
			IGamePhase game = IGameManager.get().getGamePhaseFor(context.player());
			if (game != null) {
				ServerPlayer player = (ServerPlayer) context.player();
				PacketDistributor.sendToPlayer(player, new TriviaAnswerResponseMessage(message.triviaBlock(), triviaBlockEntity.getState()));
			}
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
