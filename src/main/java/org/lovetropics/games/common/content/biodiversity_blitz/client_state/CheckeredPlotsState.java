package org.lovetropics.games.common.content.biodiversity_blitz.client_state;

import com.lovetropics.lib.BlockBox;
import org.lovetropics.games.common.content.biodiversity_blitz.BiodiversityBlitz;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record CheckeredPlotsState(List<BlockBox> plots, BlockBox global) implements GameClientState {
	public static final MapCodec<CheckeredPlotsState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockBox.CODEC.listOf().fieldOf("plots").forGetter(CheckeredPlotsState::plots)
	).apply(i, CheckeredPlotsState::new));
	public static final StreamCodec<ByteBuf, CheckeredPlotsState> STREAM_CODEC = StreamCodec.composite(
			BlockBox.STREAM_CODEC.apply(ByteBufCodecs.list()), CheckeredPlotsState::plots,
			CheckeredPlotsState::new
	);

	public CheckeredPlotsState(List<BlockBox> plots) {
		this(plots, computeGlobalBounds(plots));
	}

	private static BlockBox computeGlobalBounds(List<BlockBox> plots) {
		return plots.stream().reduce(BlockBox::encompassing).orElseGet(() -> BlockBox.of(BlockPos.ZERO));
	}

	@Override
	public GameClientStateType<?> getType() {
		return BiodiversityBlitz.CHECKERED_PLOTS_STATE.get();
	}

	public boolean contains(BlockPos pos) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		if (!global.contains(x, y, z)) {
			return false;
		}

		for (BlockBox plot : plots) {
			if (plot.contains(x, y, z)) {
				return true;
			}
		}

		return false;
	}
}
