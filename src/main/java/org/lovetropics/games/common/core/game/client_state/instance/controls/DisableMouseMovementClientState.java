package org.lovetropics.games.common.core.game.client_state.instance.controls;

import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record DisableMouseMovementClientState(boolean up, boolean down, boolean left, boolean right) implements GameClientState {

	public static final MapCodec<DisableMouseMovementClientState> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			Codec.BOOL.optionalFieldOf("up", false).forGetter(DisableMouseMovementClientState::up),
			Codec.BOOL.optionalFieldOf("down", false).forGetter(DisableMouseMovementClientState::down),
			Codec.BOOL.optionalFieldOf("left", false).forGetter(DisableMouseMovementClientState::left),
			Codec.BOOL.optionalFieldOf("right", false).forGetter(DisableMouseMovementClientState::right)
	).apply(in, DisableMouseMovementClientState::new));

	public static final StreamCodec<ByteBuf, DisableMouseMovementClientState> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, DisableMouseMovementClientState::down,
			ByteBufCodecs.BOOL, DisableMouseMovementClientState::up,
			ByteBufCodecs.BOOL, DisableMouseMovementClientState::left,
			ByteBufCodecs.BOOL, DisableMouseMovementClientState::right,
			DisableMouseMovementClientState::new
	);

	public double getXMovement(double dx) {
		if (left && dx < 0) {
			return 0;
		}

		if (right && dx > 0) {
			return 0;
		}

		return dx;
	}

	public double getYMovement(double dy) {
		if (up && dy < 0) {
			return 0;
		}

		if (down && dy > 0) {
			return 0;
		}

		return dy;
	}

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.DISABLE_MOUSE_MOVEMENT.get();
	}
}
