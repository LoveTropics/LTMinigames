package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.util.Codecs;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record HighlightBlocksState(
		List<BlockPos> positions
) implements GameClientState {
	public static final HighlightBlocksState EMPTY = new HighlightBlocksState(List.of());

	public static final MapCodec<HighlightBlocksState> CODEC = Codecs.no();
	public static final StreamCodec<ByteBuf, HighlightBlocksState> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), HighlightBlocksState::positions,
			HighlightBlocksState::new
	);

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.HIGHLIGHT_BLOCKS.get();
	}
}
