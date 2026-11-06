package org.lovetropics.games.common.content.tropic_rush.modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.BouncyGroundClientState;
import org.lovetropics.games.common.core.game.player.PlayerRole;

import java.util.List;

public record BouncyGroundBehavior(String modifier, List<Bounce> levels) implements IGameBehavior {
	public static final MapCodec<BouncyGroundBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("modifier").forGetter(BouncyGroundBehavior::modifier),
			ExtraCodecs.nonEmptyList(Bounce.CODEC.listOf()).fieldOf("levels").forGetter(BouncyGroundBehavior::levels)
	).apply(i, BouncyGroundBehavior::new));

	private static final int UPDATE_INTERVAL = 10;

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		GameModifiers modifiers = GameModifiers.getOrThrow(game, modifier);

		GameClientState.applyGlobally(game, events, UPDATE_INTERVAL, GameClientStateTypes.BOUNCY_GROUND.get(), player -> {
			Bounce bounce = getBounce(game, modifiers, player);
			return bounce != null ? new BouncyGroundClientState(bounce.minFallDistance, bounce.bounciness) : null;
		});

		events.listen(GamePlayerEvents.FALL, (player, fallDistance, damageMultiplier) -> {
			Bounce bounce = getBounce(game, modifiers, player);
			if (bounce == null || fallDistance < bounce.minFallDistance || player.isSuppressingBounce()) {
				return damageMultiplier;
			}
			return damageMultiplier * bounce.fallDamageMultiplier;
		});
	}

	private @Nullable Bounce getBounce(IGamePhase game, GameModifiers modifiers, ServerPlayer player) {
		int level = modifiers.level(modifier);
		if (level <= 0 || game.getRoleFor(player) != PlayerRole.PARTICIPANT) {
			return null;
		}
		return levels.get(Math.min(level, levels.size()) - 1);
	}

	public record Bounce(float minFallDistance, float bounciness, float fallDamageMultiplier) {
		public static final Codec<Bounce> CODEC = RecordCodecBuilder.create(i -> i.group(
				ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("min_fall_distance", 0.0f).forGetter(Bounce::minFallDistance),
				Codec.floatRange(0.0f, 1.0f).fieldOf("bounciness").forGetter(Bounce::bounciness),
				ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("fall_damage_multiplier", 1.0f).forGetter(Bounce::fallDamageMultiplier)
		).apply(i, Bounce::new));
	}
}
