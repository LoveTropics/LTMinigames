package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.DamageSourcePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameLivingEntityEvents;

import java.util.Optional;

public record ScaleDamageBehavior(
		float factor,
		DamageSourcePredicate source,
		Optional<EntityPredicate> target
) implements IGameBehavior {
	public static final MapCodec<ScaleDamageBehavior> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("factor").forGetter(ScaleDamageBehavior::factor),
			DamageSourcePredicate.CODEC.fieldOf("source").forGetter(ScaleDamageBehavior::source),
			EntityPredicate.CODEC.optionalFieldOf("target").forGetter(ScaleDamageBehavior::target)
	).apply(in, ScaleDamageBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameLivingEntityEvents.DAMAGE_AMOUNT, (level, entity, damageSource, amount, originalAmount) -> {
			if (target.isPresent() && !target.get().matches(level, entity.position(), entity)) {
				return amount;
			}
			if (source.matches(level, entity.position(), damageSource)) {
				return factor * amount;
			}
			return amount;
		});
	}
}
