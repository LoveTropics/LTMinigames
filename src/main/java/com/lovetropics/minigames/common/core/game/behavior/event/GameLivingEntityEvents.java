package com.lovetropics.minigames.common.core.game.behavior.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.function.Consumer;

public final class GameLivingEntityEvents {
	public static final GameEventType<Tick> TICK = GameEventType.create(Tick.class, listeners -> entity -> {
		for (Tick listener : listeners) {
			listener.tick(entity);
		}
	});

	public static final GameEventType<Death> DEATH = GameEventType.create(Death.class, listeners -> (entity, damageSource) -> {
		for (Death listener : listeners) {
			TriState result = listener.onDeath(entity, damageSource);
			if (!result.isDefault()) {
				return result;
			}
		}

		return TriState.DEFAULT;
	});

	public static final GameEventType<MobDrop> MOB_DROP = GameEventType.create(MobDrop.class, listeners -> (entity, damageSource, drops) -> {
		for (MobDrop listener : listeners) {
			TriState result = listener.onMobDrop(entity, damageSource, drops);
			if (!result.isDefault()) {
				return result;
			}
		}

		return TriState.DEFAULT;
	});

	public static final GameEventType<FarmlandTrample> FARMLAND_TRAMPLE = GameEventType.create(FarmlandTrample.class, listeners -> (entity, pos, state) -> {
		for (FarmlandTrample listener : listeners) {
			TriState result = listener.onFarmlandTrample(entity, pos, state);
			if (!result.isDefault()) {
				return result;
			}
		}

		return TriState.DEFAULT;
	});

	public static final GameEventType<Spawn> SPAWNED = GameEventType.create(Spawn.class, listeners -> (entity, reason, player) -> {
		for (Spawn listener : listeners) {
			listener.onSpawn(entity, reason, player);
		}
	});

	public static final GameEventType<EnderTeleport> ENDER_PEARL_TELEPORT = GameEventType.create(EnderTeleport.class, listeners -> (player, x, y, z, damage, callback) -> {
		for (EnderTeleport listener : listeners) {
			listener.onEnderPearlTeleport(player, x, y, z, damage, callback);
		}
	});

	public static final GameEventType<ModifyExplosionKnockback> MODIFY_EXPLOSION_KNOCKBACK = GameEventType.create(ModifyExplosionKnockback.class, listeners -> (entity, explosion, knockback, initialKnockback) -> {
		for (final ModifyExplosionKnockback listener : listeners) {
			knockback = listener.getKnockback(entity, explosion, knockback, initialKnockback);
		}
		return knockback;
	});

	private GameLivingEntityEvents() {
	}

	public interface Tick {
		void tick(LivingEntity entity);
	}

	public interface Death {
		TriState onDeath(LivingEntity entity, DamageSource damageSource);
	}

	public interface MobDrop {
		TriState onMobDrop(LivingEntity entity, DamageSource damageSource, Collection<ItemEntity> drops);
	}

	public interface FarmlandTrample {
		TriState onFarmlandTrample(Entity entity, BlockPos pos, BlockState state);
	}

	public interface Spawn {
		void onSpawn(LivingEntity entity, EntitySpawnReason reason, @Nullable ServerPlayer player);
	}

	public interface EnderTeleport {
		void onEnderPearlTeleport(ServerPlayer player, double targetX, double targetY, double targetZ, float attackDamage, Consumer<Float> setDamage);
	}

	public interface ModifyExplosionKnockback {
		Vec3 getKnockback(Entity entity, Explosion explosion, Vec3 velocity, Vec3 initialVelocity);
	}
}
