package org.lovetropics.games.common.content.tropic_rush.modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;

/// Thrown ender pearls veer to the left or right, by more degrees the stronger the modifier is. The deviation grows
/// exponentially: each level multiplies it by the growth, up to the max degrees at the max level of the modifier.
///
/// @param maxDegrees the deviation at the max level of the modifier, e.g. 180 to send pearls straight back
/// @param growth     how many times more each level deviates than the one below, e.g. with 3 and a max level of 5:
///                   2.2, 6.7, 20, 60, then 180 degrees
public record EnderPearlDeviationBehavior(String modifier, float maxDegrees, float growth) implements IGameBehavior {
	public static final MapCodec<EnderPearlDeviationBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("modifier").forGetter(EnderPearlDeviationBehavior::modifier),
			Codec.floatRange(0.0f, 180.0f).fieldOf("max_degrees").forGetter(EnderPearlDeviationBehavior::maxDegrees),
			Codec.floatRange(1.0f, Float.MAX_VALUE).optionalFieldOf("growth", 2.0f).forGetter(EnderPearlDeviationBehavior::growth)
	).apply(i, EnderPearlDeviationBehavior::new));

	/// Pearls are added again when they go through a portal or get loaded back in, and must not veer off again
	private static final String DEVIATED_TAG = "ltminigames.deviated";

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		GameModifiers modifiers = GameModifiers.getOrThrow(game, modifier);
		events.listen(GameWorldEvents.ENTITY_ADDED, (level, entity) -> {
			if (!(entity instanceof ThrownEnderpearl pearl) || !pearl.addTag(DEVIATED_TAG)) {
				return;
			}
			float degrees = getDegrees(modifiers.level(modifier), modifiers.maxLevel(modifier));
			if (degrees != 0.0f) {
				// Before the pearl is sent to players, so they see it veer off right away
				float sign = game.random().nextBoolean() ? 1.0f : -1.0f;
				pearl.setDeltaMovement(pearl.getDeltaMovement().yRot(sign * degrees * ((float) Math.PI / 180.0f)));
			}
		});
	}

	private float getDegrees(int level, int maxLevel) {
		if (level <= 0) {
			return 0.0f;
		}
		return maxDegrees * (float) Math.pow(growth, level - maxLevel);
	}
}
