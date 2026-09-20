package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameLivingEntityEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;

import java.util.Optional;

public record ScaleExplosionKnockbackBehavior(float factor, Optional<EntityPredicate> explodedPredicate, Optional<EntityPredicate> exploderPredicate) implements IGameBehavior {
	public static final MapCodec<ScaleExplosionKnockbackBehavior> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("factor").forGetter(ScaleExplosionKnockbackBehavior::factor),
			EntityPredicate.CODEC.optionalFieldOf("exploded").forGetter(ScaleExplosionKnockbackBehavior::explodedPredicate),
			EntityPredicate.CODEC.optionalFieldOf("exploder").forGetter(ScaleExplosionKnockbackBehavior::exploderPredicate)
	).apply(in, ScaleExplosionKnockbackBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameLivingEntityEvents.MODIFY_EXPLOSION_KNOCKBACK, (level, entity, explosion, knockback, originalKnockback) -> {
			if (matches(level, entity, explosion)) {
				return knockback.scale(factor);
			}
			return knockback;
		});
	}

	private boolean matches(ServerLevel level, Entity entity, Explosion explosion) {
		if (explodedPredicate.isPresent() && !explodedPredicate.get().matches(level, explosion.center(), entity)) {
			return false;
		}
		if (exploderPredicate.isPresent() && !exploderPredicate.get().matches(level, explosion.center(), explosion.getDirectSourceEntity())) {
			return false;
		}
		return true;
	}
}
