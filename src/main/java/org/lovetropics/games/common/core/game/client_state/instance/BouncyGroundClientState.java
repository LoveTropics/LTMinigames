package org.lovetropics.games.common.core.game.client_state.instance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;

/// Makes the player bounce like on slime blocks
public record BouncyGroundClientState(float minFallDistance, float bounciness) implements GameClientState {
	public static final MapCodec<BouncyGroundClientState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("min_fall_distance", 0.0f).forGetter(BouncyGroundClientState::minFallDistance),
			Codec.floatRange(0.0f, 1.0f).fieldOf("bounciness").forGetter(BouncyGroundClientState::bounciness)
	).apply(i, BouncyGroundClientState::new));

	public boolean bounces(Entity entity, double fallDistance) {
		return fallDistance >= minFallDistance && !entity.isSuppressingBounce();
	}

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.BOUNCY_GROUND.get();
	}
}
