package com.lovetropics.minigames.common.content.biodiversity_blitz.explosion;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Simple marker for explosions that affect mobs but not players
 */
public class FilteredExplosion extends ServerExplosion {
    public final Predicate<Entity> remove;

	public FilteredExplosion(ServerLevel level, @Nullable Entity source, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator damageCalculator, Vec3 center, float radius, boolean fire, BlockInteraction blockInteraction, Predicate<Entity> remove) {
		super(level, source, damageSource, damageCalculator, center, radius, fire, blockInteraction);
		this.remove = remove;
	}
}
