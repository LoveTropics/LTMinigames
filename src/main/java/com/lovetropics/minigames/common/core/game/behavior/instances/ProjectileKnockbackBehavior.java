package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;
import java.util.function.Supplier;

public record ProjectileKnockbackBehavior(
		Optional<EntityPredicate> projectilePredicate,
		Optional<EntityPredicate> targetPredicate,
		float strength
) implements IGameBehavior {
	public static final MapCodec<ProjectileKnockbackBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityPredicate.CODEC.optionalFieldOf("projectile").forGetter(ProjectileKnockbackBehavior::projectilePredicate),
			EntityPredicate.CODEC.optionalFieldOf("target").forGetter(ProjectileKnockbackBehavior::targetPredicate),
			Codec.FLOAT.optionalFieldOf("strength", 0.4f).forGetter(ProjectileKnockbackBehavior::strength)
	).apply(i, ProjectileKnockbackBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameWorldEvents.PROJECTILE_IMPACT, (projectile, hitResult) -> {
			if (hitResult.getType() != HitResult.Type.ENTITY) {
				return;
			}
			if (projectilePredicate.isPresent() && !projectilePredicate.get().matches(game.level(), null, projectile)) {
				return;
			}
			EntityHitResult entityHitResult = (EntityHitResult) hitResult;
			if (entityHitResult.getEntity() instanceof LivingEntity target) {
				if (targetPredicate.isPresent() && !targetPredicate.get().matches(game.level(), null, target)) {
					return;
				}
				double deltaX = -projectile.getDeltaMovement().x;
				double deltaZ = -projectile.getDeltaMovement().z;
				target.knockback(strength, deltaX, deltaZ);
				target.hurtMarked = true;
			}
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.PROJECTILE_KNOCKBACK;
	}
}
