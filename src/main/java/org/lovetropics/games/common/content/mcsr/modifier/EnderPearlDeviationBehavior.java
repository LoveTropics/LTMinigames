package org.lovetropics.games.common.content.mcsr.modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;

/// Thrown ender pearls veer to the left or right, by more degrees the stronger the modifier is
public record EnderPearlDeviationBehavior(String modifier, float degreesPerLevel) implements IGameBehavior {
	public static final MapCodec<EnderPearlDeviationBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("modifier").forGetter(EnderPearlDeviationBehavior::modifier),
			Codec.FLOAT.fieldOf("degrees_per_level").forGetter(EnderPearlDeviationBehavior::degreesPerLevel)
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
			float degrees = modifiers.level(modifier) * degreesPerLevel;
			if (degrees != 0.0f) {
				// Before the pearl is sent to players, so they see it veer off right away
				float sign = game.random().nextBoolean() ? 1.0f : -1.0f;
				pearl.setDeltaMovement(pearl.getDeltaMovement().yRot(sign * degrees * ((float) Math.PI / 180.0f)));
			}
		});
	}
}
